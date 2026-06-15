package com.pdoyle.corestorage.features.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pdoyle.corestorage.features.common.appComponent

class MainScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val component = appComponent().mainScreenComponent()
            .create(MainScreenModule(this))

        lifecycle.addObserver(component.coordinator())

        setContent {
            component.view().Render()
        }
    }
}