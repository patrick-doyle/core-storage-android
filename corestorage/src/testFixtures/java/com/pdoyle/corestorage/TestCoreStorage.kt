package com.pdoyle.corestorage

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

class TestCoreStorage: CoreStorage {

    private val cache = mutableMapOf<String, Buffer>()

    override fun <T> get(key: String, deserializer: (BufferedSource) -> T): T? {
        return cache[key].let { buffer ->
            buffer?.let(block = deserializer)
        }
    }

    override fun <T> put(key: String, data: T, serialize: (T, BufferedSink) -> Unit): Boolean {
        val buffer = Buffer()
        serialize(data, buffer)
        cache.put(key, buffer)
        return true
    }

    override fun contains(key: String): Boolean {
        return cache.contains(key)
    }

    override fun remove(key: String) {
        cache.remove(key)
    }

    override fun clear() {
        cache.clear()
    }

    override fun getKeys(): List<String> {
        return cache.keys.toList()
    }
}