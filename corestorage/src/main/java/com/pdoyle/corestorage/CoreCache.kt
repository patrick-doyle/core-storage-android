package com.pdoyle.corestorage

import okio.BufferedSink
import okio.BufferedSource
import java.time.Instant

/**
 * Cache interface, that stores in the app cache folders. This is wiped when
 * the user clears the cache, only store temp data here
 */
interface CoreCache {

    /**
     * Get data from the cache, null if data does not exist
     */
    fun <T> get(
        key: String,
        deserializer: (bufferedSource: BufferedSource) -> T,
    ): T?

    /**
     * Save a value to the cache. Once the time in expires has passed the entry will be
     * removed from the cache
     *
     * @return true if entry was saved, false otherwise.
     */
    fun <T> put(
        key: String,
        data: T,
        expires: Instant,
        serialize: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean

    /**
     * returns true if a key is found
     */
    operator fun contains(key: String): Boolean

    /**
     * removes and entry for a given key
     */
    fun remove(key: String)

    /**
     * Clears the whole storage
     */
    fun clear()

    /**
     * Gets all the keys in the cache
     */
    fun getKeys(): List<String>
}
