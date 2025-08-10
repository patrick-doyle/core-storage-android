package com.pdoyle.corestorage

import okio.BufferedSink
import okio.BufferedSource

/**
 * Gets the data from the storage or returns default if no entry has been found
 */
fun <T> CoreStorage.getOrDefault(
    key: String,
    defaultValue: T,
    deserializer: (source: BufferedSource) -> T,
): T {
    return get(key, deserializer) ?: defaultValue
}

/**
 * Gets the data from the storage using @see [storageJsonDeserializer]
 */
inline fun <reified T> CoreStorage.getOrDefault(key: String, defaultValue: T): T {
    return get(key) ?: defaultValue
}

/**
 * Gets the data from the storage using @see [storageJsonDeserializer]
 */
inline fun <reified T> CoreStorage.get(key: String): T? {
    return get(key, storageJsonDeserializer())
}

/**
 * Puts the data in the storage using @see [storageJsonDeserializer]
 */
inline fun <reified T> CoreStorage.put(key: String, data: T) {
    put(key, data, storageJsonSerializer())
}

/**
 *
 * @param migration used for migrating data from old storage systems
 */
inline fun <reified T> CoreStorage.getWithMigration(
    key: String,
    migration: CoreStorage.Migration<T>,
): T? {
    return getWithMigration(key, storageJsonDeserializer(), storageJsonSerializer(), migration)
}

/**
 * Gets the data from the storage.
 *
 * @param migration used for migrating data from old storage systems
 */
fun <T> CoreStorage.getWithMigration(
    key: String,
    deserializer: (source: BufferedSource) -> T,
    serialize: (data: T, sink: BufferedSink) -> Unit,
    migration: CoreStorage.Migration<T>,
): T? {
    val data = get(key, deserializer)
    if (data != null) {
        return data
    }

    val migrated = migration.get()
    return if (migrated != null) {
        put(key, migrated, serialize)
        migration.remove()
        migrated
    } else {
        get(key, deserializer)
    }
}
