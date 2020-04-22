package com.waicool20.cvauto.android

import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.util.matching.ITemplateMatcher
import java.awt.Rectangle
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class AndroidRegion(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    device: AndroidDevice,
    screen: Int
) : Region<AndroidDevice>(x, y, width, height, device, screen) {
    companion object {
        private val _fastCaptureMode = AtomicBoolean(false)
        private var screenRecordProcess: Process? = null
        private var lastCapture: BufferedImage? = null
        private var lastCaptureTime = System.currentTimeMillis()
        private var _captureFPS = 0.0

        private var frameSocket: Socket? = null

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                screenRecordProcess?.destroy()
            })
        }
    }

    var useServer: Boolean = true

    /**
     * Enables fast capture mode, applies to all [AndroidRegion]
     * [capture] may return blank images shortly after enabling, some time is needed before
     * process is fully started, typically waiting a few hundred ms is enough
     */
    var fastCaptureMode: Boolean
        get() = _fastCaptureMode.get()
        set(value) {
            // Starts the process if trying to enable it
            if (value) doFastCapture()
            _fastCaptureMode.set(value)
        }

    /**
     * Frames per Second for device, only valid when using fast capture mode
     */
    val captureFPS get() = _captureFPS

    override fun capture(): BufferedImage {
        val capture = if (useServer) {
            doServerCapture().getSubimage(x, y, width, height)
        } else {
            if (fastCaptureMode) {
                doFastCapture().getSubimage(x, y, width, height)
            } else {
                doNormalCapture().getSubimage(x, y, width, height)
            }
        }
        if (device.screens.contains(this)) _lastScreenCapture = System.currentTimeMillis() to capture
        return capture
    }

    override fun mapRectangleToRegion(rect: Rectangle): Region<AndroidDevice> {
        return AndroidRegion(rect.x + x, rect.y + y, rect.width, rect.height, device, screen)
    }

    override fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<AndroidDevice> {
        return RegionFindResult(mapRectangleToRegion(result.rectangle), result.score)
    }

    override fun click(random: Boolean) {
        if (random) {
            val r = randomPoint()
            device.input.touchInterface?.tap(0, r.x, r.y)
        } else {
            device.input.touchInterface?.tap(0, centerX.roundToInt(), centerY.roundToInt())
        }
    }

    override fun type(text: String) {
        click()
        device.input.keyboard.type(text)
    }

    /**
     * Swipes from this region to the other region
     * For more complex operations please use the devices respective [IInput]
     *
     * @param other Other region to swipe to
     * @param duration Duration of the swipe
     * @param random If true the source and destination location will be random points in their respective regions
     */
    fun swipeTo(other: AndroidRegion, duration: Millis = 1000, random: Boolean = true) {
        val swipe = if (random) {
            val src = randomPoint()
            val dest = other.randomPoint()
            ITouchInterface.Swipe(0, src.x, src.y, dest.x, dest.y)
        } else {
            ITouchInterface.Swipe(
                0,
                centerX.roundToInt(), centerY.roundToInt(),
                other.centerX.roundToInt(), other.centerY.roundToInt()
            )
        }
        device.input.touchInterface?.swipe(swipe, duration)
    }

    /**
     * Sends a pinch gesture, centered at this regions center
     *
     * @param r1 Starting radius of the pinch
     * @param r2 End radius of the pinch
     * @param angle Pinch angle
     * @param duration Duration of the pinch
     */
    fun pinch(r1: Int, r2: Int, angle: kotlin.Double, duration: Millis = 1000) {
        device.input.touchInterface?.pinch(
            centerX.roundToInt(), centerY.roundToInt(),
            r1, r2, angle, duration
        )
    }

    override fun copy(
        x: Pixels,
        y: Pixels,
        width: Pixels,
        height: Pixels,
        device: AndroidDevice,
        screen: Int
    ): AndroidRegion {
        return AndroidRegion(x, y, width, height, device, screen)
    }

    private fun doNormalCapture(): BufferedImage {
        val throwables = mutableListOf<Throwable>()
        for (i in 0 until 10) {
            val process = device.execute("screencap")
            val inputStream = DataInputStream(process.inputStream)
            try {
                val width = inputStream.read() or (inputStream.read() shl 8) or
                        (inputStream.read() shl 16) or (inputStream.read() shl 24)
                val height = inputStream.read() or (inputStream.read() shl 8) or
                        (inputStream.read() shl 16) or (inputStream.read() shl 24)
                if (width < 0 || height < 0) {
                    inputStream.close()
                    continue
                }
                inputStream.skip(8)
                val img = createByteRGBBufferedImage(width, height, true)
                inputStream.readFully((img.raster.dataBuffer as DataBufferByte).data)
                inputStream.close()
                return img
            } catch (t: Throwable) {
                throwables.add(t)
                inputStream.close()
                process.waitFor(1000, TimeUnit.MILLISECONDS)
                process.destroy()
            }
        }
        throwables.forEach { it.printStackTrace() }
        error("Could not capture region due to various errors")
    }

    private fun doFastCapture(): BufferedImage {
        if (screenRecordProcess == null || screenRecordProcess?.isAlive == false) {
            thread(isDaemon = true) {
                screenRecordProcess?.destroy()
                screenRecordProcess = device.execute("screenrecord", "--output-format=raw-frames", "-")
                val inputStream = DataInputStream(screenRecordProcess!!.inputStream)
                while (screenRecordProcess?.isAlive == true && fastCaptureMode) {
                    lastCapture = try {
                        createByteRGBBufferedImage(device.properties.displayWidth, device.properties.displayHeight)
                            .apply { inputStream.readFully((raster.dataBuffer as DataBufferByte).data) }
                    } catch (e: EOFException) {
                        break
                    }
                    val current = System.currentTimeMillis()
                    _captureFPS = (captureFPS + 1000.0 / (current - lastCaptureTime)) / 2
                    lastCaptureTime = current
                }
                screenRecordProcess?.destroy()
                _captureFPS = 0.0
            }
        }
        return lastCapture ?: createByteRGBBufferedImage(
            device.properties.displayWidth,
            device.properties.displayHeight
        )
    }

    private fun doServerCapture(): BufferedImage {
        if (frameSocket == null || frameSocket?.isClosed == true) {
            val s = device.server.openFrameSocket()
            thread(isDaemon = true) {
                val inputStream = DataInputStream(s.getInputStream())
                while (useServer) {
                    lastCapture = try {
                        createByteRGBBufferedImage(device.properties.displayWidth, device.properties.displayHeight, true)
                            .apply { inputStream.readFully((raster.dataBuffer as DataBufferByte).data) }
                    } catch (e: Exception) {
                        break
                    }
                    val current = System.currentTimeMillis()
                    _captureFPS = (captureFPS + 1000.0 / (current - lastCaptureTime)) / 2
                    lastCaptureTime = current
                }
                _captureFPS = 0.0
            }
        }
        while (lastCapture == null) Thread.sleep(10)
        return lastCapture!!
    }

    @Throws(NegativeArraySizeException::class)
    private fun createByteRGBBufferedImage(width: Int, height: Int, hasAlpha: Boolean = false): BufferedImage {
        val cs = ColorSpace.getInstance(ColorSpace.CS_sRGB)
        val cm: ColorModel
        val raster: WritableRaster
        if (hasAlpha) {
            cm = ComponentColorModel(cs, true, false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE)
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null)
        } else {
            cm = ComponentColorModel(cs, false, false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE)
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null)
        }
        return BufferedImage(cm, raster, false, null)
    }
}