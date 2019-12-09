package com.waicool20.cvauto.util

import georegression.struct.point.Point2D_I32

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