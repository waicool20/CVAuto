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
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * @see [ImageOpPipeline]
 */
fun BufferedImage.pipeline(): ImageOpPipeline {
    return ImageOpPipeline(this.toMat())
}

/**
 * @see [ImageOpPipeline]
 */
fun Mat.pipeline(): ImageOpPipeline {
    return ImageOpPipeline(this)
}

/**
 * An image operation pipeline runs various operations onto an image such as scaling, thresholding,
 * inverting, padding etc. The pipeline can be built with a builder style API
 * Operations are applied in the order they are called in the builder API
 */
class ImageOpPipeline(private val mat: Mat) {
    private val pipeline = mutableListOf<ImageOp>()

    /**
     * Scale an image by a factor of [factor]
     *
     * @param factor Scaling factor, must be in the range of (0.0, inf)
     * @return ImageOpPipeline builder
     */
    fun scale(factor: Double): ImageOpPipeline {
        require(factor > 0.0 && factor < Double.POSITIVE_INFINITY) {
            "Scaling factor must be in the range of (0.0, inf), given: $factor"
        }
        pipeline.add(ScaleImageOp(factor))
        return this
    }

    /**
     * Thresholding an image will convert the image to a black and white image
     *
     * @param threshold Any pixels with relative value above this number will be white else black,
     * must be in a range of [[0.0, 1.0]], defaults to 0.4
     * @return ImageOpPipeline builder
     */
    fun threshold(threshold: Double = 0.4): ImageOpPipeline {
        require(threshold in 0.0..1.0) {
            "Threshold must in the range of [0.0, 1.0], given: $threshold"
        }
        pipeline.add(ThresholdImageOp(threshold))
        return this
    }

    /**
     * Inverts the colors of an image
     *
     * @return ImageOpPipeline builder
     */
    fun invert(): ImageOpPipeline {
        pipeline.add(InvertImageOp())
        return this
    }

    /**
     * Pads an image, basically adding a border of n amount of pixels to each side with the given color
     *
     * @param top Amount of pixels to add to top of the image, must be a non-negative value
     * @param bottom Amount of pixels to add to bottom of the image, must be a non-negative value
     * @param left Amount of pixels to add to left of the image, must be a non-negative value
     * @param right Amount of pixels to add to right of the image, must be a non-negative value
     * @param color The drawn border will be this color, defaults to Black
     */
    fun pad(
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        color: Color = Color.BLACK
    ): ImageOpPipeline {
        require(top >= 0) { "top must be non-negative" }
        require(bottom >= 0) { "bottom must be non-negative" }
        require(left >= 0) { "left must be non-negative" }
        require(right >= 0) { "right must be non-negative" }
        pipeline.add(PadImageOp(top, bottom, left, right, color))
        return this
    }

    /**
     * Run the pipeline and return the final image as a [Mat]
     *
     * @return Processed [Mat]
     */
    fun toMat(): Mat {
        pipeline.forEach { it.process(mat) }
        return mat
    }

    /**
     * Run the pipeline and return the final image as a [BufferedImage]
     *
     * @return Processed [BufferedImage]
     */
    fun toBufferedImage(): BufferedImage {
        return toMat().toBufferedImage()
    }
}

private interface ImageOp {
    fun process(mat: Mat): Mat
}

private class ScaleImageOp(private val factor: Double) : ImageOp {
    override fun process(mat: Mat): Mat {
        Imgproc.resize(mat, mat, Size(0.0, 0.0), factor, factor)
        return mat
    }
}

private class ThresholdImageOp(private val threshold: Double) : ImageOp {
    override fun process(mat: Mat): Mat {
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        }
        Imgproc.threshold(mat, mat, threshold * 255.0, 255.0, Imgproc.THRESH_BINARY)
        return mat
    }
}

private class InvertImageOp : ImageOp {
    override fun process(mat: Mat): Mat {
        Core.bitwise_not(mat, mat)
        return mat
    }
}

private class PadImageOp(
    private val top: Int,
    private val bottom: Int,
    private val left: Int,
    private val right: Int,
    private val color: Color
) : ImageOp {
    override fun process(mat: Mat): Mat {
        Core.copyMakeBorder(
            mat, mat, top, bottom, left, right,
            Core.BORDER_CONSTANT,
            Scalar(color.blue.toDouble(), color.green.toDouble(), color.red.toDouble())
        )
        return mat
    }
}
