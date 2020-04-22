package com.waicool20.cvauto.android.input.server

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.android.input.KeyUtils
import com.waicool20.cvauto.core.input.CharactersPerSecond
import com.waicool20.cvauto.core.input.IKeyboard
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

class AndroidServerKeyboard(
    private val device: AndroidDevice
) : IKeyboard {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1

        const val META_CTRL_MASK = 0x00007000
        const val META_SHIFT_MASK = 0x000000c1
        const val META_META_MASK = 0x00070000
        const val META_ALT_MASK = 0x00000032
    }

    data class AndroidServerKeyboardSettings(
        override var defaultTypingSpeed: CharactersPerSecond = 7,
        override var typingSpeedVariance: Double = 0.25
    ) : IKeyboard.Settings

    val socket = device.server.openInputSocket()
    val inputStream = DataInputStream(socket.getInputStream())
    val outputStream = DataOutputStream(socket.getOutputStream())

    private val _heldKeys = mutableListOf<String>()

    override val settings = AndroidServerKeyboardSettings()
    override val heldKeys get() = Collections.unmodifiableList(_heldKeys)

    override fun keyUp(keyName: String) {
        outputStream.write(AndroidServerInput.INPUT_KEY)
        outputStream.writeInt(ACTION_UP)
        outputStream.writeInt(getKey(keyName).code.toInt())
        outputStream.writeInt(0)
        outputStream.writeInt(getMask())
        inputStream.read()
        _heldKeys.remove(keyName)
    }

    override fun keyDown(keyName: String) {
        outputStream.write(AndroidServerInput.INPUT_KEY)
        outputStream.writeInt(ACTION_DOWN)
        outputStream.writeInt(getKey(keyName).code.toInt())
        outputStream.writeInt(0)
        outputStream.writeInt(getMask())
        inputStream.read()
        _heldKeys.add(keyName)
    }

    override fun checkSupport(keyName: String): Boolean {
        return getKey(keyName) != ServerKey.KEY_UNKNOWN
    }

    override fun type(string: String, speed: CharactersPerSecond) {
        string.forEach { c ->
            if (KeyUtils.requiresShift(c)) keyDown("SHIFT")
            keyDown("$c")
            keyUp("$c")
            if (KeyUtils.requiresShift(c)) keyUp("SHIFT")
            TimeUnit.MILLISECONDS.sleep(
                (1000.0 / speed * (1 + Random.nextDouble(
                    -settings.typingSpeedVariance,
                    settings.typingSpeedVariance
                ))).roundToLong()
            )
        }
    }

    private fun getKey(keyName: String): ServerKey {
        return when (keyName) {
            "CTRL" -> ServerKey.KEY_CTRL_LEFT
            "ALT" -> ServerKey.KEY_ALT_LEFT
            "SHIFT" -> ServerKey.KEY_SHIFT_LEFT
            "WIN", "META" -> ServerKey.KEY_META_LEFT
            else -> ServerKey.findByName(keyName)
        }
    }

    private fun getMask(): Int {
        var mask = 0
        if (heldKeys.contains("CTRL")) mask = mask or META_CTRL_MASK
        if (heldKeys.contains("ALT")) mask = mask or META_ALT_MASK
        if (heldKeys.contains("SHIFT")) mask = mask or META_SHIFT_MASK
        if (heldKeys.contains("META")) mask = mask or META_META_MASK
        return mask
    }
}