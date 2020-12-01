package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.android.lineSequence
import com.waicool20.cvauto.android.readText
import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.util.Animations
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.*
import kotlin.random.Random

class AndroidTouchInterface private constructor(
    private val device: AndroidDevice,
    private val devFile: DeviceFile,
    private val touchSpecs: Map<InputEvent, Specs>
) : ITouchInterface {
    /**
     * Specification of the acceptable touch values, see "ABS_MT_XXXX" in [InputEvent]
     *
     * @param minValue Minimum value
     * @param maxValue Maximum value
     * @param fuzz
     * @param flat
     * @param resolution
     */
    private data class Specs(
        val minValue: Int,
        val maxValue: Int,
        val fuzz: Int,
        val flat: Int,
        val resolution: Int
    ) {
        companion object {
            private val REGEX =
                Regex(".*?(\\w{4})\\s+:\\s+value \\d+, min (\\d+), max (\\d+), fuzz (\\d+), flat (\\d+), resolution (\\d+).*?")

            fun parse(spec: String): Pair<InputEvent, Specs>? {
                val (event, min, max, fuzz, flat, res) =
                    REGEX.matchEntire(spec)?.destructured ?: return null
                val iet = InputEvent[event.toLong(16)] ?: return null
                return iet to Specs(min.toInt(), max.toInt(), fuzz.toInt(), flat.toInt(), res.toInt())
            }
        }
    }

    data class AndroidTouchInterfaceSettings(
        override var midTapDelay: Millis = 0,
        override var postTapDelay: Millis = 250
    ) : ITouchInterface.Settings

    companion object {
        var LOG_INPUT_EVENTS = false

        internal fun getForDevice(device: AndroidDevice): AndroidTouchInterface? {
            val inputInfo = device.execute("getevent -p")
                .readText()
                .split("add device")
                .find { it.contains("ABS") }
                ?.lines()
            if (inputInfo == null) {
                println("No touch interface found for device ${device.serial}")
                return null
            }
            val devFile = DeviceFile(inputInfo[0].takeLastWhile { !it.isWhitespace() })
            val touchSpecs = inputInfo.drop(2)
                .mapNotNull(Specs.Companion::parse).toMap()
            return AndroidTouchInterface(device, devFile, touchSpecs)
        }
    }

    private val _touches: List<ITouchInterface.Touch>
    private val writeBuffer = ByteArray(16)

    override val settings = AndroidTouchInterfaceSettings()

    override val touches get() = Collections.unmodifiableList(_touches)

    init {
        // Determine max amount of touch slots the device can handle
        val maxSlots = touchSpecs[InputEvent.ABS_MT_SLOT]?.maxValue ?: 1
        _touches = MutableList(maxSlots) { ITouchInterface.Touch(it) }

        // Keep track of the x y coordinates by monitoring the input event bus
        thread(isDaemon = true) {
            val process = device.execute("getevent", devFile.path)
            Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })
            var slot = 0L
            var x = 0
            var y = 0
            var isTouching = false
            var isTouchEvent = false

            process.lineSequence().forEach {
                val (_, sCode, sValue) = it.trim().split(Regex("\\s+"))
                val eventCode = sCode.toLong(16)
                val value = sValue.toLong(16)
                when (eventCode) {
                    InputEvent.ABS_MT_SLOT.code -> {
                        slot = value
                    }
                    InputEvent.ABS_MT_POSITION_X.code -> {
                        x = valueToCoord(value, InputEvent.ABS_MT_POSITION_X)
                    }
                    InputEvent.ABS_MT_POSITION_Y.code -> {
                        y = valueToCoord(value, InputEvent.ABS_MT_POSITION_Y)
                    }
                    InputEvent.ABS_MT_TRACKING_ID.code -> {
                        isTouchEvent = true
                        isTouching = sValue != "ffffffff"
                    }
                    EventType.EV_SYN.code -> {
                        if (isTouchEvent) {
                            if (LOG_INPUT_EVENTS) println("Detected touch slot: $slot | x: $x | y: $y | isTouching: $isTouching")
                            _touches[slot.toInt()].cursorX = x
                            _touches[slot.toInt()].cursorY = y
                            _touches[slot.toInt()].isTouching = isTouching
                        }
                    }
                    else -> {
                        if (value == InputEvent.KEY_UP.code || value == InputEvent.KEY_DOWN.code) {
                            isTouchEvent = false
                        }
                    }
                }
            }
        }
    }

    override fun touchUp(slot: Int) = synchronized(this) {
        if (_touches[slot].isTouching) {
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_SLOT, slot.toLong())
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_PRESSURE, 0)
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TRACKING_ID, -1)
            _touches[slot].isTouching = false
        }
    }

    override fun touchDown(slot: Int) = synchronized(this) {
        if (!_touches[slot].isTouching) {
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_SLOT, slot.toLong())
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TRACKING_ID, slot.toLong())
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TOUCH_MAJOR, 50L + Random.nextInt(-25, 25))
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_PRESSURE, 50L + Random.nextInt(-25, 25))
            sendCoords(_touches[slot].cursorX, _touches[slot].cursorY)
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
                sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_SLOT, slot.toLong())
                sendCoords(x, y)
                eventSync()
            }
        }
    }

    override fun eventSync() = synchronized(this) {
        sendEvent(EventType.EV_SYN, InputEvent.SYN_REPORT, 0)
    }

    override fun tap(slot: Int, x: Int, y: Int) = synchronized(this) {
        touchMove(slot, x, y)
        touchDown(slot); eventSync()
        TimeUnit.MILLISECONDS.sleep(settings.midTapDelay)
        touchUp(slot); eventSync()
        TimeUnit.MILLISECONDS.sleep(settings.postTapDelay)
    }

    override fun gesture(swipes: List<ITouchInterface.Swipe>, duration: Millis) = synchronized(this) {
        swipes.forEach { touchMove(it.slot, it.x1, it.y1) }
        swipes.forEach { touchDown(it.slot) }
        eventSync()

        val dist = swipes.maxOf {
            (it.x2 - it.x1) * (it.x2 - it.x1) + (it.y2 - it.y1) * (it.y2 - it.y1)
        }.toDouble()
        Animations.EaseInOutQuad((sqrt(dist) / 2).roundToLong()).timed(duration).forEach { p ->
            swipes.forEach { swipe ->
                val dy = (swipe.y2 - swipe.y1).toDouble()
                val dx = (swipe.x2 - swipe.x1).toDouble()
                val stepX = (dx * p + swipe.x1).roundToInt()
                val stepY = (dy * p + swipe.y1).roundToInt()
                touchMove(swipe.slot, stepX, stepY)
            }
            eventSync()
        }
        swipes.forEach { touchUp(it.slot) }
        eventSync()
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

    private fun sendCoords(x: Int, y: Int) {
        var xCoord = x
        var yCoord = y
        if (device.properties.displayWidth > device.properties.displayHeight) {
            when (device.orientation) {
                0, 2 -> {
                    xCoord = x
                    yCoord = y
                }
                1, 3 -> {
                    xCoord = device.properties.displayWidth - x
                    yCoord = device.properties.displayHeight - y
                }
            }
        } else {
            when (device.orientation) {
                0, 2 -> {
                    xCoord = y
                    yCoord = device.properties.displayHeight - x
                }
                1, 3 -> {
                    xCoord = device.properties.displayWidth - y
                    yCoord = x
                }
            }
        }
        sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_X, coordToValue(xCoord, InputEvent.ABS_MT_POSITION_X))
        sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_Y, coordToValue(yCoord, InputEvent.ABS_MT_POSITION_Y))
    }

    private fun sendEvent(type: EventType, event: InputEvent, value: Long) {
        if (LOG_INPUT_EVENTS) println("sendevent ${devFile.path} $type(${type.code}) $event(${event.code}) $value")
        ByteBuffer.wrap(writeBuffer)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0).putInt(0) // Event time can be left as 0
                .putShort(type.code.toShort())
                .putShort(event.code.toShort())
                .putInt(value.toInt())
        try {
            device.input.getDeviceFileOutputStream(devFile).apply {
                write(writeBuffer)
                write("".toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun valueToCoord(value: Long, event: InputEvent): Int {
        return when (event) {
            InputEvent.ABS_MT_POSITION_X -> {
                val max = touchSpecs[event]?.maxValue ?: 1
                ((value / max.toDouble()) * device.properties.displayWidth).roundToInt()
            }
            InputEvent.ABS_MT_POSITION_Y -> {
                val max = touchSpecs[event]?.maxValue ?: 1
                ((value / max.toDouble()) * device.properties.displayHeight).roundToInt()
            }
            else -> error("Unsupported event")
        }
    }

    private fun coordToValue(coord: Int, event: InputEvent): Long {
        return when (event) {
            InputEvent.ABS_MT_POSITION_X -> {
                val max = touchSpecs[event]?.maxValue ?: 1
                ((coord / device.properties.displayWidth.toDouble()) * max).roundToLong()
            }
            InputEvent.ABS_MT_POSITION_Y -> {
                val max = touchSpecs[event]?.maxValue ?: 1
                ((coord / device.properties.displayHeight.toDouble()) * max).roundToLong()
            }
            else -> error("Unsupported event")
        }
    }
}