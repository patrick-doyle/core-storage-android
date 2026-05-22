package com.pdoyle.corestorage.gson

import com.google.gson.Gson
import com.pdoyle.corestorage.CoreCache
import java.time.Instant

internal class CoreCacheGsonImpl(
    private val coreCache: CoreCache,
    private val gson: Gson
) : CoreCacheGson {

    override fun <T> get(key: String, clazz: Class<T>): T? {
        return coreCache.get(key) { source ->
            gson.fromJson(source.inputStream().reader(), clazz)
        }
    }

    override fun <T> put(key: String, data: T, expires: Instant, clazz: Class<T>): Boolean {
        return coreCache.put(key, data, expires) { value, sink ->
            val writer = sink.outputStream().writer()
            gson.toJson(value, clazz, writer)
            writer.flush()
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