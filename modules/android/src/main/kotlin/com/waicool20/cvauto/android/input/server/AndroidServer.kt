package com.waicool20.cvauto.android.input.server

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.concurrent.thread

/**
 * Utility class to help starting the server instance on the device and getting sockets for communication
 */
class AndroidServer(
    private val device: AndroidDevice
) {
    companion object {
        const val RESPONSE_OK = 0
        const val RESPONSE_ERR = 1

        const val MODE_TEST = 0
        const val MODE_FRAME = 1
        const val MODE_INPUT = 2
    }

    private val port: Int
    private val apkPath = ADB.dataDir.resolve("server.apk")

    private var serverProcess: Process? = null

    init {
        port = selectNewPort()
        Runtime.getRuntime().addShutdownHook(thread {
            serverProcess?.destroy()
        })
    }

    fun openFrameSocket() = openSocket(MODE_FRAME)
    fun openInputSocket() = openSocket(MODE_INPUT)

    private tailrec fun openSocket(mode: Int): Socket {
        if (serverProcess == null || serverProcess?.isAlive == false) spawnNewServer()
        val s = Socket("127.0.0.1", port)
        s.getOutputStream().write(mode)
        return if (s.getInputStream().read() == RESPONSE_OK) s else openSocket(mode)
    }

    private fun selectNewPort(): Int {
        var port = 8080
        for (i in 0 until 10) {
            val s = try {
                ServerSocket(port + i)
            } catch (e: Exception) {
                continue
            }
            port = s.localPort
            s.close()
            return port
        }
        error("No available ports in 8080 - 8090 range")
    }

    private fun spawnNewServer() {
        println("Android server will run on port: $port")
        ADB.execute("forward", "--remove-all").waitFor()
        ADB.execute("forward", "tcp:$port", "tcp:$port").waitFor()
        val i = javaClass.getResourceAsStream("/server.apk")
        Files.copy(i, apkPath, StandardCopyOption.REPLACE_EXISTING)
        ADB.execute("push", "$apkPath", "/data/local/tmp").waitFor()
        device.executeShell("killall", "app_process").waitFor()
        val p = device.executeShell(
            "CLASSPATH=/data/local/tmp/server.apk", "app_process", "/",
            "com.waicool20.cvauto.android.server.ServerKt", "-p", "$port"
        )
        serverProcess = p
        val reader = p.inputStream.bufferedReader()
        try {
            reader.readLine()
            if (reader.readLine() == null) spawnNewServer()
        } catch (e: Exception) {
            spawnNewServer()
        }
    }
}