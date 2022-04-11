package com.waicool20.cvauto.android

import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

/**
 * Wrapper for android adb tool, accessing this object downloads android platform tools to %home%/cvauto/android
 * if it doesn't exist
 */
object ADB {
    private val platformToolsDir = CVAutoAndroid.HOME_DIR.resolve("platform-tools")
    val binPath = run {
        val os = System.getProperty("os.name").lowercase()
        val adbName = when {
            os.contains("win") -> "adb.exe"
            os.contains("mac") -> "adb"
            os.contains("linux") -> "adb"
            else -> error("Unsupported OS: $os")
        }
        platformToolsDir.resolve(adbName)
    }

    init {
        if (binPath.notExists()) downloadPlatformTools()
        if (!adbBinaryOk()) error("Problem initializing adb")
    }

    private val deviceCache = mutableMapOf<String, AndroidDevice>()

    /**
     * Get instance of [AndroidDevice] with the given serial if its online
     */
    fun getDevice(serial: String): AndroidDevice? {
        val device = deviceCache[serial] ?: getDevices().find { it.serial == serial }
        if (device?.isConnected() == true) return device
        deviceCache.remove(serial)
        return null
    }

    /**
     * Gets instances of [AndroidDevice] for the devices that are currently connected to adb
     *
     * @return [AndroidDevice] list
     */
    fun getDevices(): List<AndroidDevice> {
        return execute("devices")
            .readLines().drop(1)
            .mapNotNull { Regex("(^.+?)\\s+(.+$)").matchEntire(it)?.destructured }
            .mapNotNull { (serial, status) ->
                when {
                    status.equals("offline", true) -> null
                    deviceCache.containsKey(serial) -> deviceCache[serial]
                    else -> {
                        try {
                            AndroidDevice(serial).also { deviceCache[serial] = it }
                        } catch (e: Exception) {
                            println("Could not initialize device: $serial, skipping")
                            e.printStackTrace()
                            null
                        }
                    }
                }
            }
    }

    /**
     * Runs an ADB command
     *
     * @param args command or arguments passed onto adb
     * @return [Process] instance of the command
     */
    fun execute(vararg args: String): Process {
        return try {
            ProcessBuilder("$binPath", *args).start()
        } catch (e: IOException) {
            if (e.message?.contains("Permission denied") == true
                && !System.getProperty("os.name").lowercase().contains("win")
            ) {
                ProcessBuilder("chmod", "+x", "$binPath").start()
                execute(*args)
            } else {
                throw e
            }
        }
    }

    /**
     * Connects to a remote device using given address
     *
     * @param onConnected Callback to when connection attempt is finished which takes
     * the [AndroidDevice], this may be null if connection failed
     */
    fun connect(address: String, onConnected: (AndroidDevice?) -> Unit = {}) {
        thread(name = "ADB Device Connect") {
            execute("connect", address).waitFor()
            onConnected(getDevice(address))
        }
    }

    private fun adbBinaryOk(): Boolean {
        // Bug in some adb versions (32.0.0) causes it to SEGV with empty args
        return execute("help").readText().startsWith("Android Debug Bridge version")
    }

    private fun downloadPlatformTools() {
        var os = System.getProperty("os.name").lowercase()
        os = when {
            os.contains("win") -> "windows"
            os.contains("mac") -> "darwin"
            os.contains("linux") -> "linux"
            else -> error("Unsupported OS: $os")
        }
        val platformToolsUrl =
            URL("https://dl.google.com/android/repository/platform-tools-latest-$os.zip")
        val outputFile = CVAutoAndroid.HOME_DIR.resolve("platform-tools.zip")
        outputFile.parent.createDirectories()
        val readChannel = Channels.newChannel(platformToolsUrl.openStream())

        if (outputFile.notExists()) {
            FileChannel.open(outputFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                .transferFrom(readChannel, 0, Long.MAX_VALUE)
        }

        val buffer = ByteArray(1024)

        ZipInputStream(outputFile.inputStream()).use { zis ->
            generateSequence { zis.nextEntry }
                .filterNot { it.isDirectory }
                .forEach { ze ->
                    val path = CVAutoAndroid.HOME_DIR.resolve(ze.name)
                    path.parent.createDirectories()
                    path.outputStream().use { os ->
                        generateSequence { zis.read(buffer) }.takeWhile { it > 0 }.forEach {
                            os.write(buffer, 0, it)
                        }
                    }
                }
        }
    }
}
