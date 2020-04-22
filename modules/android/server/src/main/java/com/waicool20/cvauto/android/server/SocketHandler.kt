package com.waicool20.cvauto.android.server

import com.waicool20.cvauto.android.server.Server.MODE_FRAME
import com.waicool20.cvauto.android.server.Server.MODE_INPUT
import com.waicool20.cvauto.android.server.Server.RESPONSE_ERR
import com.waicool20.cvauto.android.server.Server.RESPONSE_OK
import java.net.Socket

class SocketHandler(private val socket: Socket) : Thread() {
    private val outputStream get() = socket.getOutputStream()
    private val inputStream get() = socket.getInputStream()

    override fun run() {
        val mode = inputStream.read()
        Logger.i("Started new socket with mode: $mode")
        val handler = when (mode) {
            MODE_FRAME -> FrameHandler(socket)
            MODE_INPUT -> InputHandler(socket)
            else -> {
                Logger.e("Unknown mode: $mode")
                outputStream.write(RESPONSE_ERR)
                return
            }
        }
        outputStream.write(RESPONSE_OK)
        handler.waitFor()
        socket.close()
    }
}