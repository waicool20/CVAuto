package com.waicool20.cvauto.util

import boofcv.abst.distort.FDistort
import boofcv.alg.color.ColorHsv
import boofcv.alg.filter.blur.GBlurImageOps
import boofcv.alg.misc.PixelMath
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayF32
import boofcv.struct.image.Planar
import java.awt.image.BufferedImage
import kotlin.math.PI
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
 * Convenience extension to convert from [Planar] to [BufferedImage]
 *
 * @return New [BufferedImage] with equivalent image representation
 */
fun Planar<GrayF32>.asBufferedImage(): BufferedImage = ConvertBufferedImage.convertTo_F32(this, null, true)

/**
 * Convenience extension to convert from [BufferedImage] to [Planar]
 *
 * @return New [Planar] with equivalent image representation
 */
fun BufferedImage.asPlanar(): Planar<GrayF32> =
    ConvertBufferedImage.convertFromPlanar(this, null, true, GrayF32::class.java)

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

/**
 * Convenience extension to convert rgb [Planar] to hsv
 */
fun Planar<GrayF32>.asHsv(): Planar<GrayF32> {
    return createSameShape().apply { ColorHsv.rgbToHsv(this@asHsv, this) }
}

/**
 * Convenience extension to convert hsv [Planar] to rgb
 */
fun Planar<GrayF32>.asRgb(): Planar<GrayF32> {
    return createSameShape().apply { ColorHsv.hsvToRgb(this@asRgb, this) }
}

//<editor-fold desc="hsvFilter">

/**
 * Filters pixels in the hsv color space, this [Planar] must already be in the hsv color space
 *
 * @param hueRange Range of accepted hue values (0 - 360)
 * @param satRange Range of accepted saturation values (0 - 100)
 * @param satRange Range of accepted lightness values (0 - 255)
 */
fun Planar<GrayF32>.hsvFilter(hueRange: IntRange? = null, satRange: IntRange? = null, valRange: IntRange? = null) {
    hsvFilter(
        hueRange?.toDoubleRange(),
        satRange?.toDoubleRange(),
        valRange?.toDoubleRange()
    )
}

/**
 * Filters pixels in the hsv color space, this [Planar] must already be in the hsv color space
 *
 * @param hueRange Range of accepted hue values (0 - 360)
 * @param satRange Range of accepted saturation values (0 - 100)
 * @param satRange Range of accepted lightness values (0 - 255)
 */
fun Planar<GrayF32>.hsvFilter(
    hueRange: ClosedFloatingPointRange<Double>? = null,
    satRange: ClosedFloatingPointRange<Double>? = null,
    valRange: ClosedFloatingPointRange<Double>? = null
) {
    if (hueRange == null && satRange == null && valRange == null) {
        error("At least one critia is required for filter!")
    }

    val hRange = hueRange ?: 0.0..360.0
    val sRange = satRange ?: 0.0..100.0
    val vRange = valRange ?: 0.0..255.0

    require(hRange.start >= 0 && hRange.endInclusive <= 360) { "Hue must be in 0 - 360" }
    require(sRange.start >= 0 && sRange.endInclusive <= 100) { "Saturation must be in 0 - 100" }
    require(vRange.start >= 0 && vRange.endInclusive <= 255) { "Value must be in 0 - 255" }

    val hueBand = getBand(0)
    val satBand = getBand(1)
    val valBand = getBand(2)

    for (y in 0 until height) {
        var index = startIndex + y * stride
        for (x in 0 until width) {
            val hueValue = hueBand.data[index] * 180 / PI
            val satValue = satBand.data[index] * 100
            val valValue = valBand.data[index]

            if (!(hueValue in hRange && satValue in sRange && valValue in vRange)) {
                valBand.data[index] = 0f
            }
            index++
        }
    }
}

/**
 * Filters pixels in the hsv color space, this [Planar] must already be in the hsv color space
 *
 * @param hueRange Range of accepted hue values (0 - 360)
 * @param satRange Range of accepted saturation values (0 - 100)
 * @param satRange Range of accepted lightness values (0 - 255)
 */
fun Planar<GrayF32>.hsvFilter(hueRange: Array<IntRange>? = null, satRange: Array<IntRange>? = null, valRange: Array<IntRange>? = null) {
    hsvFilter(
        hueRange?.map { it.toDoubleRange() }?.toTypedArray(),
        satRange?.map { it.toDoubleRange() }?.toTypedArray(),
        valRange?.map { it.toDoubleRange() }?.toTypedArray()
    )
}


