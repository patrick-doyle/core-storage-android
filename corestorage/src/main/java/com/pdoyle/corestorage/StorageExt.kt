package com.pdoyle.corestorage

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.BufferedSink
import okio.BufferedSource

/**
 * Returns a Deserializer function for [CoreCache] and [CoreStorage]. This is implemented using
 * kotlinx.serialization's Json.decodeFromStream
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> storageJsonDeserializer(): ((source: BufferedSource) -> T) {
    return {
        Json.decodeFromStream<T>(it.inputStream())
    }
}

/**
 * Returns a Serializer function for [CoreCache] and [CoreStorage]. This is implemented using
 * kotlinx.serialization's Json.encodeToStream
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> storageJsonSerializer(): ((data: T, sink: BufferedSink) -> Unit) {
    return { data, sink ->
        Json.encodeToStream<T>(data, sink.outputStream())
    }
}
