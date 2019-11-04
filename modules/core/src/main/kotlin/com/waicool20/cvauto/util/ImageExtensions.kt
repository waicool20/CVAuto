package com.waicool20.cvauto.util

import boofcv.abst.distort.FDistort
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
 * @return New scaled image
 */
fun GrayF32.scale(scaleFactor: Double): GrayF32 {
    val output = createNew((width * scaleFactor).roundToInt(), (height * scaleFactor).roundToInt())
    FDistort(this, output).scaleExt().apply()
    return output
}