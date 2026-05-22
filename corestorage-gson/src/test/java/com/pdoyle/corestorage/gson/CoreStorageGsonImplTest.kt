package com.pdoyle.corestorage.gson

import com.google.common.truth.Truth
import com.pdoyle.corestorage.CoreStorageMigration
import com.pdoyle.corestorage.TestCoreStorage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoreStorageGsonImplTest {

    private val testStorageData = TestStorageData()
    private val testKey = "cache_key"
    private val testKey2 = "cache_key_2"

    private val coreStorage = TestCoreStorage()

    @BeforeEach
    fun setUp() {
        coreStorage.clear()
    }

    @Test
    fun readFileKeyMissing() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)

        Truth.assertThat(coreStorage.get(testKey2, TestStorageData::class.java)).isNull()
    }

    @Test
    fun writeAndReadFileKeyMissingOrNull() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)

        val readData = coreStorage.get(testKey2, TestStorageData::class.java)

        Truth.assertThat(readData).isNull()
    }

    @Test
    fun contains() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)

        val cacheContainsKey1 = coreStorage.contains(testKey)
        val cacheContainsKey2 = coreStorage.contains(testKey2)

        Truth.assertThat(cacheContainsKey1).isTrue()
        Truth.assertThat(cacheContainsKey2).isFalse()
    }

    @Test
    fun remove() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)

        coreStorage.remove(testKey)

        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @Test
    fun clear() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)

        coreStorage.clear()
        Truth.assertThat(coreStorage.contains(testKey)).isFalse()
    }

    @Test
    fun getKeys() {
        val coreStorage = storageInstance()
        coreStorage.put(testKey, testStorageData, TestStorageData::class.java)
        coreStorage.put(testKey2, testStorageData, TestStorageData::class.java)

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
        val readData = coreStorage.getWithMigration(testKey, migration, TestStorageData::class.java)

        val secondReadData = coreStorage.get(testKey, TestStorageData::class.java)

        Truth.assertThat(readData).isEqualTo(migratedData)
        Truth.assertThat(secondReadData).isEqualTo(migratedData)
    }

    @Test
    fun getWithMigrationWithDefault() {
        val coreStorage = storageInstance()
        val defaultData = TestStorageData(stringKey = "default value 2")
        val migration = object : CoreStorageMigration<TestStorageData?> {
            override fun get(): TestStorageData? {
                return null
            }

            override fun remove() {
                // ignored
            }
        }
        val readData = coreStorage.getWithMigrationDefault(testKey, migration, defaultData)

        Truth.assertThat(readData).isEqualTo(defaultData)
    }

    private fun storageInstance() =
        CoreStorageGsonImpl(coreStorage, testGson())
}