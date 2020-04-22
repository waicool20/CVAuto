package com.waicool20.cvauto.android.input.server

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.util.Animations
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class AndroidServerTouchInterface(
    private val device: AndroidDevice
): ITouchInterface {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
    }
    data class AndroidServerTouchInterfaceSettings(
        override var midTapDelay: Millis = 0,
        override var postTapDelay: Millis = 250
    ) : ITouchInterface.Settings

    val socket = device.server.openInputSocket()
    val inputStream = DataInputStream(socket.getInputStream())
    val outputStream = DataOutputStream(socket.getOutputStream())

    private val _touches =  List(10) { ITouchInterface.Touch(it) }

    override val settings = AndroidServerTouchInterfaceSettings()
    override val touches get() = Collections.unmodifiableList(_touches)

    override fun touchUp(slot: Int) {
        outputStream.write(AndroidServerInput.INPUT_TOUCH)
        outputStream.writeInt(ACTION_UP)
        outputStream.writeInt(slot)
        outputStream.writeInt(-1)
        outputStream.writeInt(-1)
        outputStream.writeInt(50)
        outputStream.writeFloat(1f)
        inputStream.read()
        _touches[slot].isTouching = false
    }

    override fun touchDown(slot: Int) {
        outputStream.write(AndroidServerInput.INPUT_TOUCH)
        outputStream.writeInt(ACTION_DOWN)
        outputStream.writeInt(slot)
        outputStream.writeInt(-1)
        outputStream.writeInt(-1)
        outputStream.writeInt(50)
        outputStream.writeFloat(1f)
        inputStream.read()
        _touches[slot].isTouching = true
    }

    override fun touchMove(slot: Int, x: Int, y: Int) {
        outputStream.write(AndroidServerInput.INPUT_TOUCH)
        outputStream.writeInt(ACTION_MOVE)
        outputStream.writeInt(slot)
        outputStream.writeInt(x)
        outputStream.writeInt(y)
        outputStream.writeInt(50)
        outputStream.writeFloat(1f)
        inputStream.read()
        _touches[slot].cursorX = x
        _touches[slot].cursorY = y
    }

    override fun eventSync() {
        // Do Nothing
    }

    override fun tap(slot: Int, x: Int, y: Int) {
        touchMove(slot, x, y)
        touchDown(slot)
        TimeUnit.MILLISECONDS.sleep(settings.midTapDelay)
        touchUp(slot)
        TimeUnit.MILLISECONDS.sleep(settings.postTapDelay)
    }

    override fun gesture(swipes: List<ITouchInterface.Swipe>, duration: Millis) = synchronized(this) {
        swipes.forEach { touchMove(it.slot, it.x1, it.y1) }
        swipes.forEach { touchDown(it.slot) }
        Animations.EaseInOutQuad(1000).timed(duration).forEach { p ->
            swipes.forEach { swipe ->
                val dy = (swipe.y2 - swipe.y1).toDouble()
                val dx = (swipe.x2 - swipe.x1).toDouble()
                val stepX = (dx * p + swipe.x1).roundToInt()
                val stepY = (dy * p + swipe.y1).roundToInt()
                touchMove(swipe.slot, stepX, stepY)
            }
        }
        swipes.reversed().forEach { touchUp(it.slot) }
        TimeUnit.MILLISECONDS.sleep(settings.postTapDelay)
    }

    override fun pinch(x: Int, y: Int, r1: Int, r2: Int, angle: Double, duration: Millis) = synchronized(this) {
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
}