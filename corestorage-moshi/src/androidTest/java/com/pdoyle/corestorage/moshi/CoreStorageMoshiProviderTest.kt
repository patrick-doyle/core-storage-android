package com.pdoyle.corestorage.moshi

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.pdoyle.corestorage.CoreStorageProvider
import com.pdoyle.corestorage.log.CoreStorageAndroidLogger
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Before
import org.junit.Test

class CoreStorageMoshiProviderTest {

    private val testData = TestData()
    private val testKey = "cache_key"

    private lateinit var coreStorageProvider: CoreStorageMoshiProvider

    @Before
    fun setUp() {
        coreStorageProvider = CoreStorageMoshiProvider.get(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build(),
        )
    }

    @Test
    fun getCacheFromProvider() {
        val cache = coreStorageProvider.cache()

        cache.putNoExpire(testKey, testData, TestData::class.java)

        val returned = cache.get(testKey, TestData::class.java)
        cache.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    @Test
    fun getStorageFromProvider() {
        val memoryCache = coreStorageProvider.memoryCache()

        memoryCache.put(testKey, testData)

        val returned = memoryCache.get<TestData>(testKey)
        memoryCache.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    @Test
    fun getMemoryCache() {
        val storage = coreStorageProvider.storage()

        storage.put(testKey, testData, TestData::class.java)

        val returned = storage.get(testKey, TestData::class.java)
        storage.clear()

        Truth.assertThat(returned).isEqualTo(testData)
    }

    data class TestData(
        val stringKey: String = "string-value",
        val numberKey: Int = 4322,
    )
}