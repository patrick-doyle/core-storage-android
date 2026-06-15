package com.pdoyle.corestorage.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pdoyle.corestorage.features.common.EventRelay

class MainScreenView {

    val saveDataRelay = EventRelay.create<Unit>()

    @Composable
    fun Render() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = greeting)
            }
        }
    }
}