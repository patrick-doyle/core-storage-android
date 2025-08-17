package com.pdoyle.corestorage.moshi

import android.annotation.SuppressLint
import android.content.Context
import com.pdoyle.corestorage.CoreStorageProvider
import com.pdoyle.corestorage.MemoryCache
import com.pdoyle.corestorage.log.CoreStorageAndroidLogger
import com.pdoyle.corestorage.log.CoreStorageLogger
import com.squareup.moshi.Moshi
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class CoreStorageMoshiProvider internal constructor(
    private val coreStorageProvider: CoreStorageProvider,
    private val moshi: Moshi,
) {

    private val coreCache: CoreCacheMoshi by lazy {
        CoreCacheMoshiImpl(coreCache = coreStorageProvider.cache(), moshi = moshi)
    }

    private val coreStorage: CoreStorageMoshi by lazy {
        CoreStorageMoshiImpl(coreStorage = coreStorageProvider.storage(), moshi = moshi)
    }

    fun memoryCache(): MemoryCache = coreStorageProvider.memoryCache()

    fun cache(): CoreCacheMoshi = coreCache

    fun storage(): CoreStorageMoshi = coreStorage

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: CoreStorageMoshiProvider? = null

        fun get(context: Context, moshi: Moshi): CoreStorageMoshiProvider {
            return get(context, moshi, CoreStorageAndroidLogger)
        }

        fun get(
            context: Context,
            moshi: Moshi,
            logger: CoreStorageLogger
        ): CoreStorageMoshiProvider {
            return instance ?: synchronized(this) {
                CoreStorageMoshiProvider(CoreStorageProvider.get(context, logger), moshi)
            }
        }
    }
}
