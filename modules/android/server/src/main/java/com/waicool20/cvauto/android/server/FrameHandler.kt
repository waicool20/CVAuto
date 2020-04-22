package com.waicool20.cvauto.android.server

import java.net.Socket
import java.nio.ByteBuffer

class FrameHandler(private val socket: Socket) : Handler {
    private val outputStream = socket.getOutputStream()

    override fun close() {
        socket.close()
        Logger.i("Closed frame handler")
    }

    override fun waitFor() {
        Logger.i("Initializing new frame handler")
        val bitmap = KSurfaceControl.screenshot(Device.width, Device.height)
        val buffer = ByteBuffer.allocate(bitmap.allocationByteCount)
        bitmap.copyPixelsToBuffer(buffer)
        outputStream.write(buffer.array())
        close()
    }
}