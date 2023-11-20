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
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import java.awt.Robot
import java.awt.image.BufferedImage

/**
 * Represents a [Region] that is contained inside a [Desktop]
 */
class DesktopRegion(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    device: Desktop,
    display: DesktopDisplay,
    frozenCapture: Capture? = null
) : Region<Desktop, DesktopDisplay, DesktopRegion>(x, y, width, height, device, display, frozenCapture) {
    override fun click(random: Boolean) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun type(text: String) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun copy(
        x: Pixels,
        y: Pixels,
        width: Pixels,
        height: Pixels,
        device: Desktop,
        display: DesktopDisplay
    ): DesktopRegion {
        return DesktopRegion(x, y, width, height, device, display, this.frozenCapture)
    }
}
