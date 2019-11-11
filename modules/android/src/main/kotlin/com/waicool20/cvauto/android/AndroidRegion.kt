package com.waicool20.cvauto.android

import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.util.matching.ITemplateMatcher
import java.awt.Rectangle
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.DataInputStream
import java.io.EOFException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AndroidRegion(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    device: AndroidDevice
) : Region<AndroidDevice>(x, y, width, height, device) {
    companion object {
        private val _fastCaptureMode = AtomicBoolean(false)
        private var screenRecordProcess: Process? = null
        private var lastCapture: BufferedImage? = null
        private var lastCaptureTime = System.currentTimeMillis()
        private var _captureFPS = 0.0

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                screenRecordProcess?.destroy()
            })
        }
    }

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
        if (fastCaptureMode) return doFastCapture()
        var throwable: Throwable? = null
        for (i in 0 until 3) {
            try {
                val inputStream = DataInputStream(device.execute("screencap").inputStream)
                val width = inputStream.read() or (inputStream.read() shl 8) or
                        (inputStream.read() shl 16) or (inputStream.read() shl 24)
                val height = inputStream.read() or (inputStream.read() shl 8) or
                        (inputStream.read() shl 16) or (inputStream.read() shl 24)
                inputStream.skip(8)
                return createByteRGBBufferedImage(width, height, true).apply {
                    inputStream.readFully((raster.dataBuffer as DataBufferByte).data)
                }
            } catch (t: Throwable) {
                throwable = t
            }
        }
        throw throwable ?: error("Could not capture region due to unknown error")
    }

    override fun mapRectangleToRegion(rect: Rectangle): Region<AndroidDevice> {
        return AndroidRegion(rect.x, rect.y, rect.width, rect.height, device)
    }

    override fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<AndroidDevice> {
        return RegionFindResult(mapRectangleToRegion(result.rectangle), result.score)
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