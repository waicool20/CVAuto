package com.waicool20.cvauto.android.server

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.Surface

object Device {
    private lateinit var displaySize: Point
    private var rotation: Int = Configuration.ORIENTATION_UNDEFINED

    const val DEFAULT_DISPLAY = 0

    init {
        updateDeviceInfo()
        KServiceManager.windowManager.watchRotation(object : IRotationWatcher.Stub() {
            override fun onRotationChanged(rotation: Int) {
                updateDeviceInfo()
            }
        }, DEFAULT_DISPLAY)
    }

    val pointers = Array(10) { slot ->
        val properties = MotionEvent.PointerProperties().apply {
            id = slot
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
        val coord = MotionEvent.PointerCoords().apply {
            orientation = 0f
            size = 1f
        }
        Pointer(properties, coord)
    }

    val width: Int get() = displaySize.x
    val height: Int get() = displaySize.y
    val rect: Rect get() = Rect(0, 0, width, height)

    fun registerSurface(surface: Surface) {
        val display = KSurfaceControl.createDisplay("server", false)
        KSurfaceControl.openTransaction();
        try {
            KSurfaceControl.setDisplaySurface(display, surface);
            KSurfaceControl.setDisplayProjection(display, 0, rect, rect);
            KSurfaceControl.setDisplayLayerStack(display, 0);
        } finally {
            KSurfaceControl.closeTransaction();
        }
    }

    private fun updateDeviceInfo() {
        displaySize = Point().also { KServiceManager.windowManager.getBaseDisplaySize(DEFAULT_DISPLAY, it) }
        rotation = KServiceManager.windowManager.defaultDisplayRotation
    }
}