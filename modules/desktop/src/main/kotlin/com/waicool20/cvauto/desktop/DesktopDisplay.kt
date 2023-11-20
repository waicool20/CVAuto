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

package com.waicool20.cvauto.desktop

import com.waicool20.cvauto.core.Capture
import com.waicool20.cvauto.core.IDisplay
import com.waicool20.cvauto.core.Pixels
import java.awt.GraphicsDevice
import java.awt.image.BufferedImage

class DesktopDisplay(
    override val device: Desktop,
    override val index: Int,
    private val graphicsDevice: GraphicsDevice
) : IDisplay<Desktop, DesktopDisplay, DesktopRegion> {
    override
    val region = DesktopRegion(0, 0, width, height, device, this)
    override val width: Pixels
        get() = graphicsDevice.defaultConfiguration.bounds.width
    override val height: Pixels
        get() = graphicsDevice.defaultConfiguration.bounds.height
    override var lastCapture: Capture =
        Capture(0, BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR))
        private set

    override fun capture(): Capture {
        lastCapture = Capture(
            System.currentTimeMillis(),
            device.robot.createScreenCapture(graphicsDevice.defaultConfiguration.bounds)
        )
        return lastCapture
    }
}
