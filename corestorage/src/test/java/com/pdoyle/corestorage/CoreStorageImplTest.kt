package com.pdoyle.corestorage

import com.google.common.truth.Truth
import com.pdoyle.corestorage.log.CoreStorageLog
import okio.IOException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

class CoreStorageImplTest {

    private val testStorageData = TestStorageData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    private val defaultCache = DefaultMemoryCache()

    private val logger = CoreStorageLog(CoreStorageTestLogger)

    private lateinit var dir: Path

    @BeforeEach
    fun setUp() {
        dir = Files.createTempDirectory("temp")
        dir.deleteIfExists()
        dir.createDirectories()
        defaultCache.clear()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun writeAndReadFileSuccess(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)

        coreStorage.put(testKey, testStorageData)

        val readData = coreStorage.get<TestStorageData>(testKey)
        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun writeAndDeleteFileSuccess(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        coreStorage.remove(testKey)

        Truth.assertThat(coreStorage.get<TestStorageData>(testKey)).isNull()
    }

    @Test
    fun errorFileWrite() {
        val storage = storageInstance()
        val didPut = storage.put(testKey, testStorageData, serialize = { _, _ ->
            throw IOException("Fake error writing")
        })

        Truth.assertThat(didPut).isFalse()
        Truth.assertThat(storage.get<TestStorageData>(testKey)).isNull()
    }

    @Test
    fun errorFileRead() {
        val storage = storageInstance()
        storage.put(testKey, testStorageData)

        val defaultData = testStorageData.copy(stringKey = "test_default_key")

        val readDataNull = storage.getOrDefault(testKey2, defaultData, deserializer = {
            throw IOException("Fake error reading")
        })

        Truth.assertThat(readDataNull).isEqualTo(defaultData)
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun readFileKeyMissing(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        Truth.assertThat(coreStorage.get<TestStorageData>(testKey2)).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun writeAndReadFileKeyMissingOrNull(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        val readData = coreStorage.get<TestStorageData>(testKey2)

        Truth.assertThat(readData).isNull()
    }

    @Test
    fun writeDataIsCachedInMemory() {
        val memoryCache = DefaultMemoryCache()
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        val cacheContains = memoryCache.contains(testKey)

        Truth.assertThat(cacheContains).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun contains(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        val cacheContainsKey1 = coreStorage.contains(testKey)
        val cacheContainsKey2 = coreStorage.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun remove(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        coreStorage.remove(testKey)

        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun clear(memoryCache: MemoryCache) {
        val coreStorage = storageInstance(memoryCache)
        coreStorage.put(testKey, testStorageData)

        coreStorage.clear()
        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @Test
    fun customSerializer() {
        val coreStorage = storageInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

        val testDate = Date()
        coreStorage.put(testKey, testDate, serialize = { date, sink ->
            sink.writeUtf8(formatter.format(date))
        })

        val readDate = coreStorage.get(testKey, deserializer = { source ->
            formatter.parse(source.readUtf8())!!
        })

        Truth.assertThat(readDate).isEqualTo(testDate)
    }

    @Test
    fun getKeys() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData)
        coreStorage.put(testKey2, testStorageData)

        val keys = coreStorage.getKeys()

        Truth.assertThat(keys).containsExactlyElementsIn(listOf(testKey, testKey2))
    }

    @Test
    fun defaultValue() {
        val coreStorage = storageInstance()
        val defaultTestStorageData = TestStorageData(stringKey = "default value")
        val readData = coreStorage.getOrDefault(testKey, defaultTestStorageData)

        Truth.assertThat(readData).isEqualTo(defaultTestStorageData)
    }

    @Test
    fun getWithMigration() {
        val coreStorage = storageInstance()
        val migratedData = TestStorageData(stringKey = "default value")
        val migration = object : CoreStorageMigration<TestStorageData> {
            override fun get(): TestStorageData {
                return migratedData
            }

            override fun remove() {
                // ignored
            }
        }
        val readData = coreStorage.getWithMigration(testKey, migration)

        val secondReadData = coreStorage.get<TestStorageData>(testKey)

        Truth.assertThat(readData).isEqualTo(migratedData)
        Truth.assertThat(secondReadData).isEqualTo(migratedData)
    }

    private fun storageInstance(memoryCache: MemoryCache = defaultCache) =
        CoreStorageImpl(dir, memoryCache, logger)

    companion object {

        @JvmStatic
        fun provideMemoryCacheImpls(): List<MemoryCache> {
            return listOf(
                DefaultMemoryCache(),
                NoMemoryCache,
            )
        }
    }
}
