package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateIntensityImage
import boofcv.struct.image.GrayF32
import kotlin.math.sqrt

/**
 * Implementation of matching algorithm equivalent to OpenCVs CV_TM_CCOEFF_NORMED template matcher
 */
class TemplateCosEffNorm : EvaluatorMethodAdapter() {
    private var templateArea = 0
    private var templateMean = 0.0

    override fun initialize(owner: TemplateIntensityImage<GrayF32>) {
        super.initialize(owner)
        templateArea = template.width * template.height

        for (y in 0 until template.height) {
            var templateIndex: Int = template.startIndex + y * template.stride
            for (x in 0 until template.width) {
                templateMean += template.data[templateIndex++]
            }
        }

        templateMean /= templateArea
    }

    override fun evaluate(tl_x: Int, tl_y: Int): Float {
        var imageMean = 0.0

        for (y in 0 until template.height) {
            var imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x
            for (x in 0 until template.width) {
                imageMean += image.data[imageIndex++]
            }
        }

        imageMean /= templateArea

        var total = 0.0
        var tTotal = 0.0
        var iTotal = 0.0

        for (y in 0 until template.height) {
            val imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x
            val templateIndex = template.startIndex + y * template.stride
            for (x in 0 until template.width) {
                // ArraysOutOfBoundsException may occur in a non-thread safe environment
                // Make sure image and template stay consistent if this is shared among threads
                val iData = image.data[imageIndex] - imageMean
                val tData = template.data[templateIndex] - templateMean
                total += tData * iData
                iTotal += iData * iData
                tTotal += tData * tData
            }
        }

        return (-total / sqrt(iTotal * tTotal)).toFloat()
    }
}

inline fun <T> ITemplateMatcher.useCosEffNorm(block: () -> T): T {
    val oldEval = settings.evaluator
    settings.evaluator = { TemplateCosEffNorm() }
    val value = block()
    settings.evaluator = oldEval
    return value
}