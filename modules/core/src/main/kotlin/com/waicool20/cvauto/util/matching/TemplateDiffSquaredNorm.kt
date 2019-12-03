package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateIntensityImage
import boofcv.struct.image.GrayF32
import kotlin.math.sqrt

/**
 * Implementation of matching algorithm equivalent to OpenCVs CV_TM_SQDIFF_NORMED template matcher
 */
class TemplateDiffSquaredNorm : TemplateIntensityImage.EvaluatorMethod<GrayF32> {
    companion object {
        /*
        Reflection needed since these fields are package protected in the library,
        Caching reflected fields for faster access in the future
         */
        private val tiiFields by lazy {
            TemplateIntensityImage::class.java
        }
        private val templateField by lazy {
            tiiFields.getDeclaredField("template").apply { isAccessible = true }
        }
        private val imageField by lazy {
            tiiFields.getDeclaredField("image").apply { isAccessible = true }
        }
    }

    private lateinit var template: GrayF32
    private lateinit var image: GrayF32

    override fun initialize(owner: TemplateIntensityImage<GrayF32>) {
        template = templateField.get(owner) as GrayF32
        image = imageField.get(owner) as GrayF32
    }

    override fun evaluate(tl_x: Int, tl_y: Int): Float {
        var total = 0f
        var iTotal = 0f
        var tTotal = 0f

        loop@for (y in 0 until template.height) {
            var imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x
            var templateIndex = template.startIndex + y * template.stride
            for (x in 0 until template.width) {
                // AraysOutOfBoundsException may occur in a non-thread safe environment
                // Make sure image and template stay consistent if this is shared among threads
                val iData = image.data[imageIndex++]
                val tData = template.data[templateIndex++]
                val error = tData - iData
                total += error * error
                iTotal += iData * iData
                tTotal += tData * tData
            }
        }

        return -total / sqrt(iTotal * tTotal)
    }

    override fun evaluateMask(tl_x: Int, tl_y: Int): Float {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }


    override fun isBorderProcessed() = false
    override fun isMaximize() = false
}