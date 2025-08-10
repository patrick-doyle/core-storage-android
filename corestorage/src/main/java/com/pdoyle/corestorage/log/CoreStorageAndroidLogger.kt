package com.pdoyle.corestorage.log

import android.util.Log

internal class CoreStorageAndroidLogger: CoreStorageLogger {

    /**
     *  ASSERT = 7;
     *     public static final int DEBUG = 3;
     *     public static final int ERROR = 6;
     *     public static final int INFO = 4;
     *     public static final int VERBOSE = 2;
     *     public static final int WARN = 5;
     */
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