package com.pdoyle.corestorage.features.main

import androidx.lifecycle.lifecycleScope
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Scope

@Scope
annotation class MainScreenScope

@MainScreenScope
@Subcomponent(modules = [MainScreenModule::class])
interface MainScreenComponent {

    fun view(): MainScreenView

    fun coordinator(): MainScreenCoordinator

    @Subcomponent.Factory
    interface Factory {
        fun create(module: MainScreenModule): MainScreenComponent
    }
}


@Module
class MainScreenModule(private val activity: MainActivity) {

    @Provides
    @MainScreenScope
    fun provideMainScreenData(): MainScreenData = MainScreenData()

    @Provides
    @MainScreenScope
    fun provideMainScreenView(): MainScreenView = MainScreenView()

    @Provides
    @MainScreenScope
    fun provideMainScreenCoordinator(view: MainScreenView, data: MainScreenData, ): MainScreenCoordinator =
        MainScreenCoordinator(activity.lifecycleScope, view, data)
}