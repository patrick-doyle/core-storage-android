package com.pdoyle.corestorage.application.storeddata

import com.pdoyle.corestorage.application.di.AppScope
import javax.inject.Inject

@AppScope
class GithubRepositories @Inject constructor(){

    suspend fun getRepos(): List<GithubRepo> {
        TODO("Implement")
    }
}