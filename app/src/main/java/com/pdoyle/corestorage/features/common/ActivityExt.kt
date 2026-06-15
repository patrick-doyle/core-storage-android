package com.pdoyle.corestorage.features.common

import android.app.Activity
import com.pdoyle.corestorage.application.CoreStorageDemoApplication
import com.pdoyle.corestorage.application.di.AppComponent

fun Activity.appComponent(): AppComponent {
    return (application as CoreStorageDemoApplication).appComponent
}