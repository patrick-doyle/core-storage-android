package com.pdoyle.corestorage.application.storeddata

import kotlinx.serialization.Serializable


@Serializable
data class GithubUser(
    private val id: String,
    private val username: String,
)

@Serializable
data class GithubRepo(
    private val id: String,
    private val userID : String,
)