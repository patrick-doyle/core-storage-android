package com.pdoyle.corestorage.application.di

import com.pdoyle.corestorage.features.main.MainScreenComponent
import dagger.Component
import javax.inject.Scope

@Scope
annotation class AppScope

@AppScope
@Component(modules = [AppModule::class])
interface AppComponent {

    fun mainScreenComponent(): MainScreenComponent.Factory
}