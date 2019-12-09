package com.waicool20.cvauto.util

import georegression.struct.homography.Homography2D_F64
import georegression.struct.point.Point2D_F64
import georegression.struct.point.Point2D_I32
import georegression.transform.homography.HomographyPointOps_F64

/**
 * Transform the given point based on this homography matrix
 *
 * @param x x coordinate
 * @param y y coordinate
 *
 * @return Transformed point
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Homography2D_F64.transformPoint(x: Int, y: Int) = transformPoint(x.toDouble(), y.toDouble())

/**
 * Transform the given point based on this homography matrix
 *
 * @param x x coordinate
 * @param y y coordinate
 *
 * @return Transformed point
 */
fun Homography2D_F64.transformPoint(x: Double, y: Double): Point2D_I32 {
    val result = Point2D_F64()
    HomographyPointOps_F64.transform(this, Point2D_F64(x, y), result)
    return Point2D_I32(result.x.toInt(), result.y.toInt())
}
