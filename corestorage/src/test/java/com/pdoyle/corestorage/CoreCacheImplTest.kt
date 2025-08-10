package com.pdoyle.corestorage

import com.google.common.truth.Truth
import com.pdoyle.corestorage.log.CoreStorageLog
import kotlinx.serialization.Serializable
import okio.IOException
import okio.buffer
import okio.source
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.time.Duration.Companion.milliseconds

class CoreCacheImplTest {

    private val testData = TestData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    private val defaultCache = DefaultMemoryCache()

    private lateinit var dir: Path
    private val cacheSize = 5 * 1000 * 1000 // 5MB DiskCacheSize for testing
    private val logger = CoreStorageLog(CoreStorageTestLogger)

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
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)

        coreCache.putNoExpire(testKey, testData)

        val readData = coreCache.get<TestData>(testKey)

        Truth.assertThat(readData).isEqualTo(testData)
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun writeAndDeleteFileSuccess(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.get<TestData>(testKey)).isNull()
    }

    @Test
    fun errorFileWrite() {
        val storage = CoreCacheImpl(dir, NoMemoryCache, cacheSize, logger)
        val didPut = storage.putNoExpire(testKey, testData, serialize = { _, _ ->
            throw IOException("Fake error writing")
        })

        Truth.assertThat(didPut).isFalse()
        Truth.assertThat(storage.get<TestData>(testKey)).isNull()
    }

    @Test
    fun errorFileRead() {
        val storage = CoreCacheImpl(dir, NoMemoryCache, cacheSize, logger)
        storage.putNoExpire(testKey, testData)

        val defaultData = testData.copy(stringKey = "test_default_key")

        val readDataNull = storage.getOrDefault(testKey, defaultData, deserializer = {
            throw IOException("Fake error reading")
        })

        Truth.assertThat(readDataNull).isEqualTo(defaultData)
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun readFileKeyMissing(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        Truth.assertThat(coreCache.get<TestData>(testKey2)).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun writeAndReadFileKeyMissingOrNull(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        val readData = coreCache.get<TestData>(testKey2)

        Truth.assertThat(readData).isNull()
    }

    @Test
    fun writeDataIsCachedInMemory() {
        val memoryCache = DefaultMemoryCache()
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        val cacheContains = memoryCache.contains(testKey)

        Truth.assertThat(cacheContains).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun contains(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        val cacheContainsKey1 = coreCache.contains(testKey)
        val cacheContainsKey2 = coreCache.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun remove(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun clear(memoryCache: MemoryCache) {
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)

        coreCache.clear()

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @Test
    fun customSerializer() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

        val testDate = Date()
        coreCache.putNoExpire(testKey, testDate, serialize = { date, sink ->
            sink.writeUtf8(formatter.format(date))
        })

        val readDate = coreCache.get(testKey, deserializer = { source ->
            formatter.parse(source.readUtf8())!!
        })

        Truth.assertThat(readDate).isEqualTo(testDate)
    }

    @Test
    fun getKeys() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)
        coreCache.putNoExpire(testKey2, testData)

        val keys = coreCache.getKeys()

        Truth.assertThat(keys).containsExactlyElementsIn(listOf(testKey, testKey2))
    }

    @Test
    fun defaultValue() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        val defaultTestData = TestData(stringKey = "default value")
        val readData = coreCache.getOrDefault(testKey, defaultTestData)

        Truth.assertThat(readData).isEqualTo(defaultTestData)
    }

    @Test
    fun noExpire() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        coreCache.putNoExpire(testKey, testData)

        val readData = coreCache.get<TestData>(testKey)
        Truth.assertThat(readData).isEqualTo(testData)
    }

    @Test
    fun expires() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        coreCache.put(testKey, testData, Instant.now().minusMillis(500))

        val cacheContainsKey1 = coreCache.contains(testKey)
        Truth.assertThat(cacheContainsKey1).isFalse()

        val readData = coreCache.get<TestData>(testKey)
        Truth.assertThat(readData).isNull()
    }

    @Test
    fun expiresIn() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        coreCache.putExpiresIn(testKey, testData, 10.milliseconds)
        coreCache.putExpiresIn(testKey2, testData, 100.milliseconds)

        Thread.sleep(50)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get<TestData>(testKey)).isNull()
        Truth.assertThat(coreCache.get<TestData>(testKey2)).isNotNull()
    }

    @Test
    fun expiresAt() {
        val coreCache = CoreCacheImpl(dir, defaultCache, cacheSize, logger)
        coreCache.put(testKey, testData, Instant.now().minusMillis(5000))
        coreCache.put(testKey2, testData, Instant.now().plusMillis(5000))

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get<TestData>(testKey)).isNull()
        Truth.assertThat(coreCache.get<TestData>(testKey2)).isNotNull()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun lruRemovedEntries(memoryCache: MemoryCache) {
        val cacheSize = 5 * 1000 // limit size to 5 kb
        val coreCache = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        val entry1kb = this.javaClass.classLoader?.getResourceAsStream("1k_entry.txt")
            ?.source()?.buffer()?.readUtf8()
            ?: throw IllegalStateException("1k_entry.txt file not found")

        (1..6).forEach { number ->
            coreCache.putNoExpire("${testKey}_$number", entry1kb)
        }

        Truth.assertThat(coreCache.contains("${testKey}_1")).isFalse()
        Truth.assertThat(coreCache.contains("${testKey}_2")).isFalse()
        Truth.assertThat(coreCache.contains("${testKey}_3")).isTrue()
        Truth.assertThat(coreCache.contains("${testKey}_4")).isTrue()
        Truth.assertThat(coreCache.contains("${testKey}_5")).isTrue()
        Truth.assertThat(coreCache.contains("${testKey}_6")).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideMemoryCacheImpls")
    fun lruRemovedEntriesDifferentInstancesOfCache(memoryCache: MemoryCache) {
        val cacheSize = 5 * 1000 // limit size to 5 kb
        val coreCacheImpl = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        val coreCache2 = CoreCacheImpl(dir, memoryCache, cacheSize, logger)
        val entry1KB = this.javaClass.classLoader?.getResourceAsStream("1k_entry.txt")
            ?.source()?.buffer()?.readUtf8()
            ?: throw IllegalStateException("1k_entry.txt file not found")

        (1..6).forEach { number ->
            coreCacheImpl.putNoExpire(
                "${testKey}_$number",
                entry1KB,
                storageJsonSerializer(),
            )
        }

        Truth.assertThat(coreCache2.contains("${testKey}_1")).isFalse()
        Truth.assertThat(coreCache2.contains("${testKey}_2")).isFalse()
        Truth.assertThat(coreCache2.contains("${testKey}_3")).isTrue()
        Truth.assertThat(coreCache2.contains("${testKey}_4")).isTrue()
        Truth.assertThat(coreCache2.contains("${testKey}_5")).isTrue()
        Truth.assertThat(coreCache2.contains("${testKey}_6")).isTrue()
    }

    companion object {

        @JvmStatic
        fun provideMemoryCacheImpls(): List<MemoryCache> {
            return listOf(
                DefaultMemoryCache(),
                NoMemoryCache,
            )
        }
    }

    @Serializable
    private data class TestData(
        private val stringKey: String = "string-value",
        private val numberKey: Int = 4322,
    )
}
