package com.waicool20.cvauto.util.wrapper

import boofcv.abst.feature.describe.ConfigSurfDescribe
import boofcv.abst.feature.detdesc.DetectDescribePoint
import boofcv.abst.feature.detect.interest.ConfigFastHessian
import boofcv.abst.feature.orientation.ConfigSlidingIntegral
import boofcv.factory.feature.detdesc.FactoryDetectDescribe
import boofcv.struct.feature.BrightFeature
import boofcv.struct.image.ImageGray

/**
 * Wrapper for [FactoryDetectDescribe]
 */
object KFactoryDetectDescribe {
    /**
     * @see [FactoryDetectDescribe.surfStable]
     */
    inline fun <reified T : ImageGray<T>> surfStable(
        configDetector: ConfigFastHessian? = null,
        configDescribe: ConfigSurfDescribe.Stability? = null,
        configOrientation: ConfigSlidingIntegral? = null
    ): DetectDescribePoint<T, BrightFeature> {
        return FactoryDetectDescribe.surfStable<T, T>(
            configDetector,
            configDescribe,
            configOrientation,
            T::class.java
        )
    }
}