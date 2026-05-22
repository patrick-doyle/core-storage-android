package com.pdoyle.corestorage.moshi

import com.pdoyle.corestorage.CoreCache
import com.squareup.moshi.Moshi
import java.time.Instant

internal class CoreCacheMoshiImpl(
    private val coreCache: CoreCache,
    private val moshi: Moshi
) : CoreCacheMoshi {

    override fun <T> get(key: String, clazz: Class<T>): T? {
        return coreCache.get(key) { source ->
            moshi.adapter<T>(clazz).fromJson(source)
        }
    }

    override fun <T> put(key: String, data: T, expires: Instant, clazz: Class<T>): Boolean {
        return coreCache.put(key, data, expires) { value, sink ->
            moshi.adapter<T>(clazz).toJson(sink, value)
        }
    }

    override fun contains(key: String): Boolean {
        return coreCache.contains(key)
    }

    override fun remove(key: String) {
        return coreCache.remove(key)
    }

    override fun clear() {
        return coreCache.clear()
    }

    override fun getKeys(): List<String> {
        return coreCache.getKeys()
    }
}