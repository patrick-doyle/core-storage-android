package com.pdoyle.corestorage

import com.pdoyle.corestorage.log.CoreStorageLogger

object CoreStorageTestLogger : CoreStorageLogger {
    override fun log(
        priority: Int, tag: String, message: String?, t: Throwable?, vararg args: Any?
    ) {
        println("[TAG - $tag], $message ${t?.printStackTrace()}")
    }
}