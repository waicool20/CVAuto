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
import com.waicool20.cvauto.core.IDevice
import com.waicool20.cvauto.core.Pixels
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.outputStream

/**
 * Represents an android device
 */
class AndroidDevice internal constructor(val serial: String) :
    IDevice<AndroidDevice, AndroidDisplay, AndroidRegion> {
    class UnexpectedDisconnectException(device: AndroidDevice) :
        Exception("Device ${device.serial} disconnected unexpectedly")

    /**
     * Wrapper class containing the basic properties of an android device
     */
    data class Properties(
        val androidVersion: String,
        val brand: String,
        val manufacturer: String,
        val model: String,
        val name: String,
        val displayWidth: Pixels,
        val displayHeight: Pixels
    )

    enum class Orientation {
        NORMAL, ROTATED
    }

    /**
     * Properties of this android device
     */
    val properties: Properties

    /**
     * Orientation of the device
     */
    val orientation: Orientation
        get() {
            val txt = execute("dumpsys window  | grep init=").readText()
            val (w0, h0, w1, h1) = Regex("init=(\\d+)x(\\d+) \\d+?dpi cur=(\\d+)x(\\d+)")
                .find(txt)?.destructured ?: error("Could not read orientation from device")
            return when {
                w0 == w1 && h0 == h1 -> Orientation.NORMAL
                w0 == h1 && h0 == w1 -> Orientation.ROTATED
                else -> error("Could not determine orientation from device $serial")
            }
        }

    init {
        val props = execute("getprop").readLines().mapNotNull { str ->
            Regex("\\[(.*?)]: \\[(.*?)]").matchEntire(str)?.groupValues?.let { it[1] to it[2] }
        }.toMap()

        val wmsize = execute("wm size").readText().trim()
        val (width, height) = Regex("Physical size: (\\d+?)x(\\d+?)")
            .matchEntire(wmsize)?.destructured
            ?: error("Could not detect display dimensions for device $serial, emulator returned: $wmsize")

        properties = Properties(
            androidVersion = props["ro.build.version.release"] ?: "Unknown",
            brand = props["ro.product.brand"] ?: "Unknown",
            model = props["ro.product.model"] ?: "Unknown",
            manufacturer = props["ro.product.manufacturer"] ?: "Unknown",
            name = props["ro.product.name"] ?: "Unknown",
            displayWidth = width.toInt(),
            displayHeight = height.toInt()
        )
    }

    /**
     * Reference to [Scrcpy] object tied to this device, providing services such as video and input
     */
    var scrcpy = Scrcpy.getForDevice(this)
        private set

    enum class CaptureMethod {
        SCREENCAP, SCRCPY
    }

    /**
     * Capture method to use
     *
     * - [CaptureMethod.SCREENCAP]: Uses `screencap` command, a bit slower but reliable
     * - [CaptureMethod.SCRCPY]: Uses custom scrcpy capture method
     */
    var captureMethod: CaptureMethod = CaptureMethod.SCRCPY

    enum class CompressionMode {
        NONE, GZIP, LZ4
    }

    /**
     * Compression of the captured image can reduce the amount of time it takes to copy it
     * from the emulator to pc memory, therefore reducing latency. Only applicable when
     * [captureMethod] is [CaptureMethod.SCREENCAP]
     *
     * - [CompressionMode.LZ4]: Best latency, default
     * - [CompressionMode.GZIP]: Slower than LZ4
     * - [CompressionMode.NONE]: No compression
     */
    var compressionMode = CompressionMode.LZ4

    override val input = AndroidInput(this)
    override val displays: List<AndroidDisplay>

    init {
        // Push lz4 binary
        var arch = execute("uname -m").readText().trim()
        if (arch == "armv8l") arch = "aarch64"
        try {
            val inStream = AndroidDevice::class.java
                .getResourceAsStream("/com/waicool20/cvauto/android/libs/$arch/lz4")
                ?: error("$arch not supported")
            val tmp = createTempDirectory().resolve("lz4")
            val outStream = tmp.outputStream()
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()
            push(tmp, "/data/local/tmp")
            executeShell("chmod +x /data/local/tmp/lz4")
            tmp.deleteExisting()
            tmp.parent.deleteExisting()
        } catch (e: Exception) {
            // Fallback to None
            compressionMode = CompressionMode.NONE
        }

        val displayRegex = Regex(".*DisplayDeviceInfo.*uniqueId=\".*:(\\d)\", (\\d+) x (\\d+).*")
        displays = execute("dumpsys display").readLines()
            .mapNotNull { displayRegex.matchEntire(it)?.destructured }
            .map { (index, width, height) ->
                AndroidDisplay(
                    this,
                    width.toInt(),
                    height.toInt(),
                    index.toInt()
                )
            }
    }

    /**
     * Checks if device is showing pointer information
     *
     * @returns True if pointer info is on screen
     */
    fun isShowingPointerInfo(): Boolean {
        return executeShell("settings get system pointer_location").readText().contains("1")
    }

    /**
     * Controls whether or not to show the pointer info on screen
     *
     * @param display Displays pointer info if True
     */
    fun displayPointerInfo(display: Boolean) {
        executeShell("settings put system pointer_location ${if (display) "1" else "0"}")
    }

    /**
     * Toggles pointer info
     */
    fun togglePointerInfo() {
        executeShell("settings put system pointer_location ${if (isShowingPointerInfo()) "0" else "1"}")
    }

    /**
     * Checks if device is showing touches
     *
     * @returns True if touches are shown on screen
     */
    fun isShowingTouches(): Boolean {
        return executeShell("settings get system show_touches").readText().contains("1")
    }

    /**
     * Controls whether or not to show touches on screen
     *
     * @param display Displays touches if True
     */
    fun showTouches(display: Boolean) {
        executeShell("settings put system show_touches ${if (display) "1" else "0"}")
    }

    /**
     * Toggles show touches
     */
    fun toggleTouches() {
        executeShell("settings put system show_touches ${if (isShowingTouches()) "0" else "1"}")
    }

    /**
     * Runs a command specifically on this device using "exec-out"
     *
     * @param args command or arguments passed onto adb
     * @return [Process] instance of the command
     */
    fun execute(vararg args: String): Process {
        return ADB.execute("-s", serial, "exec-out", *args)
    }

    /**
     * Same as [execute] but uses "shell" instead of "exec-out",
     * use this if there are input/output issues with [execute]
     *
     * @param args command or arguments passed onto adb
     * @return [Process] instance of the command
     */
    fun executeShell(vararg args: String): Process {
        return ADB.execute("-s", serial, "shell", *args)
    }

    /**
     * Transfer a file to this device
     *
     * @param local Path to file to push
     * @param remote Remote path to push file to
     *
     * @return true if pushed successfully
     */
    fun push(local: Path, remote: String): Boolean {
        return ADB.execute("-s", serial, "push", local.absolutePathString(), remote).run {
            waitFor()
            exitValue() == 0
        }
    }

    /**
     * This will close the current scrcpy process tied to this device and start a new instance
     */
    fun resetScrcpy() {
        scrcpy.close()
        TimeUnit.MILLISECONDS.sleep(200)
        try {
            scrcpy = Scrcpy.getForDevice(this)
        } catch (e: Exception) {
            assertConnected()
            throw e
        }
    }

    /**
     * Checks if this device is still reachable by ADB
     */
    fun isConnected(): Boolean {
        return ADB.getDevices().contains(this)
    }

    internal fun assertConnected() {
        if (!isConnected()) throw UnexpectedDisconnectException(this)
    }

    override fun toString(): String {
        return "AndroidDevice(serial = $serial)"
    }
}
