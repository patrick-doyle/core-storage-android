package com.pdoyle.corestorage

import java.time.Instant
import kotlin.time.Duration
import okio.BufferedSink
import okio.BufferedSource

/**
 * Gets the data from the cache using the default @see [storageJsonDeserializer]
 */
inline fun <reified T> CoreCache.get(key: String): T? {
    return get(key, storageJsonDeserializer())
}

/**
 * Gets the data from the cache using the default @see [storageJsonDeserializer]
 */
inline fun <reified T> CoreCache.getOrDefault(key: String, defaultValue: T): T {
    return getOrDefault(key, defaultValue, storageJsonDeserializer())
}

/**
 * Put the data into the cache using the default @see [storageJsonSerializer]
 */
inline fun <reified T> CoreCache.put(key: String, data: T, expires: Instant) {
    put(key, data, expires, storageJsonSerializer())
}

/**
 * Put the data into the cache using the default @see [storageJsonSerializer]
 */
inline fun <reified T> CoreCache.putNoExpire(key: String, data: T) {
    putNoExpire(key, data, storageJsonSerializer())
}

/**
 * Put the data into the cache using the default @see [storageJsonSerializer]
 */
inline fun <reified T> CoreCache.putExpiresIn(key: String, data: T, duration: Duration) {
    putExpiresIn(key, data, duration, storageJsonSerializer())
}

/**
 * Gets the data from the cache or returns default if no entry has been found
 */
fun <T> CoreCache.getOrDefault(
    key: String,
    defaultValue: T,
    deserializer: (source: BufferedSource) -> T,
): T {
    return get(key, deserializer) ?: defaultValue
}

/**
 * Save a value to the cache. This entry will never expire
 *
 * @return true if entry was saved, false otherwise.
 */
fun <T> CoreCache.putNoExpire(
    key: String,
    data: T,
    serialize: (data: T, sink: BufferedSink) -> Unit,
): Boolean {
    // DON'T USE Instant.MAX, it causes a crash when converting to LONG for writing an entry

    // java.lang.ArithmeticException: long overflow
    //  at java.base/java.lang.Math.multiplyExact(Math.java:1004)
    //  at java.base/java.lang.Math.multiplyExact(Math.java:980)
    //  at java.base/java.time.Instant.toEpochMilli(Instant.java:1236)
    return put(key, data, expires = Instant.ofEpochMilli(Long.MAX_VALUE), serialize)
}

/**
 * Save a value to the cache. This entry will expire once the duration has elapsed
 *
 * @return true if entry was saved, false otherwise.
 */
fun <T> CoreCache.putExpiresIn(
    key: String,
    data: T,
    duration: Duration,
    serialize: (data: T, sink: BufferedSink) -> Unit,
): Boolean {
    val expiryTime = Instant.now().plusMillis(duration.inWholeMilliseconds)
    return put(key, data, expiryTime, serialize)
}
