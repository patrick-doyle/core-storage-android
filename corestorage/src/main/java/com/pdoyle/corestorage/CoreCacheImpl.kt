package com.pdoyle.corestorage

import com.pdoyle.corestorage.log.CoreStorageLog
import java.nio.charset.Charset
import java.time.Instant
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.encodeUtf8
import okio.appendingSink
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Advanced fast and efficient storage that removes the need for wrappers.
 *
 * This is stored in the context.getCacheDir() by default, this can be wiped by the user without
 * affecting all the apps data.
 *
 * The memoryCache uses LRU to limit the size. This is 10MB by default.
 *
 * The memoryCache also holds the 5 most recently read items in memory to improve performance.
 *
 * This is thread safe for a single instance. If two memoryCache instances are pointing to the
 * same dir then threading issues will occur.
 *
 * **Version 1 entry header format**
 * <pre>
 * +--------------------------------------------------+
 * | 32Bit Int - Version int                          |
 * +--------------------------------------------------+
 * | 32Bit Int - Key length                           |
 * +--------------------------------------------------+
 * | UTF8 Encoded Base64 ByteString - Key             |
 * +--------------------------------------------------+
 * | 64Bit Long - Created at Date (epoch millis)      |
 * +--------------------------------------------------+
 * | 64Bit Long - Expires at Date (epoch millis)      |
 * +--------------------------------------------------+
 * |                                                  |
 * |                 Data Contents                    |
 * |                                                  |
 * +--------------------------------------------------+
</pre> *
 */

internal const val JOURNAL_FILE = "journal_file.jnl"

// when a trim happens it trims to 75% of the max size to prevent trimming on every write
// once the cache is near full
private const val LOAD_FACTOR = 0.75f
private const val VERSION = 1
private const val ENTRY_FILE_TEMPLATE = "%s.entry"

private const val LOG_TAG = "CoreCache"

