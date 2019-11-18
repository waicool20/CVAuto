package com.waicool20.cvauto.util

import boofcv.abst.distort.FDistort
import boofcv.alg.filter.blur.GBlurImageOps
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayF32
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * Convenience extension to convert from [GrayF32] to [BufferedImage]
 *
 * @return New [BufferedImage] with equivalent image representation
 */
fun GrayF32.asBufferedImage(): BufferedImage = ConvertBufferedImage.convertTo(this, null, true)

/**
 * Convenience extension to convert from [BufferedImage] to [GrayF32]
 *
 * @return New [GrayF32] with equivalent image representation
 */
fun BufferedImage.asGrayF32(): GrayF32 = ConvertBufferedImage.convertFrom(this, null as GrayF32?)

/**
 * Scales the image size with [scaleFactor]
 *
 * @param scaleFactor Scaling factor
 * @return New scaled image
 *
 * @throws IllegalArgumentException if scale factor is negative
 */
fun GrayF32.scale(scaleFactor: Double): GrayF32 {
    if (scaleFactor == 1.0) return this
    require(scaleFactor > 0.0) { "scaleFactor must be larger than 0" }
    val output = createNew((width * scaleFactor).roundToInt(), (height * scaleFactor).roundToInt())
    FDistort(this, output).scaleExt().apply()
    return output
}

/**
 * Blurs the image with gaussian distribution
 *
 * @param radius Gaussian blur radius
 * @return New blurred image
 */
fun GrayF32.blurred(radius: Int): GrayF32 {
    if (radius == 0) return this
    require(radius > 0) { "radius must be a non-negative number" }
    return GBlurImageOps.gaussian(this, null, -1.0, radius, null)
}