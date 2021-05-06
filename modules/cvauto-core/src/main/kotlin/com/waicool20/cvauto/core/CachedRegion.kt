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

    private val cachedImage = parentCachedImage ?: region.capture()
    override fun capture(): BufferedImage = cachedImage

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