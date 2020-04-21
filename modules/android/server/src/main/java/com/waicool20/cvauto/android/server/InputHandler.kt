package com.waicool20.cvauto.android.server

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket


class InputHandler(private val socket: Socket) : Closeable {
    companion object {
        const val MODE_ASYNC = 0
        const val MODE_WAIT = 1

        const val RESPONSE_OK = 0
        const val RESPONSE_ERR = 1

        const val INPUT_TOUCH = 0
        const val INPUT_KEY = 1
    }

    private val inputStream = DataInputStream(socket.getInputStream())
    private val outputStream = socket.getOutputStream()
    private val inputManager = KServiceManager.inputManager
    private val deviceMap = mutableMapOf<String, DataOutputStream>()

    override fun close() {
        socket.close()
        deviceMap.values.forEach { it.close() }
        Logger.i("Closed input handler")
    }

    fun waitFor() {
        loop@ while (!socket.isClosed) {
            when (val i = inputStream.read()) {
                INPUT_TOUCH -> handleTouchAndroid()
                INPUT_KEY -> handleKeyAndroid()
                -1 -> break@loop
                else -> Logger.i("Unknown input command: $i")
            }
        }
        close()
    }

    private fun handleTouchAndroid() {
        var action = inputStream.readInt()
        val slot = inputStream.readInt()
        val x = inputStream.readInt()
        val y = inputStream.readInt()
        val touchMajor = inputStream.readInt()
        val pressure = inputStream.readFloat()

        val now = SystemClock.uptimeMillis()

        val pointer = Device.pointers[slot].apply {
            if (x != -1) coords.x = x.toFloat()
            if (y != -1) coords.y = y.toFloat()
            coords.touchMajor = touchMajor.toFloat()
            coords.pressure = pressure

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchTime = now
                    isTouching = true
                    if (slot > 0) {
                        action = MotionEvent.ACTION_POINTER_DOWN or (slot shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isTouching = false
                    if (slot > 0) {
                        action = MotionEvent.ACTION_POINTER_UP or (slot shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    }
                }
            }
        }

        val touching = Device.pointers.filter { it.isTouching }
        val touches = touching.size
        val props = Device.pointers.map { it.properties }.toTypedArray()
        val coords = Device.pointers.map { it.coords }.toTypedArray()

        val motionEvent = MotionEvent.obtain(
            pointer.lastTouchTime, now, action, touches.coerceAtLeast(1),
            props, coords,
            0, 0, 1f, 1f, -1, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        inputManager.injectInputEvent(motionEvent, MODE_ASYNC)
        outputStream.write(RESPONSE_OK)
    }

    private fun handleKeyAndroid() {
        val action = inputStream.readInt()
        val keycode = inputStream.readInt()
        val repeat = inputStream.readInt()
        val metaState = inputStream.readInt()

        val now = SystemClock.uptimeMillis()
        val keyEvent = KeyEvent(
            now, now, action, keycode, repeat, metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD
        )
        inputManager.injectInputEvent(keyEvent, MODE_ASYNC)
        outputStream.write(RESPONSE_OK)
    }

    //</editor-fold>
}