package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.util.Animations
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*
import kotlin.random.Random

class AndroidTouchInterface private constructor(
    private val device: AndroidDevice
) : ITouchInterface {
    data class AndroidTouchInterfaceSettings(
        override var midTapDelay: Millis = 0,
        override var postTapDelay: Millis = 250
    ) : ITouchInterface.Settings

    companion object {
        var LOG_INPUT_EVENTS = false

        internal fun getForDevice(device: AndroidDevice): AndroidTouchInterface {
            return AndroidTouchInterface(device)
        }
    }

    private val _touches = MutableList(10) { ITouchInterface.Touch(it) }
    private val writeBuffer = ByteArray(28)

    override val settings = AndroidTouchInterfaceSettings()

    override val touches get() = Collections.unmodifiableList(_touches)

    override fun touchUp(slot: Int) = synchronized(this) {
        if (_touches[slot].isTouching) {
            sendEvent(InputEvent.MOTION_ACTION_UP, transformTouch(_touches[slot]), 0)
            _touches[slot].isTouching = false
        }
    }

    override fun touchDown(slot: Int) = synchronized(this) {
        if (!_touches[slot].isTouching) {
            sendEvent(InputEvent.MOTION_ACTION_DOWN, transformTouch(_touches[slot]))
            _touches[slot].isTouching = true
        }
    }

    override fun touchMove(slot: Int, x: Int, y: Int) = synchronized(this) {
        val xChanged = _touches[slot].cursorX != x
        val yChanged = _touches[slot].cursorY != y
        if (xChanged) _touches[slot].cursorX = x
        if (yChanged) _touches[slot].cursorY = y
        if (_touches[slot].isTouching) {
            if (xChanged || yChanged) {
                sendEvent(InputEvent.MOTION_ACTION_MOVE, transformTouch(_touches[slot]))
            }
        }
    }

    override fun eventSync() = Unit

    override fun tap(slot: Int, x: Int, y: Int) = synchronized(this) {
        touchMove(slot, x, y)
        touchDown(slot)
        TimeUnit.MILLISECONDS.sleep(settings.midTapDelay)
        touchUp(slot)
        TimeUnit.MILLISECONDS.sleep(settings.postTapDelay)
    }

    override fun gesture(swipes: List<ITouchInterface.Swipe>, duration: Millis) =
        synchronized(this) {
            swipes.forEach { touchMove(it.slot, it.x1, it.y1) }
            swipes.forEach { touchDown(it.slot) }
            val dist = swipes.maxOf {
                (it.x2 - it.x1) * (it.x2 - it.x1) + (it.y2 - it.y1) * (it.y2 - it.y1)
            }.toDouble()
            Animations.EaseInOutQuad(sqrt(dist).roundToLong()).timed(duration).forEach { p ->
                swipes.forEach { swipe ->
                    val dy = (swipe.y2 - swipe.y1).toDouble()
                    val dx = (swipe.x2 - swipe.x1).toDouble()
                    val stepX = (dx * p + swipe.x1).roundToInt()
                    val stepY = (dy * p + swipe.y1).roundToInt()
                    touchMove(swipe.slot, stepX, stepY)
                }
            }
            swipes.forEach { touchUp(it.slot) }
            TimeUnit.MILLISECONDS.sleep(settings.postTapDelay)
        }

    override fun pinch(x: Int, y: Int, r1: Int, r2: Int, angle: Double, duration: Millis) =
        synchronized(this) {
            val rad = (angle * PI) / 180
            val dr1x = (r1 * cos(rad)).roundToInt()
            val dr1y = (r1 * sin(rad)).roundToInt()
            val dr2x = (r2 * cos(rad)).roundToInt()
            val dr2y = (r2 * sin(rad)).roundToInt()

            gesture(
                listOf(
                    ITouchInterface.Swipe(0, x + dr1x, y + dr1y, x + dr2x, y + dr2y),
                    ITouchInterface.Swipe(1, x - dr1x, y - dr1y, x - dr2x, y - dr2y)
                ),
                duration
            )
        }

    private fun transformTouch(touch: ITouchInterface.Touch): ITouchInterface.Touch {
        var xCoord = touch.cursorX
        var yCoord = touch.cursorY
        if (device.properties.displayWidth > device.properties.displayHeight) {
            when (device.orientation) {
                0, 2 -> {
                    xCoord = touch.cursorX
                    yCoord = touch.cursorY
                }
                1, 3 -> {
                    xCoord = device.properties.displayWidth - touch.cursorX
                    yCoord = device.properties.displayHeight - touch.cursorY
                }
            }
        } else {
            when (device.orientation) {
                0, 2 -> {
                    xCoord = touch.cursorY
                    yCoord = device.properties.displayHeight - touch.cursorX
                }
                1, 3 -> {
                    xCoord = device.properties.displayWidth - touch.cursorY
                    yCoord = touch.cursorX
                }
            }
        }
        return touch.copy(cursorX = xCoord, cursorY = yCoord)
    }

    private fun sendEvent(
        event: InputEvent,
        touch: ITouchInterface.Touch,
        pressure: Int = 50 + Random.nextInt(-25, 25),
        button: InputEvent = InputEvent.MOTION_BUTTON_PRIMARY
    ) {
        val x = touch.cursorX
        val y = touch.cursorY
        val w = device.properties.displayWidth
        val h = device.properties.displayHeight

        if (LOG_INPUT_EVENTS) println("$event $touch $pressure $button")
        ByteBuffer.wrap(writeBuffer)
            .order(ByteOrder.BIG_ENDIAN)
            .put(ControlMessage.TYPE_INJECT_TOUCH_EVENT.toByte())
            .put(event.code.toByte())
            .putLong(touch.slot.toLong())
            .putInt(x)
            .putInt(y)
            .putShort(w.toShort())
            .putShort(h.toShort())
            .putShort(pressure.toShort())
            .putInt(button.code.toInt())
        try {
            device.scrcpy.control.getOutputStream().apply {
                write(writeBuffer)
                flush()
            }
        } catch (e: Exception) {
            device.resetScrcpy()
            sendEvent(event, touch, pressure, button)
        }
    }
}
