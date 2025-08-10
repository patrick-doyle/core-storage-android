package com.pdoyle.corestorage

import okio.BufferedSink
import okio.BufferedSource

/**
 * Storage interface, that stores in the app data folders. This is not wiped when
 * the user clears the cache
 */
interface CoreStorage {

    /**
     * get the stored data, Returns null if no data present
     */
    fun <T> get(key: String, deserializer: (bufferedSource: BufferedSource) -> T): T?

    /**
     * put data into storage
     */
    fun <T> put(
        key: String,
        data: T,
        serialize: (data: T, sink: BufferedSink) -> Unit,
    ): Boolean

    /**
     * returns true if an entry is found
     */
    fun contains(key: String): Boolean

    /**
     * removes and entry for a given key
     */
    fun remove(key: String)

    /**
     * Clears the whole storage
     */
    fun clear()

    /**
     * Gets all the keys in the storage
     */
    fun getKeys(): List<String>

    /**
     * Migration used for Migration data from another storage system
     */
    interface Migration<T> {

        fun get(): T?
        fun remove()
    }
}
