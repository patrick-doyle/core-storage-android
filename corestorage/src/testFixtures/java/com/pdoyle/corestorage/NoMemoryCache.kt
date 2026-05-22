package com.pdoyle.corestorage

object NoMemoryCache : MemoryCache {

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
    }

    override fun remove(key: String) {
    }

    override fun contains(key: String): Boolean {
        return false
    }

    override fun trim() {
    }
}