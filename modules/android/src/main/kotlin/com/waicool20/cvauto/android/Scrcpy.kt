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
import java.io.Closeable
import java.net.Socket
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream

/**
 * Wrapper class for scrcpy process and sockets
 */
class Scrcpy private constructor(
    val port: Int,
    val process: Process,
    val video: Socket,
    val control: Socket
) : Closeable {
    companion object {
        val VERSION = "1.25"
        val PATH: Path = CVAutoAndroid.HOME_DIR.resolve("scrcpy-server")

        internal fun getForDevice(device: AndroidDevice): Scrcpy {
            extractServer()

            device.push(PATH, "/data/local/tmp/")
            device.executeShell("mv", "/data/local/tmp/scrcpy-server{,.jar}")

            val port = NetUtils.getNextAvailablePort()
            ADB.execute("-s", device.serial, "forward", "tcp:$port", "localabstract:scrcpy")

            val process = ProcessBuilder(
                ADB.binPath.absolutePathString(), "-s", device.serial, "shell",
                "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server",
                VERSION, "log_level=INFO", "tunnel_forward=true"
            ).inheritIO().also {
                it.environment()["ADB"] = ADB.binPath.absolutePathString()
            }.start()

            Thread.sleep(1000) // Needed otherwise socket sometimes doesnt connect properly

            val vs = Socket("127.0.0.1", port).apply { tcpNoDelay = true }
            val vsi = vs.getInputStream()
            val cs = Socket("127.0.0.1", port).apply { tcpNoDelay = true }

            // Read device information from video socket and print it
            if (vsi.read() != 0) error("Could not connect to scrcpy server")
            val readBuffer = ByteArray(68)
            vsi.read(readBuffer)
            val deviceNameIndex =
                readBuffer.indexOfFirst { it == 0.toByte() }.takeIf { it != -1 } ?: 64
            val deviceName = readBuffer.sliceArray(0 until deviceNameIndex).decodeToString()
            val width = readBuffer.sliceArray(64..65).let { it[0].toInt() shl 8 or it[1].toInt() }
            val height = readBuffer.sliceArray(66..67).let { it[0].toInt() shl 8 or it[1].toInt() }

            println("Connected to $deviceName, width: $width, height: $height")

            val scrcpy = Scrcpy(port, process, vs, cs)
            // Make sure adb stuff gets cleaned up during shutdown
            Runtime.getRuntime().addShutdownHook(Thread { scrcpy.close() })
            return scrcpy
        }

        private fun extractServer() {
            val inStream = AndroidInput::class.java
                .getResourceAsStream("/com/waicool20/cvauto/android/scrcpy-server-v$VERSION")!!
            val outStream = PATH.outputStream()
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()
        }
    }

    var isClosed = false
        private set

    override fun close() {
        isClosed = true
        control.close()
        video.close()
        process.destroy()
        ADB.execute("forward", "--remove", "tcp:$port")
    }
}
