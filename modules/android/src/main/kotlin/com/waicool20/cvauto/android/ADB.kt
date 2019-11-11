package com.waicool20.cvauto.android

import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream

/**
 * Wrapper for android adb tool, accessing this object downloads android platform tools to %home%/cvauto/android
 * if it doesn't exist
 */
object ADB {
    private val dataDir = Paths.get(System.getProperty("user.home")).resolve(".cvauto/android")
    private val platformToolsDir = dataDir.resolve("platform-tools")
    private val adbBinaryPath = run {
        val os = System.getProperty("os.name").toLowerCase()
        val adbName = when {
            os.contains("win") -> "adb.exe"
            os.contains("mac") -> "adb"
            os.contains("linux") -> "adb"
            else -> error("Unsupported OS: $os")
        }
        platformToolsDir.resolve(adbName)
    }

    init {
        if (Files.notExists(adbBinaryPath)) downloadPlatformTools()
        if (!adbBinaryOk()) error("Problem initializing adb")
    }

    private val deviceCache = mutableMapOf<String, AndroidDevice>()

    /**
     * Gets instances of [AndroidDevice] for the devices that are currently connected to adb
     *
     * @return [AndroidDevice] list
     */
    fun getDevices(): List<AndroidDevice> {
        return execute("devices")
            .readLines()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { it.takeWhile { !it.isWhitespace() } }
            .map { deviceCache.getOrPut(it) { AndroidDevice(it) } }
    }

    /**
     * Runs an ADB command
     *
     * @param args command or arguments passed onto adb
     * @return [Process] instance of the command
     */
    fun execute(vararg args: String): Process {
        return try {
            ProcessBuilder("$adbBinaryPath", *args).start()
        } catch (e: IOException) {
            if (e.message?.contains("Permission denied") == true
                && !System.getProperty("os.name").toLowerCase().contains("win")
            ) {
                ProcessBuilder("chmod", "+x", "$adbBinaryPath").start()
                execute(*args)
            } else {
                throw e
            }
        }
    }

    private fun adbBinaryOk(): Boolean {
        return execute().readText().startsWith("Android Debug Bridge version")
    }

    private fun downloadPlatformTools() {
        var os = System.getProperty("os.name").toLowerCase()
        os = when {
            os.contains("win") -> "windows"
            os.contains("mac") -> "darwin"
            os.contains("linux") -> "linux"
            else -> error("Unsupported OS: $os")
        }
        val platformToolsUrl = URL("https://dl.google.com/android/repository/platform-tools-latest-$os.zip")
        val outputFile = dataDir.resolve("platform-tools.zip")
        Files.createDirectories(outputFile.parent)
        val readChannel = Channels.newChannel(platformToolsUrl.openStream())

        if (Files.notExists(outputFile)) {
            FileChannel.open(outputFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                .transferFrom(readChannel, 0, Long.MAX_VALUE)
        }

        val buffer = ByteArray(1024)

        ZipInputStream(Files.newInputStream(outputFile)).use { zis ->
            generateSequence { zis.nextEntry }
                .filterNot { it.isDirectory }
                .forEach { ze ->
                    val path = dataDir.resolve(ze.name)
                    Files.createDirectories(path.parent)
                    Files.newOutputStream(path).use { os ->
                        generateSequence { zis.read(buffer) }.takeWhile { it > 0 }.forEach {
                            os.write(buffer, 0, it)
                        }
                    }
                }
        }
    }
}