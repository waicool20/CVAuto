@file:Suppress("NOTHING_TO_INLINE")
package com.waicool20.cvauto.util

import georegression.struct.homography.Homography2D_F64
import georegression.struct.point.Point2D_F64
import georegression.struct.point.Point2D_I32
import georegression.transform.homography.HomographyPointOps_F64
import java.awt.Rectangle

/**
 * Transform the given point based on this homography matrix
 *
 * @param x x coordinate
 * @param y y coordinate
 *
 * @return Transformed point
 */
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

/**
 * Transform the given rectangle based on this homography matrix
 *
 * @param rect Rectangle to transform
 *
 * @return Transformed rectangle
 */
inline fun Homography2D_F64.transformRect(rect: Rectangle) = transformRect(rect.x, rect.y, rect.width, rect.height)

/**
 * Transform the given rectangle based on this homography matrix
 *
 * @param x x coordinate
 * @param y y coordinate
 * @param width rectangle width
 * @param height rectangle height
 *
 * @return Transformed rectangle
 */
fun Homography2D_F64.transformRect(x: Int, y: Int, width: Int, height: Int): Rectangle {
    val (x1, y1) = transformPoint(x, y)
    val (x2, y2) = transformPoint(x + width, y + height)
    return Rectangle(x1, y1, x2 - x1, y2 - y1)
}