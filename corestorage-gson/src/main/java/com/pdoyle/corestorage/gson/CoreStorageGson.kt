package com.pdoyle.corestorage.gson

/**
 * Storage interface, that stores in the app data folders. This is not wiped when
 * the user clears the cache
 */
interface CoreStorageGson {

    /**
     * get the stored data, Returns null if no data present
     */
    fun <T> get(key: String, clazz: Class<T>): T?

    /**
     * put data into storage
     */
    fun <T> put(key: String, data: T, clazz: Class<T>): Boolean

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
}