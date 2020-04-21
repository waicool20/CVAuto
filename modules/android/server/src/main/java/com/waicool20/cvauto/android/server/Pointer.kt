package com.waicool20.cvauto.android.server

import android.view.MotionEvent

data class Pointer(
    val properties: MotionEvent.PointerProperties,
    val coords: MotionEvent.PointerCoords,
    var isTouching: Boolean = false,
    var lastTouchTime: Long = 0
)