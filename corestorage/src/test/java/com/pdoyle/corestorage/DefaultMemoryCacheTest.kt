package com.pdoyle.corestorage

import com.google.common.truth.Truth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultMemoryCacheTest {

    private val testStorageData1 = "test-data-1"
    private val testStorageData2 = "test-data-2"

    private val testKey1 = "test-key-1"
    private val testKey2 = "test-key-2"

    private lateinit var memoryCache: MemoryCache
    private val cacheMaxSize = 5

    @BeforeEach
    fun setUp() {
        memoryCache = DefaultMemoryCache(sizeLimit = cacheMaxSize)
    }

    @Test
    fun readAndWrite() {
        memoryCache.put(testKey1, testStorageData1)

        val data = memoryCache.get<String>(testKey1)

        Truth.assertThat(data).isEqualTo(testStorageData1)
    }

    @Test
    fun contains() {
        memoryCache.put(testKey1, testStorageData1)

        val contains1 = memoryCache.contains(testKey1)
        val contains2 = memoryCache.contains(testKey2)

        Truth.assertThat(contains1).isTrue()
        Truth.assertThat(contains2).isFalse()
    }

    @Test
    fun remove() {
        memoryCache.put(testKey1, testStorageData1)

        memoryCache.remove(testKey1)
        val contains1 = memoryCache.contains(testKey1)

        Truth.assertThat(contains1).isFalse()
    }

    @Test
    fun clear() {
        memoryCache.put(testKey1, testStorageData1)

        memoryCache.clear()
        val contains1 = memoryCache.contains(testKey1)

        Truth.assertThat(contains1).isFalse()
    }

    @Test
    fun getOrPutEntryExists() {
        memoryCache.put(testKey1, testStorageData1)

        val data = memoryCache.getOrPut(testKey1) {
            testStorageData2
        }

        Truth.assertThat(data).isEqualTo(testStorageData1)
    }

    @Test
    fun getOrPutEntryMissing() {
        val data = memoryCache.getOrPut(testKey1) {
            testStorageData2
        }

        Truth.assertThat(data).isEqualTo(testStorageData2)
    }

    @Test
    fun getOrDefaultEntryExists() {
        memoryCache.put(testKey1, testStorageData1)

        val data = memoryCache.getOrDefault(testKey1, testStorageData2)

        Truth.assertThat(data).isEqualTo(testStorageData1)
    }

    @Test
    fun getOrDefaultEntryMissing() {
        val data = memoryCache.getOrDefault(testKey1, testStorageData2)

        Truth.assertThat(data).isEqualTo(testStorageData2)
    }

    @Test
    fun lruEvictsEldestEntry() {
        memoryCache.put(testKey1, testStorageData1)
        // overfill cache
        for (i in 1..cacheMaxSize) {
            memoryCache.put("test-fill-key-$i", "test-fill-data-$i")
        }

        memoryCache.put(testKey2, testStorageData2)

        val contains1 = memoryCache.contains(testKey1)
        val contains2 = memoryCache.contains(testKey2)

        Truth.assertThat(contains1).isFalse()
        Truth.assertThat(contains2).isTrue()
    }
}
