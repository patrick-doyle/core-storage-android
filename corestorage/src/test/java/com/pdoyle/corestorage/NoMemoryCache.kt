package com.pdoyle.corestorage

internal object NoMemoryCache : MemoryCache {

    override fun <T> get(key: String): T? {
        return null
    }

    override fun <T> getOrDefault(key: String, defaultValue: T): T {
        return defaultValue
    }

    override fun <T : Any> getOrPut(key: String, putBlock: () -> T): T {
        return putBlock()
    }

    override fun <T> put(key: String, data: T): T {
        return data
    }

    override fun clear() {
        return Unit
    }

    override fun remove(key: String) {
        return Unit
    }

    override fun contains(key: String): Boolean {
        return false
    }
}
