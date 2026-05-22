package com.pdoyle.corestorage.log

import android.util.Log

internal class CoreStorageLog(private val logger: CoreStorageLogger) {

    fun v(tag: String, message: String? = null, vararg args: Any? = emptyArray<Any?>()) {
        logger.log(Log.VERBOSE, tag, message, null, args)
    }

    fun i(tag: String, message: String? = null, vararg args: Any? = emptyArray<Any?>()) {
        logger.log(Log.INFO, tag, message, null, args)
    }

    fun d(tag: String, message: String? = null, vararg args: Any? = emptyArray<Any?>()) {
        logger.log(Log.DEBUG, tag, message, null, args)
    }

    fun w(
        t: Throwable? = null,
        tag: String,
        message: String? = null,
        vararg args: Any? = emptyArray<Any?>()
    ) {
        logger.log(Log.WARN, tag, message, t, args)
    }

    fun e(
        t: Throwable? = null,
        tag: String,
        message: String? = null,
        vararg args: Any? = emptyArray<Any?>()
    ) {
        logger.log(Log.ERROR, tag, message, t, args)
    }

}