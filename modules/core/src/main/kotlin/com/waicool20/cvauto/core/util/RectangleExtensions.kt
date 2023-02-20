/*
 * The MIT License (MIT)
 *
 * Copyright (c) waicool20
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.waicool20.cvauto.core.util

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
