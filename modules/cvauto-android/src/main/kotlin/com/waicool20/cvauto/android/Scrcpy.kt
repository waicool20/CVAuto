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
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.notExists
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
        val VERSION = "1.17"
        val PATH: Path = CVAutoAndroid.HOME_DIR.resolve("scrcpy-server")
        val MAX_SIZE = 1024

        internal fun getForDevice(device: AndroidDevice): Scrcpy {
            if (PATH.notExists()) extractServer()

            device.push(PATH, "/data/local/tmp/")
            device.executeShell("mv", "/data/local/tmp/scrcpy-server{,.jar}")

            val port = getNextAvailablePort()
            ADB.execute("-s", device.serial, "forward", "tcp:$port", "localabstract:scrcpy")

            val process = ProcessBuilder(
                ADB.binPath.absolutePathString(), "-s", device.serial, "shell",
                "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server",
                VERSION,
                /* Log Level ( INFO, DEBUG, WARN, ERROR ) */
                "DEBUG",
                /* Max Size */
                "$MAX_SIZE",
                /* Bitrate ( kbps ) */
                "1",
                /* FPS */
                "1",
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

    override fun close() {
        process.destroy()
        ADB.execute("forward", "--remove", "tcp:$port")
    }
}