package com.pdoyle.corestorage.application.di

import dagger.Component
import javax.inject.Singleton

@Scope
annotation class AppScope

@AppScope
@Component(modules = [AppModule::class])
interface AppComponent {


}