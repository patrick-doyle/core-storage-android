package com.pdoyle.corestorage.moshi

import com.pdoyle.corestorage.CoreStorage
import com.squareup.moshi.Moshi

internal class CoreStorageMoshiImpl(
    private val coreStorage: CoreStorage,
    private val moshi: Moshi
) : CoreStorageMoshi {

    override fun <T> get(key: String, clazz: Class<T>): T? {
        return coreStorage.get(key) { source ->
            moshi.adapter<T>(clazz).fromJson(source)
        }
    }

    override fun <T> put(key: String, data: T, clazz: Class<T>): Boolean {
        return coreStorage.put(key, data) { value, sink ->
            moshi.adapter<T>(clazz).toJson(sink, value)
        }
    }

    override fun contains(key: String): Boolean {
        return coreStorage.contains(key)
    }

    override fun remove(key: String) {
        return coreStorage.remove(key)
    }

    override fun clear() {
        return coreStorage.clear()
    }

    override fun getKeys(): List<String> {
        return coreStorage.getKeys()
    }
}