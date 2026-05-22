package com.pdoyle.corestorage.gson

import com.google.gson.Gson

internal fun testGson() = Gson()

internal data class TestStorageData(
    val stringKey: String = "string-value",
    val numberKey: Int = 4322,
)