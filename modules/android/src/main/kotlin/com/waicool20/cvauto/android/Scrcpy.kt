/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        val VERSION = "1.23c"
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
                "1.23", "log_level=INFO", "tunnel_forward=true"
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
