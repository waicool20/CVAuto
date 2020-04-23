package com.waicool20.cvauto.util.wrapper

import boofcv.factory.geo.ConfigHomography
import boofcv.factory.geo.ConfigRansac
import boofcv.factory.geo.FactoryMultiViewRobust
import boofcv.struct.geo.AssociatedPair
import georegression.struct.homography.Homography2D_F64
import org.ddogleg.fitting.modelset.ransac.Ransac

/**
 * Wrapper for [FactoryMultiViewRobust]
 */
object KFactoryMultiViewRobust {
    /**
     * @see [FactoryMultiViewRobust.homographyRansac]
     */
    fun homographyRansac(
        ransac: ConfigRansac,
        homography: ConfigHomography? = null
    ): Ransac<Homography2D_F64, AssociatedPair> {
        return FactoryMultiViewRobust.homographyRansac(homography, ransac)
    }
}