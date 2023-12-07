/*
 * The MIT License (MIT)
 *
 * Copyright (c) waicool20
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.waicool20.cvauto.android

import com.waicool20.cvauto.android.input.AndroidInput
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.io.Closeable
import java.net.Socket
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.outputStream

/**
 * Wrapper class for scrcpy process and sockets
 */
class Scrcpy private constructor(val device: AndroidDevice) : Closeable {
    private val logger = LoggerFactory.getLogger(Scrcpy::class.java)

    companion object {
        val VERSION = "v2.2"
        val PATH: Path = CVAutoAndroid.HOME_DIR.resolve("scrcpy-server")

        internal fun getForDevice(device: AndroidDevice): Scrcpy {
            return Scrcpy(device)
        }
    }

    val port: Int = NetUtils.getNextAvailablePort()
    val process: Process
    val video: Socket
    val audio: Socket? = null
    val control: Socket

    private var _isClosed = false

    val isClosed: Boolean
        get() {
            return _isClosed || video.isClosed || audio?.isClosed == true || control.isClosed
        }

    init {
        extractServer()
        ADB.execute("-s", device.serial, "forward", "tcp:$port", "localabstract:scrcpy")

        process = runServerCommand("tunnel_forward=true", "audio=false")

        Thread.sleep(1000) // Needed otherwise socket sometimes doesnt connect properly

        val videoSocket = Socket("127.0.0.1", port).apply { tcpNoDelay = true }
        val vsi = videoSocket.getInputStream()
        //val audioSocket = Socket("127.0.0.1", port).apply { tcpNoDelay = true }
        //val asi = audioSocket.getInputStream()
        val controlSocket = Socket("127.0.0.1", port).apply { tcpNoDelay = true }

        // Read device information from video socket and print it
        if (vsi.read() != 0) error("Could not connect to scrcpy video socket")
        //if (asi.read() != 0) error("Could not connect to scrcpy audio socket")
        val readBuffer = ByteArray(64)
        vsi.read(readBuffer)
        val deviceNameIndex =
            readBuffer.indexOfFirst { it == 0.toByte() }.takeIf { it != -1 } ?: 64
        val deviceName = readBuffer.sliceArray(0 until deviceNameIndex).decodeToString()

        println("Connected to $deviceName")
        video = videoSocket
        control = controlSocket

        // Make sure adb stuff gets cleaned up during shutdown
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    fun getDisplayInfo(): List<Dimension> {
        val regex = Regex(".*\\((\\d+)x(\\d+)\\).*")
        return runServerCommand("list_displays=true").readLines()
            .mapNotNull { line ->
                val match = regex.matchEntire(line) ?: return@mapNotNull null
                val (wstring, hstring) = match.destructured
                return@mapNotNull Dimension(wstring.toInt(), hstring.toInt())
            }
    }

    override fun close() {
        _isClosed = true
        control.close()
        audio?.close()
        video.close()
        process.destroy()
        ADB.execute("forward", "--remove", "tcp:$port")
    }

    private fun runServerCommand(vararg args: String): Process {
        device.push(PATH, "/data/local/tmp/")
        device.executeShell("mv", "/data/local/tmp/scrcpy-server{,.jar}")
        return device.executeShell(
            "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server",
            VERSION, "log_level=INFO", *args
        )
    }

    private fun extractServer() {
        val inStream = AndroidInput::class.java
            .getResourceAsStream("/com/waicool20/cvauto/android/scrcpy-server-$VERSION")!!
        val outStream = PATH.outputStream()
        inStream.copyTo(outStream)
        inStream.close()
        outStream.close()
    }
}
