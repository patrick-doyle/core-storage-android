package com.pdoyle.corestorage.features.common

import android.view.View

fun View.clickRelay() : EventRelay<View> {
    val relay = EventRelay.create<View>()
    setOnClickListener { relay.send(it) }
    return relay
}