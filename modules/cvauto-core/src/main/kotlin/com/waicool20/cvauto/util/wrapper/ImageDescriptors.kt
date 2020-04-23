package com.waicool20.cvauto.util.wrapper

import boofcv.struct.feature.BrightFeature
import com.waicool20.cvauto.util.describeImage
import georegression.struct.point.Point2D_F64
import org.ddogleg.struct.FastQueue

/**
 * Wrapper class for the result of [describeImage]
 */
data class ImageDescriptors(val points: List<Point2D_F64>, val descriptorList: FastQueue<BrightFeature>)