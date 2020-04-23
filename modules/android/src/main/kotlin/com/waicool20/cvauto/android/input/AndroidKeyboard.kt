package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.android.readText
import com.waicool20.cvauto.core.input.CharactersPerSecond
import com.waicool20.cvauto.core.input.IKeyboard
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

class AndroidKeyboard private constructor(
    private val device: AndroidDevice,
    private val keyDevFileMap: Map<Key, DeviceFile>
) : IKeyboard {
    data class AndroidKeyboardSettings(
        override var defaultTypingSpeed: CharactersPerSecond = 7,
        override var typingSpeedVariance: Double = 0.25
    ) : IKeyboard.Settings

    companion object {
        private val KEY_CODE_REGEX = Regex("[\\w\\d]{4}\\*?")
        internal fun getForDevice(device: AndroidDevice): AndroidKeyboard {
            val inputInfo = device.execute("getevent -p")
                .readText()
                .split("add device")
                .filter { it.contains("KEY") }
                .takeIf { it.isNotEmpty() }
            if (inputInfo == null) {
                println("No keyboard found for device ${device.serial}")
                return AndroidKeyboard(device, emptyMap())
            }

            val keyDevFileMap = mutableMapOf<Key, DeviceFile>()
            inputInfo.map { it.split(Regex("\\s+")) }.forEach { devEntryTokens ->
                val devFile = DeviceFile(devEntryTokens[2])
                devEntryTokens
                    .dropWhile { it != "KEY" }.drop(2)
                    .takeWhile { it.matches(KEY_CODE_REGEX) }
                    .mapNotNull { Key.findByCode(it.take(4).toLong(16)) }
                    .forEach { keyDevFileMap[it] = devFile }
            }
            return AndroidKeyboard(device, keyDevFileMap)
        }
    }

    private val _heldKeys = mutableListOf<String>()
    private val writeBuffer = ByteArray(16)

    override val settings: AndroidKeyboardSettings = AndroidKeyboardSettings()

    override val heldKeys: List<String> get() = Collections.unmodifiableList(_heldKeys)

    override fun keyUp(keyName: String): Unit = synchronized(this) {
        if (!heldKeys.contains(keyName)) return@synchronized
        val key = getKey(keyName)
        val devFile = keyDevFileMap[key]?.path ?: return
        sendKeyEvent(key, devFile, InputEvent.KEY_UP)
        sendEvent(devFile, EventType.EV_SYN, InputEvent.SYN_REPORT, 0)
        _heldKeys.remove(keyName)
    }

    override fun keyDown(keyName: String): Unit = synchronized(this) {
        if (heldKeys.contains(keyName)) return@synchronized
        val key = getKey(keyName)
        val devFile = keyDevFileMap[key]?.path ?: return
        sendKeyEvent(key, devFile, InputEvent.KEY_DOWN)
        sendEvent(devFile, EventType.EV_SYN, InputEvent.SYN_REPORT, 0)
        _heldKeys.add(keyName)
    }

    override fun checkSupport(keyName: String): Boolean {
        return getKey(keyName) != Key.KEY_UNKNOWN
    }

    override fun type(string: String, speed: CharactersPerSecond) {
        string.forEach { c ->
            if (Key.requiresShift(c)) keyDown("SHIFT")
            keyDown("$c")
            keyUp("$c")
            if (Key.requiresShift(c)) keyUp("SHIFT")
            TimeUnit.MILLISECONDS.sleep(
                (1000.0 / speed * (1 + Random.nextDouble(
                    -settings.typingSpeedVariance,
                    settings.typingSpeedVariance
                ))).roundToLong()
            )
        }
    }

    private fun getKey(keyName: String): Key {
        return when (keyName) {
            "CTRL" -> Key.KEY_LEFTCTRL
            "ALT" -> Key.KEY_LEFTALT
            "SHIFT" -> Key.KEY_LEFTSHIFT
            "WIN", "META" -> Key.KEY_LEFTMETA
            else -> Key.findByName(keyName)
        }
    }

    private fun sendEvent(devFile: String, type: EventType, code: InputEvent, value: Long) {
        ByteBuffer.wrap(writeBuffer)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0).putInt(0) // Event time can be left as 0
                .putShort(type.code.toShort())
                .putShort(code.code.toShort())
                .putInt(value.toInt())
        try {
            device.input.getDeviceFileOutputStream(DeviceFile(devFile)).apply {
                write(writeBuffer)
                write("".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendKeyEvent(key: Key, devFile: String, event: InputEvent) {
        ByteBuffer.wrap(writeBuffer)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0).putInt(0) // Event time can be left as 0
                .putShort(EventType.EV_KEY.code.toShort())
                .putShort(key.code.toShort())
                .putInt(event.code.toInt())
        try {
            device.input.getDeviceFileOutputStream(DeviceFile(devFile)).apply {
                write(writeBuffer)
                write("".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}