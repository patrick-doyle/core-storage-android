package com.pdoyle.corestorage.moshi

import com.pdoyle.corestorage.CoreStorageMigration


/**
 * Gets the data from the storage
 */
inline fun <reified T> CoreStorageMoshi.getOrDefault(key: String, defaultValue: T): T {
    return get(key, defaultValue::class.java) ?: defaultValue
}

/**
 * Gets the data from the storage.
 *
 * @param migration used for migrating data from old storage systems
 */
fun <T> CoreStorageMoshi.getWithMigration(
    key: String,
    migration: CoreStorageMigration<T>,
    clazz: Class<T>,
): T? {
    val data = get(key, clazz)
    if (data != null) {
        return data
    }

    val migrated = migration.get()
    return if (migrated != null) {
        put(key, migrated, clazz)
        migration.remove()
        migrated
    } else {
        get(key, clazz)
    }
}

/**
 * Gets the data from the storage.
 *
 * @param migration used for migrating data from old storage systems
 */
inline fun <reified T> CoreStorageMoshi.getWithMigrationDefault(
    key: String,
    migration: CoreStorageMigration<T>,
    defaultValue: T
): T {
    return getWithMigration(key, migration, defaultValue::class.java) ?: defaultValue
}