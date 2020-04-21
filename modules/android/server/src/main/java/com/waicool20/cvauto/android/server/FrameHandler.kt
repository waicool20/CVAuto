package com.waicool20.cvauto.android.server

import android.graphics.PixelFormat
import android.media.ImageReader
import android.view.IRotationWatcher
import java.io.Closeable
import java.net.Socket
import java.net.SocketException
import kotlin.math.min
import kotlin.system.measureTimeMillis

class FrameHandler(private val socket: Socket) : Closeable, ImageReader.OnImageAvailableListener {
    private val outputStream = socket.getOutputStream()
    private val writeBuffer = ByteArray(8192)
    private val imageReader = ImageReader.newInstance(Device.width, Device.height, PixelFormat.RGBA_8888, 2)

    init {
        Logger.i("Initializing new frame handler")
        imageReader.setOnImageAvailableListener(this, Server.serverHandler)
        KServiceManager.windowManager.watchRotation(object : IRotationWatcher.Stub() {
            override fun onRotationChanged(rotation: Int) {
                Logger.i("Device rotated, closing frame handler")
                close()
            }
        }, Device.DEFAULT_DISPLAY)
        Device.registerSurface(imageReader.surface)
    }

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireLatestImage()
        try {
            val imageBuffer = image.planes[0].buffer
            measureTimeMillis {
                var rem: Int
                while (imageBuffer.hasRemaining()) {
                    rem = min(imageBuffer.remaining(),  writeBuffer.size)
                    imageBuffer.get(writeBuffer, 0, rem)
                    outputStream.write(writeBuffer, 0, rem)
                }
            }.let { Logger.i("Buffer $it") }
        } catch (e: SocketException) {
            Logger.i("Socket closed")
            close()
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        } finally {
            image.close()
        }
    }

    override fun close() {
        imageReader.close()
        socket.close()
        Logger.i("Closed frame handler")
    }

    fun waitFor() {
        while (!socket.isClosed) Thread.sleep(100)
    }
}