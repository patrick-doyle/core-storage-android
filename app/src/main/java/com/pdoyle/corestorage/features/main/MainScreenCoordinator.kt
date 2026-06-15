package com.pdoyle.corestorage.features.main

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pdoyle.corestorage.features.common.launchCoroutine
import kotlinx.coroutines.CoroutineScope

class MainScreenCoordinator(
    private val scope: CoroutineScope,
    private val view: MainScreenView,
    private val data: MainScreenData,
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        saveData()
    }

    private fun saveData() {
        view.listenForSaveData()
            .launchCoroutine(scope) {
                data.saveData()
            }
    }
}