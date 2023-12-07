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

import com.waicool20.cvauto.core.Capture
import com.waicool20.cvauto.core.IDisplay
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.util.deepClone
import net.jpountz.lz4.LZ4FrameInputStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.swscale
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.EOFException
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class AndroidDisplay(
    override val device: AndroidDevice,
    override val width: Pixels,
    override val height: Pixels,
    override val index: Int
) : IDisplay<AndroidDevice, AndroidDisplay, AndroidRegion> {
    private val logger = LoggerFactory.getLogger(AndroidDisplay::class.java)

    data class Stats(var captureRequests: Long = 0, var cacheHits: Long = 0)
    companion object {
        private val executorLock = ReentrantLock()
        private var executor = Executors.newSingleThreadExecutor()

        private var _stats = Stats()

        val stats get() = _stats.copy()
    }


    override val region = AndroidRegion(0, 0, width, height, device, this)
    override var lastCapture: Capture =
        Capture(0, BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR))
        private set


    override fun capture(): Capture {
        _stats.captureRequests++
        if (System.currentTimeMillis() - lastCapture.timestamp <= 33) {
            _stats.cacheHits++
            return lastCapture.copy(img = lastCapture.img.deepClone());
        }
        return executorLock.withLock {
            val future = executor.submit<Capture> {
                return@submit when (device.captureMethod) {
                    AndroidDevice.CaptureMethod.SCREENCAP -> doNormalCapture()
                    AndroidDevice.CaptureMethod.SCRCPY -> doScrcpyCapture()
                }
            }
            try {
                future.get(10_000, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Seems to work better if we just retry instead of re-throwing
                executor.shutdownNow()
                executor = Executors.newSingleThreadExecutor()
                return capture()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }
    }

    private fun doNormalCapture(): Capture {
        val throwables = mutableListOf<Throwable>()
        for (i in 0 until 3) {
            val process: Process
            val inputStream = when (device.compressionMode) {
                AndroidDevice.CompressionMode.NONE -> {
                    process = device.execute("screencap")
                    process.inputStream.buffered(1024 * 1024)
                }
                AndroidDevice.CompressionMode.GZIP -> {
                    process = device.execute("screencap | toybox gzip -1")
                    GZIPInputStream(process.inputStream).buffered(1024 * 1024)
                }
                AndroidDevice.CompressionMode.LZ4 -> {
                    // LZ4 mode runs slower if buffered, maybe it has internal buffer already
                    process = device.execute("screencap | /data/local/tmp/lz4 -c -1")
                    LZ4FrameInputStream(process.inputStream)
                }
            }
            try {
                val width = inputStream.read() or (inputStream.read() shl 8) or
                    (inputStream.read() shl 16) or (inputStream.read() shl 24)
                val height = inputStream.read() or (inputStream.read() shl 8) or
                    (inputStream.read() shl 16) or (inputStream.read() shl 24)
                if (width < 0 || height < 0) continue
                if (device.properties.androidVersion.split(".")[0].toInt() >= 8) {
                    // ADB screencap on android versions 8 and above have 8 extra bytes instead of 4
                    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/pie-release/cmds/screencap/screencap.cpp#247
                    inputStream.skip(8)
                } else {
                    // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/nougat-release/cmds/screencap/screencap.cpp#191
                    inputStream.skip(4)
                }
                val img = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
                val buffer = (img.raster.dataBuffer as DataBufferByte).data
                for (n in buffer.indices step 3) {
                    // Data comes in RGBA byte format, repack into BGR
                    buffer[n + 2] = inputStream.read().toByte()
                    buffer[n + 1] = inputStream.read().toByte()
                    buffer[n + 0] = inputStream.read().toByte()
                    inputStream.skip(1)
                }
                val capture = Capture(System.currentTimeMillis(), img)
                lastCapture = capture
                return capture
            } catch (t: Throwable) {
                device.assertConnected()
                throwables.add(t)
            }
        }
        throw Region.CaptureIOException(throwables.reduceOrNull { acc, _ -> Exception(acc) }
            ?: Exception("Unknown cause"))
    }

    private var scrcpyThread: Thread? = null
    private val bgrFrame = avutil.av_frame_alloc().apply {
        format(avutil.AV_PIX_FMT_BGR24)
        width(width)
        height(height)
        avutil.av_image_alloc(data(), linesize(), width(), height(), format(), 1)
    }
    private var lastFrameTime = 0L
    private var latch: CountDownLatch? = null

    private fun doScrcpyCapture(): Capture {
        if (scrcpyThread == null) {
            scrcpyThread = thread(true, name = "Scrcpy Capture Thread") {
                val metaDataBuffer = ByteBuffer.allocate(12)

                val ch = Channels.newChannel(device.scrcpy.video.getInputStream())
                ch.readFully(metaDataBuffer)

                metaDataBuffer.getInt() // codecId
                val videoWidth = metaDataBuffer.getInt()
                val videoHeight = metaDataBuffer.getInt()

                val avCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
                val avCodecContext = avcodec.avcodec_alloc_context3(avCodec).apply {
                    flags(flags() or avcodec.AV_CODEC_FLAG_LOW_DELAY)
                    width(videoWidth)
                    height(videoHeight)
                    pix_fmt(avutil.AV_PIX_FMT_YUV420P)
                }

                avcodec.avcodec_open2(avCodecContext, avCodec, AVDictionary())

                val packet = avcodec.av_packet_alloc()
                val yuvFrame = avutil.av_frame_alloc()

                val convertContext = swscale.sws_getContext(
                    videoWidth,
                    videoHeight,
                    avCodecContext.pix_fmt(),
                    videoWidth,
                    videoHeight,
                    avutil.AV_PIX_FMT_BGR24,
                    swscale.SWS_BICUBIC,
                    null, null, doubleArrayOf()
                )

                while (!Thread.interrupted()) {
                    ch.readFully(metaDataBuffer)

                    val ptsAndFlags = metaDataBuffer.getLong().toULong()
                    val packetSize = metaDataBuffer.getInt()

                    val isConfig = ptsAndFlags and (1UL shl 63) != 0UL
                    val isKeyFrame = ptsAndFlags and (1UL shl 62) != 0UL
                    val pts = ptsAndFlags and (3UL shl 62).inv()

                    avcodec.av_new_packet(packet, packetSize)

                    val packetBuffer = packet.data().capacity(packetSize.toLong()).asByteBuffer()
                    ch.readFully(packetBuffer)

                    packet.pts(if (isConfig) avutil.AV_NOPTS_VALUE else pts.toLong())

                    if (isKeyFrame) {
                        packet.flags(packet.flags() or avcodec.AV_PKT_FLAG_KEY)
                    }

                    avcodec.avcodec_send_packet(avCodecContext, packet)
                    avcodec.avcodec_receive_frame(avCodecContext, yuvFrame)

                    if (!isConfig) {
                        swscale.sws_scale(
                            convertContext,
                            yuvFrame.data(),
                            yuvFrame.linesize(),
                            0,
                            yuvFrame.height(),
                            bgrFrame.data(),
                            bgrFrame.linesize()
                        )
                        lastFrameTime = System.currentTimeMillis()
                        latch?.countDown()
                    }
                }

                // Free data when done
                avcodec.av_packet_free(packet)
                avutil.av_frame_free(yuvFrame)
                swscale.sws_freeContext(convertContext)
                avcodec.avcodec_close(avCodecContext)
                avcodec.avcodec_free_context(avCodecContext)
            }
        }

        if (scrcpyThread?.isAlive == false || device.scrcpy.isClosed) {
            device.resetScrcpy()
            scrcpyThread?.interrupt()
            scrcpyThread = null
            return doScrcpyCapture()
        }

        return try {
            val img = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            latch = CountDownLatch(1)
            latch?.await(1000, TimeUnit.MILLISECONDS)
            bgrFrame.data(0).get((img.raster.dataBuffer as DataBufferByte).data)
            Capture(lastFrameTime, img)
        } catch (e: SocketException) {
            device.resetScrcpy()
            doScrcpyCapture()
        } catch (t: Throwable) {
            device.assertConnected()
            throw Region.CaptureIOException(t)
        }
    }

    private fun ReadableByteChannel.readFully(buffer: ByteBuffer) {
        buffer.rewind()
        while (buffer.remaining() > 0) {
            if (read(buffer) < 0) throw EOFException()
        }
        buffer.rewind()
    }
}
