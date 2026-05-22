package com.pdoyle.corestorage

import kotlinx.serialization.Serializable

@Serializable
internal data class TestStorageData(
    private val stringKey: String = "string-value",
    private val numberKey: Int = 4322,
)