package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.android.lineSequence
import com.waicool20.cvauto.core.input.IInput
import java.io.OutputStream
import java.lang.Integer.max
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread

inline class DeviceFile(val path: String)

class AndroidInput internal constructor(private val device: AndroidDevice) : IInput {
    class ScrcpySockets(val process: Process, val video: Socket, val control: Socket)

    private val devmap = mutableMapOf<DeviceFile, Socket>()
    private val scrcpyVersion = "1.17"
    private val dataDir = Paths.get(System.getProperty("user.home")).resolve(".cvauto/android")
    private val scrcpyServerPath = dataDir.resolve("scrcpy-server")

    private var scrcpySockets: ScrcpySockets? = null

    init {
        device.executeShell("killall", "toybox")
        ADB.execute("forward", "--remove-all")
    }

    override val mouse = null
    override val keyboard = AndroidKeyboard.getForDevice(device)
    override val touchInterface = AndroidTouchInterface.getForDevice(device)

    fun getScrcpySockets(): ScrcpySockets {
        var sockets = scrcpySockets
        if (sockets != null && !sockets.video.isClosed && !sockets.control.isClosed) {
            return sockets
        }

        sockets?.process?.destroy()

        if (Files.notExists(scrcpyServerPath)) {
            extractScrcpyServer()
        }

        device.push(scrcpyServerPath, "/data/local/tmp/")
        device.executeShell("mv", "/data/local/tmp/scrcpy-server{,.jar}")

        val port = getNextAvailablePort()
        ADB.execute("-s", device.serial, "forward", "tcp:$port", "localabstract:scrcpy")

        val process = device.executeShell(
            "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server",
            scrcpyVersion,
            /* Log Level ( INFO, DEBUG, WARN, ERROR ) */
            "DEBUG",
            /* Max Size */
            "${max(device.properties.displayHeight, device.properties.displayWidth)}",
            /* Bitrate ( kbps ) */
            "5000",
            /* FPS */
            "60",
            /* Video Orientation ( 0 for normal orientation, inc 1 for every 90 deg ) */
            "0",
            /* Tunnel Forward ( true if using adb forward ) */
            "true",
            /* Crop ( - for No crop, otherwise w:h:x:y ) */
            "-",
            /* Send frame metadata */
            "true",
            /* Control */
            "true",
            /* Display ID */
            "0",
            /* Show Touches */
            "true",
            /* Stay Awake */
            "true",
            /* Codec Options ( - for default ) */
            "-",
            /* Encoder ( - for default ) */
            "-",
        )

        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        thread(isDaemon = true, name = "Scrcpy Logging") {
            process.lineSequence().forEach(::println)
        }

        Thread.sleep(1000) // Needed otherwise socket sometimes doesnt connect properly

        val vs = Socket("127.0.0.1", port).apply { tcpNoDelay = true }
        val vsi = vs.getInputStream()
        val cs = Socket("127.0.0.1", port).apply { tcpNoDelay = true }

        // Read device information from video socket and print it
        if (vsi.read() != 0) error("Could not connect to scrcpy server")
        val readBuffer = ByteArray(68)
        vsi.read(readBuffer)
        val deviceNameIndex = readBuffer.indexOfFirst { it == 0.toByte() }.takeIf { it != -1 } ?: 64
        val deviceName = readBuffer.sliceArray(0 until deviceNameIndex).decodeToString()
        val width = readBuffer.sliceArray(64..65).let { it[0].toInt() shl 8 or it[1].toInt() }
        val height = readBuffer.sliceArray(66..67).let { it[0].toInt() shl 8 or it[1].toInt() }

        println("Connected to $deviceName, width: $width, height: $height")

        sockets = ScrcpySockets(process, vs, cs)
        scrcpySockets = sockets
        return sockets
    }

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

    private fun extractScrcpyServer() {
        val inStream = AndroidInput::class.java
            .getResourceAsStream("/com/waicool20/cvauto/android/scrcpy-server-v$scrcpyVersion")!!
        val outStream = Files.newOutputStream(scrcpyServerPath)
        inStream.copyTo(outStream)
        inStream.close()
        outStream.close()
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