internal class CoreCacheImpl(
    private val dir: Path,
    private val memoryCache: MemoryCache, // Lru memoryCache for memory disk objects
    private val maxSizeBytes: Int,
    private val logger: CoreStorageLog,
) : CoreCache {

    // Locks for each file/entry, ReadWriteLock allows multiple reads and only one writing at a time
    private val locks = Collections.synchronizedMap(HashMap<String, ReadWriteLock>())

    // master lock for managing the entire directory, prevents clearing the dir
    // from causing issues reading and writing
    private val masterLock = Any()

    private val journalFile: Path
    private var loadedJournal = false

    // Lru journal for disk objects.
    private var journal: MutableList<String> = Collections.synchronizedList(LinkedList())

    init {
        if (!dir.exists() || !dir.isDirectory()) {
            throw IllegalArgumentException(
                "The dir passed to storage must be a directory and must exist!!!",
            )
        }
        journalFile = dir.resolve(JOURNAL_FILE)
    }

    override fun <T> get(
        key: String,
        deserializer: (source: BufferedSource) -> T,
    ): T? {
        loadJournalIfNeeded()
        appendJournalForKey(key)

        return when {
            memoryCache.contains(key) -> { // Check memory memoryCache for entry
                memoryCache.get<CoreCacheEntry<T>>(key)?.let { data ->
                    if (data.hasExpired()) {
                        // entry expired remove it from memoryCache and disk
                        remove(key)
                        null
                    } else {
                        // not expired return entry
                        data.data
                    }
                }
            }

            contains(key) -> { // Check if there is an entry on disk
                readEntry(key, deserializer)?.let { entry ->
                    // Entry found, add it to the memory memoryCache
                    memoryCache.put(key, entry)
                    entry.data
                }
            }

            else -> {
                // no entry, make sure the entry is removed
                remove(key)
                null
            }
        }
    }

    /**
     * Used by the builder to store the built entry
     */
    override fun <T> put(
        key: String,
        data: T,
        expires: Instant,
        serialize: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean {
        loadJournalIfNeeded()
        val entry = CoreCacheEntry(data, Instant.now(), expires)
        memoryCache.put(key, entry)
        val didWrite = writeEntry(key, entry, serialize)
        var didUpdateJournal = false
        if (didWrite) {
            // update journal
            didUpdateJournal = appendJournalForKey(key)
        }
        // trim entries to size
        trim()
        return didWrite && didUpdateJournal
    }

    @Suppress("SwallowedException")
    override fun contains(key: String): Boolean {
        return try {
            val memoryCacheEntry = memoryCache.get<CoreCacheEntry<Any>>(key)
            if (memoryCacheEntry?.hasExpired() == true) {
                return false
            } else {
                val file = getEntryFile(key)
                if (!file.exists()) {
                    return false
                }
                inWriteLock(key) {
                    getEntryFile(key).source().buffer().use {
                        val entryHeader = EntryHeader.read(it)
                        if (entryHeader.hasExpired()) {
                            // expired entry, remove from disk
                            remove(key)
                            false
                        } else {
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            @Suppress("SwallowedException")
            logger.w(e, LOG_TAG, "Error checking for - [$key], ignoring")
            false
        }
    }

    /**
     * Remove an entry from the storage
     */
    override fun remove(key: String) {
        loadJournalIfNeeded()
        memoryCache.remove(key)
        journal.removeAll(listOf(key))

        try {
            inWriteLock(key) {
                getEntryFile(key).deleteIfExists()
                null
            }
        } catch (e: Exception) {
            @Suppress("SwallowedException")
            logger.w(e, LOG_TAG, "Remove missing key - [$key], ignoring")
        }
        writeJournal(journalFile, journal, false)
    }

    /**
     * Deletes the entire memoryCache and stats fresh
     */
    override fun clear() {
        journal.clear()
        memoryCache.clear()
        if (dir.exists()) {
            synchronized(masterLock) {
                deleteFileAndLog(journalFile)
                dir.listDirectoryEntries().forEach { deleteFileAndLog(it) }
            }
        }
        locks.clear()
    }

    @Suppress("SwallowedException")
    override fun getKeys(): List<String> {
        synchronized(masterLock) {
            val entries = dir.listDirectoryEntries()
            val keys = ArrayList<String>()
            entries.forEach {
                try {
                    if (it.name != journalFile.name) {
                        val entryHeader = EntryHeader.read(it.source().buffer())
                        keys.add(entryHeader.key)
                    }
                } catch (e: Exception) {
                    logger.w(e, LOG_TAG, "Remove getting keys, ignoring")
                    deleteFileAndLog(it)
                }
            }
            return keys
        }
    }

    // Gets the file for the key, key is base64 url encoded to prevent any
    // illegal chars sneaking in
    private fun getEntryFile(key: String): Path {
        val byteString = key.encode(Charset.forName("UTF-8"))
        val entryName = byteString.base64Url()
        return dir.resolve(String.format(Locale.US, ENTRY_FILE_TEMPLATE, entryName))
    }

    private fun currentStorageDiskUsage(): Long {
        synchronized(masterLock) {
            var currentSize: Long = 0
            // Measure current memoryCache size
            dir.listDirectoryEntries().let { fileList ->
                for (i in fileList.indices) {
                    if (fileList[i].name != journalFile.name) {
                        currentSize += fileList[i].fileSize()
                    }
                }
            }
            return currentSize
        }
    }

    /**
     * Trims the memoryCache until its less than 75% of the max size.
     */
    @Suppress(
        "NestedBlockDepth", "CognitiveComplexMethod", "MagicNumber",
        "LongMethod", "CyclomaticComplexMethod",
    )
    private fun trim() {
        fun sizeStatsLog(currentSize: Long, maxSizeBytes: Long): String {
            return String.format(
                Locale.US,
                "size [%.1fKB], max - [%.1fKB], load factor - [%.1fKB]",
                currentSize.toDouble() / 1000,
                maxSizeBytes.toDouble() / 1000,
                (maxSizeBytes * LOAD_FACTOR).toDouble() / 1000,
            )
        }

        // trim memoryCache to 75% of max size to prevent trimming on every write when
        // cache is near full
        val maxLoadSize = (maxSizeBytes * LOAD_FACTOR).toLong()

        // measure memoryCache size
        var currentSize = currentStorageDiskUsage()
        if (currentSize <= maxSizeBytes) {
            logger.d(
                LOG_TAG,
                "Skipping unneeded trim ${sizeStatsLog(currentSize, maxSizeBytes.toLong())}",
            )
            return
        }

        logger.d(LOG_TAG, "Starting trim")
        logger.d(LOG_TAG, "Before trim ${sizeStatsLog(currentSize, maxSizeBytes.toLong())}")

        // Remove expired entries
        logger.d(LOG_TAG, "----------------------------------------------------------------")
        logger.d(LOG_TAG, "Starting Expired trim")

        val expiredKeys = HashSet<String>()
        // synchronized on master lock, whole cache is affected
        synchronized(masterLock) {
            // remove all expired entries
            val files = dir.listDirectoryEntries()
            for (i in files.indices) {
                val file = files[i]
                if (file.name != journalFile.name) {
                    try {
                        val entryHeader = file.source().buffer().use {
                            EntryHeader.read(it)
                        }
                        if (entryHeader.hasExpired()) {
                            expiredKeys.add(entryHeader.key)
                            deleteFileAndLog(file)
                        }
                    } catch (
                        @Suppress("SwallowedException") e: Exception,
                    ) {
                        logger.w(e, LOG_TAG, "Remove reading expired file, ignoring")
                        // Error trimming expired file
                        deleteFileAndLog(file)
                    }
                }
            }
        }

        // if expired files were removed, remove the files from the journal
        // and remeasure the files to see if further trimming is needed
        if (expiredKeys.isNotEmpty()) {
            journal.removeAll(expiredKeys)
            writeJournal(journalFile, journal, false)
            currentSize = currentStorageDiskUsage()
        }

        logger.d(LOG_TAG, "After expired trim ${sizeStatsLog(currentSize, maxSizeBytes.toLong())}")
        logger.d(LOG_TAG, "----------------------------------------------------------------")
        if (currentSize <= maxLoadSize) {
            logger.d(LOG_TAG, "Skipping LRU trim ${sizeStatsLog(currentSize, maxSizeBytes.toLong())}")
            logger.d(LOG_TAG, "----------------------------------------------------------------")
            logger.d(LOG_TAG, "Finished trim")
            return
        }

        // Trim LRU files
        logger.d(LOG_TAG, "Starting LRU trim")
        run {
            /*
             * Entries are appended to the bottom of the journal as entries are created
             * so these need to be removed from the top of the list to preserve LRU order
             */
            val journalSize = journal.size
            val orderedList = ArrayList<String>(journal.size)
            run {
                for (i in 0 until journalSize) {
                    val key = journal[journalSize - 1 - i]
                    if (!orderedList.contains(key)) {
                        orderedList.add(key)
                    }
                }
                orderedList.reverse()
            }

            // synchronized on master lock, whole cache is affected
            synchronized(masterLock) {
                // Loop until the cache is empty or current size is smaller than maxSize
                // Work from the top of the list as the new entries are removed downwards
                val removedKeys = LinkedHashSet<String>()
                for (key in orderedList) {
                    val file = getEntryFile(key)
                    val fileSize = file.fileSize()
                    logger.d(LOG_TAG, "Trimming file for key [$key], size [${fileSize.toDouble() / 1000}KB]")
                    file.deleteIfExists()
                    if (!file.exists()) {
                        // each removed entry is added to the deleted list
                        removedKeys.add(key)
                        currentSize -= fileSize
                        if (currentSize <= maxLoadSize) {
                            // storage is small enough, don't ned to trim anymore
                            break
                        }
                    }
                }

                // Remove any entries from the memory memoryCache
                for (removedKey in removedKeys) {
                    memoryCache.remove(removedKey)
                }
                // Remove all deleted keys
                journal.removeAll(removedKeys)
            }
        }

        logger.d(LOG_TAG, "After LRU trim ${sizeStatsLog(currentSize, maxSizeBytes.toLong())}")
        logger.d(LOG_TAG, "----------------------------------------------------------------")

        // journal has been trimmed, overwrite with a fresh journal
        writeJournal(journalFile, journal, false)
        logger.d(LOG_TAG, "Finished trim")
    }

    private fun appendJournalForKey(key: String): Boolean {
        journal.remove(key)
        journal.add(key)
        return writeJournal(journalFile, listOf(key), true)
    }

    private fun loadJournalIfNeeded() {
        if (!loadedJournal) {
            journal.clear()
            journal = readJournal(journalFile).toMutableList()
            loadedJournal = true
        }
    }
    private fun <T> readEntry(
        key: String,
        deserializer: (source: BufferedSource) -> T,
    ): CoreCacheEntry<T>? {
        try {
            return inWriteLock(key) {
                getEntryFile(key).source().buffer().use {
                    val entryHeader = EntryHeader.read(it)
                    if (entryHeader.hasExpired()) {
                        // expired entry, remove from disk
                        remove(key)
                        return@inWriteLock null
                    }
                    CoreCacheEntry(
                        deserializer(it),
                        entryHeader.created,
                        entryHeader.expires,
                    )
                }
            }
        } catch (e: Exception) {
            // on error reading remove the entry
            logger.w(e,LOG_TAG, "Error reading entry for key - $key, deleting entry...")
            remove(key)
            return null
        }
    }

    private fun <T> writeEntry(
        key: String,
        entry: CoreCacheEntry<T>,
        serializer: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean {
        try {
            if (entry.hasExpired()) {
                logger.d(LOG_TAG, "Cant write expired entry!")
                return false
            }
            inWriteLock(key) {
                getEntryFile(key).sink().buffer().use {
                    val entryHeader =
                        EntryHeader(VERSION, key, Instant.now(), entry.expires)
                    entryHeader.write(it) // Write entryHeader
                    serializer(entry.data, it) // write entry
                    it.flush()
                    memoryCache.put(key, entry)
                }
                null
            }
            return true
        } catch (e: Exception) {
            remove(key)
            // on error writing remove the entry
            logger.w(e,LOG_TAG, "Error writing entry for key - $key")
            return false
        }
    }

    private fun writeJournal(
        journalFile: Path,
        lines: Collection<String>,
        append: Boolean,
    ): Boolean {
        try {
            return inWriteLock(JOURNAL_FILE) {
                if (!journalFile.exists()) {
                    journalFile.createFile()
                }

                val openOption = if (append) {
                    StandardOpenOption.APPEND
                } else {
                    StandardOpenOption.WRITE
                }
                val sink = journalFile.sink(openOption).buffer()
                sink.use {
                    for (entry in lines) {
                        sink.writeUtf8(entry)
                        sink.writeUtf8("\n")
                    }
                    sink.flush()
                }
                true
            }
        } catch (@Suppress("SwallowedException") e: Exception) {
            logger.w(e,LOG_TAG, "Error writing journal, clearing memoryCache")
            clear()
            return false
        }
    }

    private fun readJournal(journalFile: Path): List<String> {
        try {
            return inReadLock<List<String>>(JOURNAL_FILE) {
                try {
                    if (journalFile.exists()) {
                        val journal = LinkedList<String>()
                        journalFile.source().buffer().use {
                            var line: String? = it.readUtf8Line()
                            while (line != null) {
                                journal.add(line)
                                line = it.readUtf8Line()
                            }
                        }
                        return@inReadLock journal
                    } else {
                        return@inReadLock LinkedList<String>()
                    }
                } catch (e: Exception) {
                    logger.w(e,LOG_TAG, "Error reading journal, clearing cache")
                    clear()
                    return@inReadLock LinkedList<String>()
                }
            }
        } catch (e: Exception) {
            logger.w(e,LOG_TAG, "Error reading journal, clearing cache")
            clear()
            return LinkedList()
        }
    }

    private fun rwLockForKey(key: String): ReadWriteLock {
        return locks.getOrPut(key, { ReentrantReadWriteLock() })
    }

    private fun <T> inReadLock(key: String, func: () -> T): T {
        synchronized(masterLock) {
            var lock: Lock? = null
            try {
                val rwLock = rwLockForKey(key)
                lock = rwLock.readLock()
                lock.lock()
                return func()
            } finally {
                lock?.unlock()
            }
        }
    }

    private fun <T> inWriteLock(key: String, func: () -> T): T {
        synchronized(masterLock) {
            var lock: Lock? = null
            try {
                val rwLock = rwLockForKey(key)
                lock = rwLock.writeLock()
                lock.lock()
                return func()
            } finally {
                lock?.unlock()
            }
        }
    }

    private fun deleteFileAndLog(path: Path?) {
        try {
            path?.deleteIfExists()
            logger.d( "deleted file ${path?.name}")
        } catch (error: Exception) {
            logger.w(error, "error deleting file")
        }
    }

    /**
     * Header for entries, this contains metadata such as the version of the memoryCache and the
     * date created.
     */
    class EntryHeader(
        val version: Int,
        val key: String,
        val created: Instant,
        val expires: Instant,
    ) {

        // Write the entryHeader to disk, do NOT close the stream
        @Throws(Exception::class)
        fun write(sink: BufferedSink) {
            sink.writeInt(version)

            // write key value
            val keyData = key.encodeUtf8()
            sink.writeInt(keyData.size)
            sink.write(keyData)

            sink.writeLong(created.toEpochMilli())
            sink.writeLong(expires.toEpochMilli())
            sink.flush()
        }

        fun hasExpired(): Boolean {
            return Instant.now().isAfter(expires)
        }

        companion object {

            fun read(source: BufferedSource): EntryHeader {
                // first 4 bytes are always the version (32bit int)
                // switch on the version to enable reading of older entries
                // more versions can be added of the header changes
                when (val version = source.readInt()) {
                    1 -> {
                        // read back key value
                        val keyStringLength = source.readInt()
                        val byteStringKey = source.readByteString(keyStringLength.toLong())
                        val key = byteStringKey.utf8()

                        val created = Instant.ofEpochMilli(source.readLong())
                        val expires = Instant.ofEpochMilli(source.readLong())
                        return EntryHeader(version, key, created, expires)
                    }

                    else -> throw IllegalArgumentException(
                        "Error reading file entryHeader, verson - [$version] not recognised",
                    )
                }
            }
        }
    }
}
