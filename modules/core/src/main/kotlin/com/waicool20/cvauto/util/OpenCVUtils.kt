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

package com.waicool20.cvauto.util

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.abs


/**
 * Converts a [BufferedImage] to [Mat], the [BufferedImage] type to [CvType] mapping is listed as below:
 *
 * - [BufferedImage.TYPE_BYTE_GRAY] -> [CvType.CV_8UC1]
 * - [BufferedImage.TYPE_3BYTE_BGR] -> [CvType.CV_8UC3]
 * - [BufferedImage.TYPE_4BYTE_ABGR] -> [CvType.CV_8UC4]
 *
 * Other types are not supported
 *
 * @see [toBufferedImage]
 */
fun BufferedImage.toMat(): Mat {
    val matType = when (type) {
        BufferedImage.TYPE_BYTE_GRAY -> CvType.CV_8UC1
        BufferedImage.TYPE_3BYTE_BGR -> CvType.CV_8UC3
        BufferedImage.TYPE_4BYTE_ABGR -> CvType.CV_8UC4
        else -> error("Unsupported type: $type")
    }
    val mat = Mat(height, width, matType)
    val data = (raster.dataBuffer as DataBufferByte).data
    if (raster.parent == null) {
        mat.put(intArrayOf(0, 0), data)
    } else {
        val skipY =
            abs(raster.sampleModelTranslateY) * raster.sampleModel.width * raster.sampleModel.numBands
        val skipX = abs(raster.sampleModelTranslateX) * raster.sampleModel.numBands
        for (y in 0 until height) {
            val offset = skipX + skipY + y * raster.sampleModel.width * raster.sampleModel.numBands
            mat.put(intArrayOf(y, 0), data, offset, width * raster.sampleModel.numBands)
        }
    }
    return mat
}

/**
 * Converts a [Mat] to [BufferedImage], the [CvType] to [BufferedImage] type mapping is listed as below:
 *
 * - [CvType.CV_8UC1] -> [BufferedImage.TYPE_BYTE_GRAY]
 * - [CvType.CV_8UC3] -> [BufferedImage.TYPE_3BYTE_BGR]
 * - [CvType.CV_8UC4] -> [BufferedImage.TYPE_4BYTE_ABGR]
 *
 * Other types are not supported
 *
 * @see [toMat]
 */
fun Mat.toBufferedImage(): BufferedImage {
    val type = when (val c = channels()) {
        1 -> BufferedImage.TYPE_BYTE_GRAY
        3 -> BufferedImage.TYPE_3BYTE_BGR
        4 -> BufferedImage.TYPE_4BYTE_ABGR
        else -> error("Unsupported channel number: $c")
    }
    val img = BufferedImage(width(), height(), type)
    val data = (img.raster.dataBuffer as DataBufferByte).data
    get(intArrayOf(0, 0), data)
    return img
}

/**
 * Remove the given channels from a [Mat]
 *
 * @param channelIndex An array of channel index, these will be removed from the [Mat]
 */
fun Mat.removeChannels(vararg channelIndex: Int): Mat {
    val dst = mutableListOf<Mat>()
    val tmp = mutableListOf<Mat>()
    Core.split(this, tmp)
    for ((i, mat) in tmp.withIndex()) {
        if (i !in channelIndex) dst.add(mat)
    }
    Core.merge(dst, this)
    return this
}

inline fun <reified T> Mat.at(row: Int, col: Int): Mat.Atable<T> {
    return at(T::class.java, row, col)
}