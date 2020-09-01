package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.input.IInput
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

inline class DeviceFile(val path: String)

class AndroidInput internal constructor(private val device: AndroidDevice) : IInput {
    private val devmap = mutableMapOf<DeviceFile, Socket>()

    init {
        device.executeShell("killall", "toybox")
        ADB.execute("forward", "--remove-all")
    }

    override val mouse = null
    override val keyboard = AndroidKeyboard.getForDevice(device)
    override val touchInterface = AndroidTouchInterface.getForDevice(device)

    fun getDeviceFileOutputStream(deviceFile: DeviceFile): OutputStream {
        val socket = devmap[deviceFile]
        if (socket != null && !socket.isClosed) {
            return socket.getOutputStream()
        }
        val port = getNextAvailablePort()
        val process = device.executeShell("sh", "-c", "'toybox nc -l -p $port >${deviceFile.path}'")
        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        ADB.execute("-s", device.serial, "forward", "tcp:$port", "tcp:$port")

        Thread.sleep(1000) // Needed otherwise socket sometimes doesnt connect to netcat properly
        val newSocket = Socket("127.0.0.1", port).apply { tcpNoDelay = true }
        devmap[deviceFile] = newSocket
        return newSocket.getOutputStream()
    }

    private fun getNextAvailablePort(): Int {
        var port: Int? = null
        for (i in 8080..8180) {
            try {
                val s = ServerSocket(i)
                port = s.localPort
                s.close()
                return port
            } catch (e: Exception) {
                continue
            }
        }
        return port ?: error("Cannot find available port in range 8080-8180")
    }
}
