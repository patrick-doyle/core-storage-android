package com.pdoyle.corestorage.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal fun testMoshi() = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

internal data class TestStorageData(
    private val stringKey: String = "string-value",
    private val numberKey: Int = 4322,
)