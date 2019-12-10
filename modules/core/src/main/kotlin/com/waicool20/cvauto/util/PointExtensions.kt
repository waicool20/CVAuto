package com.waicool20.cvauto.util

import georegression.struct.point.Point2D_I32
import java.awt.Point

/**
 * For destructuring point eg.
 * ```kotlin
 * val (x, y) = point
 * ```
 */
operator fun Point2D_I32.component1() = x

/**
 * For destructuring point eg.
 * ```kotlin
 * val (x, y) = point
 * ```
 */
operator fun Point2D_I32.component2() = y

/**
 * Converts from [Point2D_I32] to [Point]
 */
fun Point2D_I32.toAwtPoint() = Point(x, y)

/**
 * Converts from [Point] to [Point2D_I32]
 */
fun Point.toBoofPoint() = Point2D_I32(x, y)