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

package com.waicool20.cvauto.android

import com.waicool20.cvauto.core.Capture
import com.waicool20.cvauto.core.Millis
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.input.ITouchInterface
import kotlin.math.roundToInt

class AndroidRegion(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    device: AndroidDevice,
    display: AndroidDisplay,
    frozenCapture: Capture? = null
) : Region<AndroidDevice, AndroidDisplay, AndroidRegion>(x, y, width, height, device, display, frozenCapture) {

    override fun click(random: Boolean) {
        if (random) {
            val r = randomPoint()
            device.input.touchInterface.tap(0, r.x, r.y)
        } else {
            device.input.touchInterface.tap(0, centerX.roundToInt(), centerY.roundToInt())
        }
    }

    override fun type(text: String) {
        device.input.keyboard.type(text)
    }

    /**
     * Swipes from this region to the other region
     * For more complex operations please use the devices respective [IInput]
     *
     * @param other Other region to swipe to
     * @param duration Duration of the swipe
     * @param random If true the source and destination location will be random points in their respective regions
     */
    fun swipeTo(other: AndroidRegion, duration: Millis = 1000, random: Boolean = true) {
        val swipe = if (random) {
            val src = randomPoint()
            val dest = other.randomPoint()
            ITouchInterface.Swipe(0, src.x, src.y, dest.x, dest.y)
        } else {
            ITouchInterface.Swipe(
                0,
                centerX.roundToInt(), centerY.roundToInt(),
                other.centerX.roundToInt(), other.centerY.roundToInt()
            )
        }
        device.input.touchInterface.swipe(swipe, duration)
    }

    /**
     * Sends a pinch gesture, centered at this regions center
     *
     * @param r1 Starting radius of the pinch
     * @param r2 End radius of the pinch
     * @param angle Pinch angle
     * @param duration Duration of the pinch
     */
    fun pinch(r1: Int, r2: Int, angle: kotlin.Double, duration: Millis = 1000) {
        device.input.touchInterface.pinch(
            centerX.roundToInt(), centerY.roundToInt(),
            r1, r2, angle, duration
        )
    }

    override fun copy(
        x: Pixels,
        y: Pixels,
        width: Pixels,
        height: Pixels,
        device: AndroidDevice,
        display: AndroidDisplay
    ): AndroidRegion {
        return AndroidRegion(x, y, width, height, device, display, this.frozenCapture)
    }
}
