package com.pdoyle.corestorage.features.common

class EventRelay<T> private constructor() {

    private val callbacks = mutableSetOf<(T) -> Unit>()

    fun onEvent(callback: (T) -> Unit) {
        this.callbacks.add(callback)
    }

    fun send(event: T) {
        callbacks.forEach { it(event) }
    }

    companion object {

        fun <T> create(): EventRelay<T> {
            return EventRelay()
        }
    }
}