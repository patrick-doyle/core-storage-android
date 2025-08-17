package com.pdoyle.corestorage

import com.pdoyle.corestorage.log.CoreStorageLog
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.encode
import okio.ByteString.Companion.encodeUtf8
import okio.buffer
import okio.sink
import okio.source
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Advanced fast and efficient storage that removes the need for wrappers.
 *
 * This is a CoreStorage System without a limit, make sure only to store truly long
 * term storage here. On Android this is created in the Files dir.
 *
 * **Version 1**
 * <pre>
 * +--------------------------------------------------+
 * | 32Bit Int - Version name                         |
 * +--------------------------------------------------+
 * | 32Bit Int - Key length                           |
 * +--------------------------------------------------+
 * | UTF8 Encoded Base64 ByteString - Key             |
 * +--------------------------------------------------+
 * | 64Bit Long - Updated at Date                     |
 * +--------------------------------------------------+
 * |                                                  |
 * |                 Data Contents                    |
 * |                                                  |
 * +--------------------------------------------------+
 * </pre>
 *
 */

private const val VERSION = 1
private const val ENTRY_FILE_TEMPLATE = "%s.entry"
private const val LOG_TAG = "CoreStorage"

// master lock for managing the entire directory, prevents clearing the dir from causing
// issues reading and writing
private val masterLock = Any()

internal class CoreStorageImpl(
    private val dir: Path,
    private val memoryCache: MemoryCache,
    private val logger: CoreStorageLog,
) : CoreStorage {

    // Locks for each file, ReadWriteLock allows multiple reads and only one writing at a time
    private val locks = Collections.synchronizedMap(HashMap<String, ReadWriteLock>())

    init {
        if (!dir.exists() || !dir.isDirectory()) {
            throw IllegalArgumentException(
                "The dir passed to storage must be a directory and must exist!!!",
            )
        }
    }

    /**
     * Returns an entry for the given key.
     *
     * @return the entry if found, null if missing.
     */
    override fun <T> get(key: String, deserializer: (bufferedSource: BufferedSource) -> T): T? {
        when {
            // Check memory memoryCache for entry
            memoryCache.contains(key) -> {
                @Suppress("UNCHECKED_CAST")
                return memoryCache.get<Any>(key) as? T
            }
            // Check if there is an entry on disk
            contains(key) -> {
                // only lock for that entry, allow multi-access to disk
                val entry = readEntry(key, deserializer)
                return if (entry != null) {
                    // Entry found, add it to the memory memoryCache
                    memoryCache.put(key, entry)
                    entry
                } else {
                    null
                }
            }

            else -> {
                // no entry, make sure any entry remains is removed
                remove(key)
                return null
            }
        }
    }

    /**
     * Put an entry into the storage.
     *
     * @param key  the key to store
     * @param data entry to store
     * @param serialize function to write to disk
     * @return true if the entry was written
     */
    override fun <T> put(
        key: String,
        data: T,
        serialize: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean {
        memoryCache.put(key, data)
        val didWrite = writeDiskEntry(key, data, serialize)
        return didWrite
    }

    /**
     * Checks if an entry is present
     *
     * @return true if the entry is present
     */
    override fun contains(key: String): Boolean {
        return try {
            memoryCache.contains(key) || inReadLock(key) { getEntryFile(key).exists() }
        } catch (e: Exception) {
            logger.w(e, LOG_TAG, "Error contains entry for key - $key")
            false
        }
    }

    /**
     * Remove an entry from the storage
     */
    override fun remove(key: String) {
        try {
            inWriteLock<Any>(key) {
                getEntryFile(key).deleteIfExists()
            }
        } catch (e: Exception) {
            logger.w(e, LOG_TAG, "Error remove entry for key - $key")
        }

        locks.remove(key)
        memoryCache.remove(key)
    }

    /**
     * Deletes the entire storage
     */
    override fun clear() {
        synchronized(masterLock) {
            dir.listDirectoryEntries().forEach {
                deleteFileAndLog(it)
            }
        }
        memoryCache.clear()
        locks.clear()
    }

    override fun getKeys(): List<String> {
        synchronized(masterLock) {
            val entries = dir.listDirectoryEntries()
            val keys = ArrayList<String>()
            entries.forEach {
                try {
                    val header = StorageHeader.read(it.source().buffer())
                    keys.add(header.key)
                } catch (e: Exception) {
                    logger.w(e,LOG_TAG, "Error getting all keys")
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

    private fun rwLockForKey(key: String): ReadWriteLock {
        return locks.getOrPut(key) { ReentrantReadWriteLock() }
    }

    @Throws(Exception::class)
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

    @Throws(Exception::class)
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

    private fun <T> readEntry(
        key: String,
        deserializer: (bufferedSource: BufferedSource) -> T,
    ): T? {
        return try {
            inReadLock(key) {
                getEntryFile(key).source().buffer().use { closable ->
                    StorageHeader.read(closable) // Read storageHeader
                    deserializer(closable) // read entry
                }
            }
        } catch (e: Exception) {
            // on error reading remove the entry
            logger.w(e,LOG_TAG, "Error reading entry for key - $key, deleting entry...")
            remove(key)
            null
        }
    }

    private fun <T> writeDiskEntry(
        key: String,
        entry: T,
        serialize: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean {
        try {
            return inReadLock(key) {
                getEntryFile(key).sink().buffer().use { closable ->
                    val storageHeader =
                        StorageHeader(VERSION, key, Date(System.currentTimeMillis()))
                    storageHeader.write(closable) // Write storageHeader
                    serialize(entry, closable) // write entry
                    closable.flush()
                }
                true
            }
        } catch (e: Exception) {
            // on error writing remove the entry
            remove(key)
            logger.w(e,LOG_TAG, "Error writing entry for key - $key")
            return false
        }
    }

    /**
     * Header for entries, this contains metadata such as the version of the memoryCache and the
     * date created.
     */
    internal class StorageHeader(val version: Int, val key: String, val created: Date) {

        // Write the entryHeader to disk, do NOT close the stream
        @Throws(Exception::class)
        fun write(sink: BufferedSink) {
            sink.writeInt(version)

            // write key value
            val keyData = key.encodeUtf8()
            sink.writeInt(keyData.size)
            sink.write(keyData)
            sink.writeLong(created.time)
            sink.flush()
        }

        companion object {

            @Throws(Exception::class)
            fun read(source: BufferedSource): StorageHeader {
                // first 4 bytes are always the version (32bit int)
                when (val version = source.readInt()) {
                    1 -> {
                        // read back key value
                        val keyStringLength = source.readInt()
                        val byteStringKey = source.readByteString(keyStringLength.toLong())
                        val key = byteStringKey.utf8()

                        val created = Date(source.readLong())
                        return StorageHeader(version, key, created)
                    }

                    else -> throw IllegalArgumentException(
                        "Error reading file storageHeader, verson - [$version] not recognised",
                    )
                }
            }
        }
    }

    private fun deleteFileAndLog(path: Path?) {
        try {
            path?.deleteIfExists()
            logger.d(LOG_TAG, "deleted file ${path?.name}")
        } catch (error: Exception) {
            logger.w(error, LOG_TAG, "error deleting file")
        }
    }
}