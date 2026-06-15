package com.pdoyle.corestorage.features.main

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn

class MainScreenCoordinator(
    private val scope: CoroutineScope,
    private val view: MainScreenView,
    private val data: MainScreenData,
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        view.saveData.events
            .onEach(::handleEvent)
            .launchIn(scope)
    }

    private fun handleEvent(event: MainScreenEvent) {

    }
}