/**
 * Filters pixels in the hsv color space, this [Planar] must already be in the hsv color space
 *
 * @param hueRange Range of accepted hue values (0 - 360)
 * @param satRange Range of accepted saturation values (0 - 100)
 * @param satRange Range of accepted lightness values (0 - 255)
 */
fun Planar<GrayF32>.hsvFilter(
    hueRange: Array<ClosedFloatingPointRange<Double>>? = null,
    satRange: Array<ClosedFloatingPointRange<Double>>? = null,
    valRange: Array<ClosedFloatingPointRange<Double>>? = null
) {
    if (hueRange == null && satRange == null && valRange == null) {
        error("At least one critia is required for filter!")
    }

    val hRanges = hueRange ?: arrayOf(0.0..360.0)
    val sRanges = satRange ?: arrayOf(0.0..100.0)
    val vRanges = valRange ?: arrayOf(0.0..255.0)

    require(hRanges.all { it.start >= 0 && it.endInclusive <= 360 }) { "Hue must be in 0 - 360" }
    require(sRanges.all { it.start >= 0 && it.endInclusive <= 360 }) { "Saturation must be in 0 - 100" }
    require(vRanges.all { it.start >= 0 && it.endInclusive <= 360 }) { "Value must be in 0 - 255" }

    val hueBand = getBand(0)
    val satBand = getBand(1)
    val valBand = getBand(2)

    for (y in 0 until height) {
        var index = startIndex + y * stride
        for (x in 0 until width) {
            val hueValue = hueBand.data[index] * 180 / PI
            val satValue = satBand.data[index] * 100
            val valValue = valBand.data[index]

            if (!(hRanges.any { hueValue in it } && sRanges.any { satValue in it } && vRanges.any { valValue in it })) {
                valBand.data[index] = 0f
            }
            index++
        }
    }
}

//</editor-fold>

//<editor-fold desc="Pixel Math">

operator fun GrayF32.plus(other: GrayF32): GrayF32 {
    val result = createSameShape()
    PixelMath.add(this, other, result)
    return result
}

operator fun GrayF32.plus(value: Float): GrayF32 {
    val result = createSameShape()
    PixelMath.plus(this, value, result)
    return result
}

operator fun GrayF32.plusAssign(other: GrayF32) {
    PixelMath.add(this, other, this)
}

operator fun GrayF32.plusAssign(value: Float) {
    PixelMath.plus(this, value, this)
}

operator fun GrayF32.minus(other: GrayF32): GrayF32 {
    val result = createSameShape()
    PixelMath.subtract(this, other, result)
    return result
}

operator fun GrayF32.minus(value: Float): GrayF32 {
    val result = createSameShape()
    PixelMath.minus(this, value, result)
    return result
}

operator fun GrayF32.minusAssign(other: GrayF32) {
    PixelMath.subtract(this, other, this)
}

operator fun GrayF32.minusAssign(value: Float) {
    PixelMath.minus(this, value, this)
}

operator fun GrayF32.times(other: GrayF32): GrayF32 {
    val result = createSameShape()
    PixelMath.multiply(this, other, result)
    return result
}

operator fun GrayF32.times(value: Float): GrayF32 {
    val result = createSameShape()
    PixelMath.multiply(this, value, result)
    return result
}

operator fun GrayF32.timesAssign(other: GrayF32) {
    PixelMath.multiply(this, other, this)
}

operator fun GrayF32.timesAssign(value: Float) {
    PixelMath.multiply(this, value, this)
}

operator fun GrayF32.div(other: GrayF32): GrayF32 {
    val result = createSameShape()
    PixelMath.divide(this, other, result)
    return result
}

operator fun GrayF32.div(value: Float): GrayF32 {
    val result = createSameShape()
    PixelMath.divide(this, value, result)
    return result
}

operator fun GrayF32.divAssign(other: GrayF32) {
    PixelMath.divide(this, other, this)
}

operator fun GrayF32.divAssign(value: Float) {
    PixelMath.divide(this, value, this)
}

operator fun GrayF32.unaryMinus() {
    PixelMath.negative(this, this)
}

//</editor-fold>

private inline fun IntRange.toDoubleRange() = first.toDouble()..last.toDouble()