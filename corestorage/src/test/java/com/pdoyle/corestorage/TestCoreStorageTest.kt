package com.pdoyle.corestorage

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestCoreStorageTest {

    private val testStorageData = TestStorageData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    @Test
    fun writeAndReadFileSuccess() {
        val coreStorage = TestCoreStorage()

        coreStorage.put(testKey, testStorageData)

        val readData = coreStorage.get<TestStorageData>(testKey)

        Truth.assertThat(readData).isEqualTo(testStorageData)
    }

    @Test
    fun writeAndDeleteFileSuccess() {
        val coreStorage = TestCoreStorage()
        coreStorage.put(testKey, testStorageData)

        coreStorage.remove(testKey)

        Truth.assertThat(coreStorage.get<TestStorageData>(testKey)).isNull()
    }

    @Test
    fun contains() {
        val coreStorage = TestCoreStorage()
        coreStorage.put(testKey, testStorageData)

        val cacheContainsKey1 = coreStorage.contains(testKey)
        val cacheContainsKey2 = coreStorage.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @Test
    fun remove() {
        val coreStorage = TestCoreStorage()
        coreStorage.put(testKey, testStorageData)

        coreStorage.remove(testKey)

        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @Test
    fun clear() {
        val coreStorage = TestCoreStorage()

        coreStorage.clear()

        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @Test
    fun customSerializer() {
        val coreStorage = TestCoreStorage()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

        val testDate = Date()
        coreStorage.put(testKey, testDate, serialize = { date, sink ->
            sink.writeUtf8(formatter.format(date))
        })

        val readDate = coreStorage.get(testKey, deserializer = { source ->
            formatter.parse(source.readUtf8())!!
        })

        Truth.assertThat(readDate.toString()).isEqualTo(testDate.toString())
    }

    @Test
    fun getKeys() {
        val coreStorage = TestCoreStorage()
        coreStorage.put(testKey, testStorageData)
        coreStorage.put(testKey2, testStorageData)

        val keys = coreStorage.getKeys()

        Truth.assertThat(keys).containsExactlyElementsIn(listOf(testKey, testKey2))
    }

    @Test
    fun defaultValue() {
        val coreStorage = TestCoreStorage()
        val defaultTestStorageData = TestStorageData(stringKey = "default value")
        val readData = coreStorage.getOrDefault(testKey, defaultTestStorageData)

        Truth.assertThat(readData).isEqualTo(defaultTestStorageData)
    }
}
