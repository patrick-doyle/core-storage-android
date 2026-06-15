package com.pdoyle.corestorage.features.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun <T, R> EventRelay<T>.map(transform: (T) -> R) : EventRelay<R> {
    val relay = EventRelay.create<R>()
    this.onEvent { relay.send(transform(it)) }
    return relay
}

fun <T> EventRelay<T>.debounce(duration: Duration = 250.milliseconds) : EventRelay<T> {
    val relay = EventRelay.create<T>()
    this.onEvent(object : (T) -> Unit {

        private val lastTimeInvoked : Long = 0

        override fun invoke(event: T) {
            if ((System.currentTimeMillis() - lastTimeInvoked) > duration.inWholeMilliseconds) {
                relay.send(event)
            }
        }
    })
    return relay
}

fun <T> EventRelay<T>.launchCoroutine(scope: CoroutineScope, suspendOnEvent: suspend (T) -> Unit) : EventRelay<T> {
    val relay = EventRelay.create<T>()
    this.onEvent(object : (T) -> Unit {

        private var job: Job? = null

        override fun invoke(event: T) {
            job?.cancel()
            job = scope.launch {
                suspendOnEvent(event)
            }
        }
    })
    return relay
}

fun <T> EventRelay<T>.unit() : EventRelay<Unit> {
    return map {  }
}