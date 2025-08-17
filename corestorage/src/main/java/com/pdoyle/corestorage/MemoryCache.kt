package com.pdoyle.corestorage

interface MemoryCache {

    /**
     * @return item fom the cache or null not item for the given key
     */
    fun <T> get(key: String): T?

    /**
     * @return item fom the cache or the defaultValue for the given key
     */
    fun <T> getOrDefault(key: String, defaultValue: T): T

    /**
     * Gets and item from the cache. If the cache does not have am item for the given key
     * the putBlock will be called and its return value will be stored in the cache and returned
     *
     * @see MutableMap.getOrPut
     *
     * @return item fom the cache or the putBlock for the given key
     */
    fun <T : Any> getOrPut(key: String, putBlock: () -> T): T

    /**
     * puts a key/value pair in the cache
     */
    fun <T> put(key: String, data: T): T

    /**
     * clears the cache
     */
    fun clear()

    /**
     * removes a key/value pair from the cache
     */
    fun remove(key: String)

    /**
     * @return true the cache contains an key, false otherwise
     */
    fun contains(key: String): Boolean

    /**
     * @return true the cache contains an key, false otherwise
     */
    fun trim()
}

/**
 * Default impl of the memory cache, lru based
 */
internal class DefaultMemoryCache(val sizeLimit: Int = 25) : MemoryCache {

    private val map = object : LinkedHashMap<String, Any>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>?): Boolean {
            return size >= sizeLimit
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String): T? {
        return map[key] as? T
    }

    override fun <T> getOrDefault(key: String, defaultValue: T): T {
        return get(key) ?: defaultValue
    }

    override fun <T : Any> getOrPut(key: String, putBlock: () -> T): T {
        val data = get<T>(key)
        return if (data != null) {
            data
        } else {
            val putData = putBlock()
            map[key] = putData
            putData
        }
    }

    override fun <T> put(key: String, data: T): T {
        map[key] = data as Any
        return data
    }

    override fun clear() {
        map.clear()
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun contains(key: String): Boolean {
        return map.contains(key)
    }

    override fun trim() {
        (0 until (sizeLimit / 2)).forEach { i ->
            map.remove(map.keys.last())
        }
    }

}
