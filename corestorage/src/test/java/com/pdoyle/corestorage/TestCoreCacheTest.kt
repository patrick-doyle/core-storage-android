package com.pdoyle.corestorage

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class TestCoreCacheTest {

    private val testStorageData = TestStorageData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    @Test
    fun writeAndReadFileSuccess() {
        val coreCache = TestCoreCache()

        coreCache.putNoExpire(testKey, testStorageData)

        val readData = coreCache.get<TestStorageData>(testKey)

        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @Test
    fun writeAndDeleteFileSuccess() {
        val coreCache = TestCoreCache()
        coreCache.putNoExpire(testKey, testStorageData)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.get<TestStorageData>(testKey)).isNull()
    }

    @Test
    fun contains() {
        val coreCache = TestCoreCache()
        coreCache.putNoExpire(testKey, testStorageData)

        val cacheContainsKey1 = coreCache.contains(testKey)
        val cacheContainsKey2 = coreCache.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @Test
    fun remove() {
        val coreCache = TestCoreCache()
        coreCache.putNoExpire(testKey, testStorageData)

        coreCache.remove(testKey)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @Test
    fun clear() {
        val coreCache = TestCoreCache()

        coreCache.clear()

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
    }

    @Test
    fun customSerializer() {
        val coreCache = TestCoreCache()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

        val testDate = Date()
        coreCache.putNoExpire(testKey, testDate, serialize = { date, sink ->
            sink.writeUtf8(formatter.format(date))
        })

        val readDate = coreCache.get(testKey, deserializer = { source ->
            formatter.parse(source.readUtf8())!!
        })

        Truth.assertThat(readDate.toString()).isEqualTo(testDate.toString())
    }

    @Test
    fun getKeys() {
        val coreCache = TestCoreCache()
        coreCache.putNoExpire(testKey, testStorageData)
        coreCache.putNoExpire(testKey2, testStorageData)

        val keys = coreCache.getKeys()

        Truth.assertThat(keys).containsExactlyElementsIn(listOf(testKey, testKey2))
    }

    @Test
    fun defaultValue() {
        val coreCache = TestCoreCache()
        val defaultTestStorageData = TestStorageData(stringKey = "default value")
        val readData = coreCache.getOrDefault(testKey, defaultTestStorageData)

        Truth.assertThat(readData).isEqualTo(defaultTestStorageData)
    }

    @Test
    fun noExpire() {
        val coreCache = TestCoreCache()
        coreCache.putNoExpire(testKey, testStorageData)

        val readData = coreCache.get<TestStorageData>(testKey)
        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @Test
    fun expires() {
        val coreCache = TestCoreCache()
        coreCache.put(testKey, testStorageData, Instant.now().minusMillis(500))

        val cacheContainsKey1 = coreCache.contains(testKey)
        Truth.assertThat(cacheContainsKey1).isFalse()

        val readData = coreCache.get<TestStorageData>(testKey)
        Truth.assertThat(readData).isNull()
    }

    @Test
    fun expiresIn() {
        val coreCache = TestCoreCache()
        coreCache.putExpiresIn(testKey, testStorageData, 10.milliseconds)
        coreCache.putExpiresIn(testKey2, testStorageData, 100.milliseconds)

        Thread.sleep(50)

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get<TestStorageData>(testKey)).isNull()
        Truth.assertThat(coreCache.get<TestStorageData>(testKey2)).isNotNull()
    }

    @Test
    fun expiresAt() {
        val coreCache = TestCoreCache()
        coreCache.put(testKey, testStorageData, Instant.now().minusMillis(5000))
        coreCache.put(testKey2, testStorageData, Instant.now().plusMillis(5000))

        Truth.assertThat(coreCache.contains(testKey)).isFalse()
        Truth.assertThat(coreCache.contains(testKey2)).isTrue()

        Truth.assertThat(coreCache.get<TestStorageData>(testKey)).isNull()
        Truth.assertThat(coreCache.get<TestStorageData>(testKey2)).isNotNull()
    }
}
