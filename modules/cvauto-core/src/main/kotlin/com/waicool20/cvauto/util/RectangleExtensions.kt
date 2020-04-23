package com.waicool20.cvauto.util

import java.awt.Rectangle

/**
 * Area of the rectangle
 */
val Rectangle.area get() = width * height

/**
 * Convenience value, equivalent to [Rectangle.x]
 */
val Rectangle.x1 get() = x

/**
 * Convenience value, equivalent to [Rectangle.x] + [Rectangle.width]
 */
val Rectangle.x2 get() = x + width

/**
 * Convenience value, equivalent to [Rectangle.y]
 */
val Rectangle.y1 get() = y

/**
 * Convenience value, equivalent to [Rectangle.y] + [Rectangle.height]
 */
val Rectangle.y2 get() = y + height

/**
 * Crops and returns a new rect that fits in the other rect
 */
fun Rectangle.cropIntoRect(other: Rectangle): Rectangle {
    return createIntersection(other).bounds
}