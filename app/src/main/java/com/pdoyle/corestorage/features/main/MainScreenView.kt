package com.pdoyle.corestorage.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import com.pdoyle.corestorage.application.storeddata.GithubRepo
import com.pdoyle.corestorage.features.common.EventRelay
class MainScreenView {

    private val saveDataRelay = EventRelay.create<Unit>()
    private val repos = MutableLiveData<List<GithubRepo>>()

    @Composable
    fun Render() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Hello world")
            }
        }
    }

    fun listenForSaveData() = saveDataRelay

    fun showSavedData(githubRepos: List<GithubRepo>) {
        this.repos.value = githubRepos
    }
}