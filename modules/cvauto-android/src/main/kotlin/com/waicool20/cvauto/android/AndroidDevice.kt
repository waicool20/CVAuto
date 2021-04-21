package com.waicool20.cvauto.android

import com.waicool20.cvauto.android.input.AndroidInput
import com.waicool20.cvauto.core.IDevice
import com.waicool20.cvauto.core.Pixels
import java.nio.file.Files
import java.nio.file.Path

/**
 * Represents an android device
 */
class AndroidDevice internal constructor(val serial: String) : IDevice {
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

    /**
     * Properties of this android device
     */
    val properties: Properties

    /**
     * Orientation of the device
     */
    val orientation: Int
        get() = execute("dumpsys input | grep SurfaceOrientation").readText().takeLast(1)
            .toIntOrNull() ?: 0

    init {
        val props = execute("getprop").readLines().mapNotNull { str ->
            Regex("\\[(.*?)]: \\[(.*?)]").matchEntire(str)?.groupValues?.let { it[1] to it[2] }
        }.toMap()
        val (width, height) = Regex("Physical size: (\\d+?)x(\\d+?)")
            .matchEntire(execute("wm size").readText().trim())?.destructured
            ?: error("Could not detect display dimensions for device $serial")

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

    override val input = AndroidInput(this)
    override val screens: List<AndroidRegion> =
        listOf(AndroidRegion(0, 0, properties.displayWidth, properties.displayHeight, this, 0))

    init {
        // Push lz4 binary
        var arch = execute("uname -m").readText().trim()
        if (arch == "armv8l") arch = "aarch64"
        try {
            val inStream = AndroidDevice::class.java
                .getResourceAsStream("/com/waicool20/cvauto/android/libs/$arch/lz4")
                ?: error("$arch not supported")
            val tmp = Files.createTempDirectory("").resolve("lz4")
            val outStream = Files.newOutputStream(tmp)
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()
            push(tmp, "/data/local/tmp")
            executeShell("chmod +x /data/local/tmp/lz4")
            Files.delete(tmp)
            Files.delete(tmp.parent)
        } catch (e: Exception) {
            // Fallback to GZIP
            screens.forEach { it.compressionMode = AndroidRegion.CompressionMode.GZIP }
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
        return ADB.execute("-s", serial, "push", "${local.toAbsolutePath()}", remote).run {
            waitFor()
            exitValue() == 0
        }
    }

    /**
     * This will close the current scrcpy process tied to this device and start a new instance
     */
    fun resetScrcpy() {
        scrcpy.close()
        scrcpy = Scrcpy.getForDevice(this)
    }

    override fun toString(): String {
        return "AndroidDevice(serial = $serial)"
    }
}