package com.pdoyle.corestorage.application.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class AppModule(private val context: Context) {

    @Reusable
    @Provides
    fun provideContext(): Context = context.applicationContext
}