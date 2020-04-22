package com.waicool20.cvauto.android.server

import android.os.Handler
import android.os.Looper
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.Closeable
import java.net.ServerSocket
import kotlin.concurrent.thread

val options = Options().apply {
    addOption("p", "port", true, "Server port")
}

fun main(args: Array<String>) {
    Logger.i("Initializing cvauto android server")
    val cmd = DefaultParser().parse(options, args)
    when {
        cmd.hasOption("p") -> Server.port = cmd.getOptionValue("p")?.toIntOrNull() ?: error("Invalid port")
    }
    Server.run()
}

object Server : Thread(), Closeable {
    const val RESPONSE_OK = 0
    const val RESPONSE_ERR = 1

    const val MODE_TEST = 0
    const val MODE_FRAME = 1
    const val MODE_INPUT = 2

    var port = 8080
    lateinit var serverSocket: ServerSocket
    lateinit var serverHandler: Handler

    override fun run() {
        spawnLooper()
        serverSocket = ServerSocket(port)
        Logger.i("Listening on port $port")
        while (true) {
            try {
                Logger.i("Waiting for connection...")
                SocketHandler(serverSocket.accept()).start()
                Logger.i("Spawned handler")
            } catch (e: Exception) {
                break
            }
        }
        Logger.i("Server closed")
    }

    override fun close() {
        serverSocket.close()
    }

    private fun spawnLooper() {
        thread(name = "Server Looper") {
            Looper.prepare()
            serverHandler = Handler()
            Looper.loop()
        }
    }
}
