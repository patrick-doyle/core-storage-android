package com.pdoyle.corestorage.application.storeddata

import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    private val id: String
)
