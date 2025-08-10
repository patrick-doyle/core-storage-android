package com.pdoyle.corestorage.log

interface CoreStorageLogger {

    fun log(priority: Int, tag: String, message: String?, t: Throwable?, vararg args: Any?)
}