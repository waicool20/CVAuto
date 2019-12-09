package com.waicool20.cvauto.util.wrapper

import boofcv.abst.feature.detect.interest.ConfigFastHessian
import boofcv.factory.geo.ConfigRansac

@Suppress("NOTHING_TO_INLINE")
object Config {
    inline fun FastHessian(
        detectThreshold: Double,
        extractRadius: Int,
        maxFeaturesPerScale: Int,
        initialSampleSize: Int,
        initialSize: Int,
        numberScalesPerOctave: Int,
        numberOfOctaves: Int
    ): ConfigFastHessian {
        return ConfigFastHessian(
            detectThreshold.toFloat(),
            extractRadius,
            maxFeaturesPerScale,
            initialSampleSize,
            initialSize,
            numberScalesPerOctave,
            numberOfOctaves
        )
    }

    inline fun Ransac(
        maxIterations: Int,
        inlierThreshold: Double
    ): ConfigRansac {
        return ConfigRansac(maxIterations, inlierThreshold)
    }
}