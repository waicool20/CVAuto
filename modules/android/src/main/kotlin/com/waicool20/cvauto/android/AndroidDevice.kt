package com.waicool20.cvauto.android

import com.waicool20.cvauto.core.IDevice
import com.waicool20.cvauto.core.input.IInput

/**
 * Represents an android device
 */
class AndroidDevice(val serial: String) : IDevice {
    /**
     * Wrapper class containing the basic properties of an android device
     */
    data class Properties(
        val androidVersion: String,
        val brand: String,
        val manufacturer: String,
        val model: String,
        val name: String,
        val displayWidth: Int,
        val displayHeight: Int
    )

    /**
     * Properties of this android device
     */
    val properties: Properties

    /**
     * Orientation of the device
     */
    val orientation: Int
        get() = execute("dumpsys input | grep SurfaceOrientation").readText().takeLast(1).toIntOrNull() ?: 0

    init {
        val props = execute("getprop").readLines().mapNotNull {
            Regex("\\[(.*?)]: \\[(.*?)]").matchEntire(it)?.groupValues?.let { it[1] to it[2] }
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

    override val input: IInput
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val screens: List<AndroidRegion> =
        listOf(AndroidRegion(0, 0, properties.displayWidth, properties.displayHeight, this))

    /**
     * Checks if the showing pointer information
     *
     * @returns True if pointer info is on screen
     */
    fun isShowingPointerInfo(): Boolean {
        return execute("settings get system pointer_location").readText() == "1"
    }

    /**
     * Controls whether or not to show the pointer info on screen
     *
     * @param display Displays pointer info if True
     */
    fun displayPointerInfo(display: Boolean) {
        execute("settings put system pointer_location ${if (display) "1" else "0"}")
    }

    /**
     * Toggles pointer info
     */
    fun togglePointerInfo() {
        execute("settings put system pointer_location ${if (isShowingPointerInfo()) "0" else "1"}")
    }

    /**
     * Same as [ADB.execute] but runs the command specifically on this device
     *
     * @param args command or arguments passed onto adb
     * @return [Process] instance of the command
     */
    fun execute(vararg args: String): Process {
        return ADB.execute("-s", serial, "exec-out", *args)
    }

    override fun toString(): String {
        return "AndroidDevice(serial = $serial)"
    }
}