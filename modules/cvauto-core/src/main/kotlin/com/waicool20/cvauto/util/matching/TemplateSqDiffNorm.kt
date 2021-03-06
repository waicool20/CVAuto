package com.waicool20.cvauto.util.matching

import kotlin.math.sqrt

/**
 * Implementation of matching algorithm equivalent to OpenCVs CV_TM_SQDIFF_NORMED template matcher
 */
class TemplateSqDiffNorm : EvaluatorMethodAdapter() {
    override fun evaluate(tl_x: Int, tl_y: Int): Float {
        var total = 0.0
        var iTotal = 0.0
        var tTotal = 0.0

        loop@ for (y in 0 until template.height) {
            var imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x
            var templateIndex = template.startIndex + y * template.stride
            for (x in 0 until template.width) {
                // ArraysOutOfBoundsException may occur in a non-thread safe environment
                // Make sure image and template stay consistent if this is shared among threads
                val iData = image.data[imageIndex++]
                val tData = template.data[templateIndex++]
                val error = tData - iData
                total += error * error
                iTotal += iData * iData
                tTotal += tData * tData
            }
        }

        return (total / sqrt(iTotal * tTotal)).toFloat() - 1
    }
}

inline fun <T> ITemplateMatcher.useSqDiffNorm(block: () -> T): T {
    val oldEval = settings.evaluator
    settings.evaluator = { TemplateSqDiffNorm() }
    val value = block()
    settings.evaluator = oldEval
    return value
}