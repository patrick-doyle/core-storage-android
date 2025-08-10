package com.pdoyle.corestorage

import android.annotation.SuppressLint
import android.content.Context
import com.pdoyle.corestorage.log.CoreStorageAndroidLogger
import com.pdoyle.corestorage.log.CoreStorageLog
import com.pdoyle.corestorage.log.CoreStorageLogger
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class StorageProvider private constructor(
    private val context: Context,
    private val logger: CoreStorageLogger,
) {

    private val memoryCache: MemoryCache by lazy { DefaultMemoryCache() }

    private val log: CoreStorageLog by lazy { CoreStorageLog(logger) }

    private val coreCache: CoreCache by lazy {
        val cacheFolder = Paths.get(context.cacheDir.path, CACHE_DIR)
        cacheFolder.createDirectories()
        CoreCacheImpl(cacheFolder, memoryCache, CACHE_SIZE, log)
    }

    private val coreStorage: CoreStorage by lazy {
        val storageFolder = Paths.get(context.filesDir.path, STORAGE_DIR)
        storageFolder.createDirectories()
        CoreStorageImpl(storageFolder, memoryCache, log)
    }

    fun memoryCache(): MemoryCache = memoryCache

    fun cache(): CoreCache = coreCache

    fun storage(): CoreStorage = coreStorage

    companion object {

        private const val CACHE_DIR = "core_cache"
        private const val STORAGE_DIR = "core_storage"
        private const val CACHE_SIZE = 50 * 1000 * 1000 // ~50MB DiskCacheSize

        @SuppressLint("StaticFieldLeak")
        private var instance: StorageProvider? = null

        fun get(context: Context): StorageProvider {
            return get(context, CoreStorageAndroidLogger())
        }

        fun get(context: Context, logger: CoreStorageLogger): StorageProvider {
            return instance ?: synchronized(this) {
                StorageProvider(context.applicationContext, logger).also { instance = it }
            }
        }
    }
}
