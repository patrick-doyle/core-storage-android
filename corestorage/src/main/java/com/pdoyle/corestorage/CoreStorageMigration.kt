package com.pdoyle.corestorage

/**
 * Migration used for Migration data from another storage system
 */
interface CoreStorageMigration<T> {

    fun get(): T?

    fun remove()
}