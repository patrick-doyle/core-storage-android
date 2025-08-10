package com.pdoyle.corestorage

import java.time.Instant

/**
 * Wrapper around the cached data to hold metadata like expiry, created etc
 */
internal data class CoreCacheEntry<T>(
    val data: T,
    val created: Instant,
    val expires: Instant,
)

internal fun <T> CoreCacheEntry<T>.hasExpired(): Boolean {
    return Instant.now().isAfter(expires)
}
