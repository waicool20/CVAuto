package com.waicool20.cvauto.android

import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.input.ITouchInterface
import net.jpountz.lz4.LZ4FrameInputStream
import java.awt.color.ColorSpace
import java.awt.image.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

class AndroidRegion(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    device: AndroidDevice,
    screen: Int
) : Region<AndroidDevice, AndroidRegion>(x, y, width, height, device, screen) {

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
    }

    enum class CompressionMode {
        NONE, GZIP, LZ4
    }

    /**
     * Compression of the captured image can reduce the amount of time it takes to copy it
     * from the emulator to pc memory, therefore reducing latency.
     *
     * - LZ4: Best latency, default
     * - GZIP: Slower than LZ4 but still much better than NONE
     * - NONE: No compression
     */
    var compressionMode = CompressionMode.LZ4

    override fun capture(): BufferedImage {
        val future = executor.submit<BufferedImage> {
            val last = device.screens[screen]._lastScreenCapture
            if (last != null && System.currentTimeMillis() - last.first <= 66) {
                return@submit if (isDeviceScreen()) {
                    last.second
                } else {
                    last.second.getSubimage(x, y, width, height)
                }
            }
            val capture = doNormalCapture()
            device.screens[screen]._lastScreenCapture = System.currentTimeMillis() to capture
            return@submit if (isDeviceScreen()) {
                capture
            } else {
                capture.getSubimage(x, y, width, height)
            }
        }
        try {
            return future.get(CAPTURE_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            throw CaptureTimeoutException()
        }
    }

    override fun click(random: Boolean) {
        if (random) {
            val r = randomPoint()
            device.input.touchInterface.tap(0, r.x, r.y)
        } else {
            device.input.touchInterface.tap(0, centerX.roundToInt(), centerY.roundToInt())
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
        device.input.touchInterface.swipe(swipe, duration)
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
        device.input.touchInterface.pinch(
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
        for (i in 0 until 3) {
            val process: Process
            val inputStream = when (compressionMode) {
                CompressionMode.NONE -> {
                    process = device.execute("screencap")
                    process.inputStream.buffered(1024 * 1024)
                }
                CompressionMode.GZIP -> {
                    process = device.execute("screencap | toybox gzip -1")
                    GZIPInputStream(process.inputStream).buffered(1024 * 1024)
                }
                CompressionMode.LZ4 -> {
                    // LZ4 mode runs slower if buffered, maybe it has internal buffer already
                    process = device.execute("screencap | /data/local/tmp/lz4 -c -1")
                    LZ4FrameInputStream(process.inputStream)
                }
            }
            try {
                val width = inputStream.read() or (inputStream.read() shl 8) or
                    (inputStream.read() shl 16) or (inputStream.read() shl 24)
                val height = inputStream.read() or (inputStream.read() shl 8) or
                    (inputStream.read() shl 16) or (inputStream.read() shl 24)
                if (width < 0 || height < 0) continue
                if (device.screens[screen].width != width) device.screens[screen].width = width
                if (device.screens[screen].height != height) device.screens[screen].height = height
                if (device.properties.androidVersion.split(".")[0].toInt() >= 8) {
                    // ADB screencap on android versions 8 and above have 8 extra bytes instead of 4
                    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/pie-release/cmds/screencap/screencap.cpp#247
                    inputStream.skip(8)
                } else {
                    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-release/cmds/screencap/screencap.cpp#191
                    inputStream.skip(4)
                }
                val img = createByteRGBBufferedImage(width, height, false)
                val buffer = (img.raster.dataBuffer as DataBufferByte).data
                for (n in buffer.indices step 3) {
                    // Data comes in RGBA byte format, alpha byte is unused and is discarded
                    buffer[n] = inputStream.read().toByte()
                    buffer[n + 1] = inputStream.read().toByte()
                    buffer[n + 2] = inputStream.read().toByte()
                    inputStream.skip(1)
                }
                return img
            } catch (t: Throwable) {
                throwables.add(t)
            } finally {
                inputStream.close()
                process.destroy()
            }
        }
        throw throwables.reduce { acc, _ -> Exception(acc) }
    }

    @Throws(NegativeArraySizeException::class)
    private fun createByteRGBBufferedImage(
        width: Int,
        height: Int,
        hasAlpha: Boolean = false
    ): BufferedImage {
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