package com.waicool20.cvauto.util

import boofcv.abst.feature.detdesc.DetectDescribePoint
import boofcv.alg.descriptor.UtilFeature
import boofcv.struct.feature.BrightFeature
import boofcv.struct.image.ImageGray
import com.waicool20.cvauto.util.wrapper.ImageDescriptors
import georegression.struct.point.Point2D_F64

/**
 * Computes the point and descriptors of the given image
 *
 * @return [ImageDescriptors] containing list of points and descriptors
 */
fun <T : ImageGray<T>> DetectDescribePoint<T, BrightFeature>.describeImage(image: T): ImageDescriptors {
    val points = mutableListOf<Point2D_F64>()
    val descriptors = UtilFeature.createQueue(this, 100)
    detect(image)
    descriptors.reset()
    for (i in 0 until numberOfFeatures) {
        points.add(getLocation(i).copy())
        descriptors.grow().setTo(getDescription(i))
    }
    return ImageDescriptors(points, descriptors)
}