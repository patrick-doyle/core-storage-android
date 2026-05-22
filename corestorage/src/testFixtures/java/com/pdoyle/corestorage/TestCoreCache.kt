package com.pdoyle.corestorage

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import java.time.Instant

/**
 * Test implementation for [CoreCache]. Used as a test fixture. This causes does not have
 * LRU support and is in memory for tests
 */
class TestCoreCache: CoreCache {

    private val cache = mutableMapOf<String, Pair<Buffer, Instant>?>()

    override fun <T> get(key: String, deserializer: (BufferedSource) -> T): T? {
        val entry = cache[key] ?: return null
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.first.let(block = deserializer)
    }

    override fun <T> put(key: String, data: T, expires: Instant, serialize: (T, BufferedSink) -> Unit): Boolean {
        val buffer = Buffer()
        serialize(data, buffer)
        cache.put(key, Pair(buffer, expires))
        return true
    }

    override fun contains(key: String): Boolean {
        val entry = cache[key] ?: return false
        if (entry.isExpired()) {
            cache.remove(key)
            return false
        } else {
            return true
        }
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

    private fun  Pair<Buffer, Instant>.isExpired(): Boolean {
        return second.isBefore(Instant.now())
    }
}