package com.waicool20.cvauto.android.server

import java.net.Socket

class SocketHandler(private val socket: Socket) : Thread() {
    companion object {
        const val MODE_FRAME = 0
        const val MODE_INPUT = 1
    }

    private val outputStream get() = socket.getOutputStream()
    private val inputStream get() = socket.getInputStream()

    override fun run() {
        val mode = inputStream.read()
        Logger.i("Started new socket with mode: $mode")
        when (mode) {
            MODE_FRAME -> FrameHandler(socket).waitFor()
            MODE_INPUT -> InputHandler(socket).waitFor()
            else -> Logger.e("Unknown mode: $mode")
        }
        socket.close()
    }
}