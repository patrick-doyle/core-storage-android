package com.pdoyle.corestorage.features.main

import androidx.lifecycle.lifecycleScope
import com.pdoyle.corestorage.application.storeddata.GithubRepositories
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
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
class MainScreenModule(private val activity: MainScreenActivity) {

    @Provides
    @MainScreenScope
    fun provideMainScreenData(githubRepositories: GithubRepositories): MainScreenData {
        return MainScreenData(githubRepositories)
    }

    @Provides
    @MainScreenScope
    fun provideMainScreenView(): MainScreenView = MainScreenView()

    @Provides
    @MainScreenScope
    fun provideMainScreenCoordinator(view: MainScreenView, data: MainScreenData, ): MainScreenCoordinator =
        MainScreenCoordinator(activity.lifecycleScope, view, data)
}