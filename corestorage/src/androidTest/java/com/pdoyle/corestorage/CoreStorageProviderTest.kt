package com.pdoyle.corestorage

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.pdoyle.corestorage.log.CoreStorageAndroidLogger
import kotlinx.serialization.Serializable
import org.junit.Before
import org.junit.Test

class CoreStorageProviderTest {

    private val testData = TestData()
    private val testKey = "cache_key"

    private lateinit var coreStorageProvider: CoreStorageProvider

    @Before
    fun setUp() {
        coreStorageProvider = CoreStorageProvider(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CoreStorageAndroidLogger,
            NoMemoryCache)
    }

    @Test
    fun getCacheFromProvider() {
        val cache = coreStorageProvider.cache()

        cache.putNoExpire(testKey, testData)

        val returned = cache.get<TestData>(testKey)
        cache.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    @Test
    fun getStorageFromProvider() {
        val memoryCache = CoreStorageProvider(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CoreStorageAndroidLogger).memoryCache()

        memoryCache.put(testKey, testData)

        val returned = memoryCache.get<TestData>(testKey)
        memoryCache.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    @Test
    fun getMemoryCache() {
        val storage = coreStorageProvider.storage()

        storage.put(testKey, testData)

        val returned = storage.get<TestData>(testKey)
        storage.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    @Serializable
    private data class TestData(
        private val stringKey: String = "string-value",
        private val numberKey: Int = 4322,
    )
}