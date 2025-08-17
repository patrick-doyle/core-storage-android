package com.pdoyle.corestorage.log

import android.util.Log

object CoreStorageAndroidLogger: CoreStorageLogger {

    override fun log(priority: Int, tag: String, message: String?, t: Throwable?, vararg args: Any?) {
        when(priority) {
            Log.INFO -> Log.i(tag, message, t)
            Log.ERROR -> Log.e(tag, message, t)
            Log.VERBOSE -> Log.v(tag, message, t)
            Log.WARN -> Log.w(tag, message, t)
            Log.DEBUG -> Log.d(tag, message, t)
            else -> Log.v(tag, message, t)
        }
    }
}