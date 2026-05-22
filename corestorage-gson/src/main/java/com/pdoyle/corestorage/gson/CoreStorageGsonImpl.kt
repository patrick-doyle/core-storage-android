package com.pdoyle.corestorage.gson

import com.google.gson.Gson
import com.pdoyle.corestorage.CoreStorage

internal class CoreStorageGsonImpl(
    private val coreStorage: CoreStorage,
    private val gson: Gson
) : CoreStorageGson {

    override fun <T> get(key: String, clazz: Class<T>): T? {
        return coreStorage.get(key) { source ->
            gson.fromJson(source.inputStream().reader(), clazz)
        }
    }

    override fun <T> put(key: String, data: T, clazz: Class<T>): Boolean {
        return coreStorage.put(key, data) { value, sink ->
            val writer = sink.outputStream().writer()
            gson.toJson(value, clazz, writer)
            writer.flush()
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