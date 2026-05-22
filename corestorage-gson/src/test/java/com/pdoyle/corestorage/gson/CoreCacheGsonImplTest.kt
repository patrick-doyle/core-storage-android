package com.pdoyle.corestorage.gson

import com.google.common.truth.Truth
import com.pdoyle.corestorage.TestCoreCache
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class CoreCacheGsonImplTest {

    private val testStorageData = TestStorageData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    @Test
    fun writeAndReadFileSuccess() {
        val coreCache = createCache()

        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        val readData = coreCache.get(testKey, TestStorageData::class.java)

        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @Test
    fun writeAndDeleteFileSuccess() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.get(testKey, TestStorageData::class.java)).isNull()
    }

    @Test
    fun readFileKeyMissing() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        Truth.assertThat(coreCache.get(testKey2, TestStorageData::class.java)).isNull()
    }

    @Test
    fun writeAndReadFileKeyMissingOrNull() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        val readData = coreCache.get(testKey2, TestStorageData::class.java)

        Truth.assertThat(readData).isNull()
    }

    @Test
    fun contains() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        val cacheContainsKey1 = coreCache.contains(testKey)
        val cacheContainsKey2 = coreCache.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @Test
    fun remove() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @Test
    fun clear() {
        val coreCache = createCache()

        coreCache.clear()

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @Test
    fun getKeys() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)
        coreCache.putNoExpire(testKey2, testStorageData, TestStorageData::class.java)

        val keys = coreCache.getKeys()

        Truth.assertThat(keys).containsExactlyElementsIn(listOf(testKey, testKey2))
    }

    @Test
    fun defaultValue() {
        val coreCache = createCache()
        val defaultTestStorageData = TestStorageData(stringKey = "default value")
        val readData = coreCache.getOrDefault(testKey, defaultTestStorageData)

        Truth.assertThat(readData).isEqualTo(defaultTestStorageData)
    }

    @Test
    fun noExpire() {
        val coreCache = createCache()
        coreCache.putNoExpire(testKey, testStorageData, TestStorageData::class.java)

        val readData = coreCache.get(testKey, TestStorageData::class.java)
        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @Test
    fun expires() {
        val coreCache = createCache()
        coreCache.put(testKey, testStorageData, Instant.now().minusMillis(500), TestStorageData::class.java)

        val cacheContainsKey1 = coreCache.contains(testKey)
        Truth.assertThat(cacheContainsKey1).isFalse()

        val readData = coreCache.get(testKey, TestStorageData::class.java)
        Truth.assertThat(readData).isNull()
    }

    @Test
    fun expiresIn() {
        val coreCache = createCache()
        coreCache.putExpiresIn(testKey, testStorageData, 10.milliseconds, TestStorageData::class.java)
        coreCache.putExpiresIn(testKey2, testStorageData, 100.milliseconds, TestStorageData::class.java)

        Thread.sleep(50)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get(testKey, TestStorageData::class.java)).isNull()
        Truth.assertThat(coreCache.get(testKey2, TestStorageData::class.java)).isNotNull()
    }

    @Test
    fun expiresAt() {
        val coreCache = createCache()
        coreCache.put(testKey, testStorageData, Instant.now().minusMillis(5000), TestStorageData::class.java)
        coreCache.put(testKey2, testStorageData, Instant.now().plusMillis(5000), TestStorageData::class.java)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get(testKey, TestStorageData::class.java)).isNull()
        Truth.assertThat(coreCache.get(testKey2, TestStorageData::class.java)).isNotNull()
    }

    private fun createCache(): CoreCacheGson {
        return CoreCacheGsonImpl(TestCoreCache(), testGson())
    }
}