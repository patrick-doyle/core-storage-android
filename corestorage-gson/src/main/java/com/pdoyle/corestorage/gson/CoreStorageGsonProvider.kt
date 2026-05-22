package com.pdoyle.corestorage.gson

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.pdoyle.corestorage.CoreStorageProvider
import com.pdoyle.corestorage.MemoryCache
import com.pdoyle.corestorage.log.CoreStorageAndroidLogger
import com.pdoyle.corestorage.log.CoreStorageLogger

class CoreStorageGsonProvider internal constructor(
    private val coreStorageProvider: CoreStorageProvider,
    private val gson: Gson,
) {

    private val coreCache: CoreCacheGson by lazy {
        CoreCacheGsonImpl(coreCache = coreStorageProvider.cache(), gson = gson)
    }

    private val coreStorage: CoreStorageGson by lazy {
        CoreStorageGsonImpl(coreStorage = coreStorageProvider.storage(), gson = gson)
    }

    fun memoryCache(): MemoryCache = coreStorageProvider.memoryCache()

    fun cache(): CoreCacheGson = coreCache

    fun storage(): CoreStorageGson = coreStorage

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var instance: CoreStorageGsonProvider? = null

        fun get(context: Context, gson: Gson): CoreStorageGsonProvider {
            return get(context, gson, CoreStorageAndroidLogger)
        }

        fun get(
            context: Context,
            gson: Gson,
            logger: CoreStorageLogger
        ): CoreStorageGsonProvider {
            return instance ?: synchronized(this) {
                CoreStorageGsonProvider(CoreStorageProvider.get(context, logger), gson)
            }
        }
    }
}