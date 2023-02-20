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

package com.waicool20.cvauto.core

import java.awt.image.BufferedImage

/**
 * Cached region, this variant of region will capture the region on creation
 * and cache it, [capture] will always return this cached image.
 * Sub-regions created from this region will also use this cached image for their operations
 */
class CachedRegion<T : IDevice<T, R>, R : Region<T, R>> private constructor(
    val region: Region<T, R>, parentCachedImage: BufferedImage? = null
) : Region<CachedDevice<T, R>, CachedRegion<T, R>>(
    region.x,
    region.y,
    region.width,
    region.height,
    CachedDevice(region.device),
    region.screen
) {
    constructor(region: Region<T, R>) : this(region, null)

    private val cachedImage = parentCachedImage ?: region.device.screens[screen].capture()
    override fun capture(): BufferedImage = cachedImage.getSubimage(x, y, width, height)

    override fun click(random: Boolean) = region.click()

    override fun type(text: String) = region.type(text)

    /**
     * Copy constructor, the new cached region will be the snapshot at the time of copy
     */
    @Suppress("UNCHECKED_CAST")
    override fun copy(
        x: Pixels,
        y: Pixels,
        width: Pixels,
        height: Pixels,
        device: CachedDevice<T, R>,
        screen: Int
    ): CachedRegion<T, R> {
        return CachedRegion(
            region.copy(x, y, width, height, device.device as T, screen),
            cachedImage
        )
    }
}
