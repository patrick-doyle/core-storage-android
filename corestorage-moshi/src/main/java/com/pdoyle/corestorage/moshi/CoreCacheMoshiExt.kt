package com.pdoyle.corestorage.moshi

import java.time.Instant
import kotlin.time.Duration

/**
 * Gets the data from the cache
 */
inline fun <reified T> CoreCacheMoshi.getOrDefault(key: String, defaultValue: T): T {
    return get(key, T::class.java) ?: defaultValue
}

/**
 * Save a value to the cache. This entry will never expire
 *
 * @return true if entry was saved, false otherwise.
 */
fun <T> CoreCacheMoshi.putNoExpire(
    key: String,
    data: T,
    clazz: Class<T>,
): Boolean {
    // DON'T USE Instant.MAX, it causes a crash when converting to LONG for writing an entry

    // java.lang.ArithmeticException: long overflow
    //  at java.base/java.lang.Math.multiplyExact(Math.java:1004)
    //  at java.base/java.lang.Math.multiplyExact(Math.java:980)
    //  at java.base/java.time.Instant.toEpochMilli(Instant.java:1236)
    return put(key, data, expires = Instant.ofEpochMilli(Long.MAX_VALUE), clazz)
}

/**
 * Save a value to the cache. This entry will expire once the duration has elapsed
 *
 * @return true if entry was saved, false otherwise.
 */
fun <T> CoreCacheMoshi.putExpiresIn(
    key: String,
    data: T,
    duration: Duration,
    clazz: Class<T>,
): Boolean {
    val expiryTime = Instant.now().plusMillis(duration.inWholeMilliseconds)
    return put(key, data, expiryTime, clazz)
}
