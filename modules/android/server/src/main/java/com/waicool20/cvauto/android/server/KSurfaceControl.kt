package com.waicool20.cvauto.android.server

import android.graphics.Rect
import android.os.IBinder
import android.view.Surface


object KSurfaceControl {
    private val cls = Class.forName("android.view.SurfaceControl")

    private val createDisplayMethod by lazy {
        cls.getMethod("createDisplay", String::class.java, Boolean::class.javaPrimitiveType)
    }

    private val openTransactionMethod by lazy { cls.getMethod("openTransaction") }
    private val closeTransactionMethod by lazy { cls.getMethod("closeTransaction") }

    private val setDisplaySurfaceMethod by lazy {
        cls.getMethod("setDisplaySurface", IBinder::class.java, Surface::class.java)
    }

    private val setDisplayProjectionMethod by lazy {
        cls.getMethod(
            "setDisplayProjection",
            IBinder::class.java,
            Int::class.javaPrimitiveType,
            Rect::class.java,
            Rect::class.java
        )
    }

    private val setDisplayLayerStackMethod by lazy {
        cls.getMethod("setDisplayLayerStack", IBinder::class.java, Int::class.javaPrimitiveType)
    }

    fun createDisplay(name: String, secure: Boolean): IBinder {
        return createDisplayMethod.invoke(null, name, secure) as IBinder
    }

    fun openTransaction() {
        openTransactionMethod.invoke(null)
    }

    fun closeTransaction() {
        closeTransactionMethod.invoke(null)
    }

    fun setDisplaySurface(display: IBinder, surface: Surface) {
        setDisplaySurfaceMethod.invoke(null, display, surface)
    }

    fun setDisplayProjection(display: IBinder, orientation: Int, layerStackRect: Rect, displayRect: Rect) {
        setDisplayProjectionMethod.invoke(null, display, orientation, layerStackRect, displayRect)
    }

    fun setDisplayLayerStack(display: IBinder, layerStack: Int) {
        setDisplayLayerStackMethod.invoke(null, display, layerStack)
    }
}