package com.pdoyle.corestorage.application

import android.app.Application
import com.pdoyle.corestorage.application.di.AppComponent
import com.pdoyle.corestorage.application.di.AppModule
import com.pdoyle.corestorage.application.di.DaggerAppComponent

class CoreStorageDemoApplication : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

}