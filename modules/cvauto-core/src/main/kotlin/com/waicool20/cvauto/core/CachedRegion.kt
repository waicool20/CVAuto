package com.waicool20.cvauto.core

import com.waicool20.cvauto.util.matching.ITemplateMatcher
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Cached region, this variant of region will capture the region on creation
 * and cache it, [capture] will always return this cached image.
 * Sub-regions created from this region will also use this cached image for their operations
 */
class CachedRegion<T : IDevice> private constructor(
    val region: Region<T>, parentCachedImage: BufferedImage? = null
) : Region<T>(
    region.x,
    region.y,
    region.width,
    region.height,
    region.device,
    region.screen
) {
    constructor(region: Region<T>) : this(region, null)

    private val cachedImage = parentCachedImage ?: region.capture()
    override fun capture(): BufferedImage = cachedImage

    override fun mapRectangleToRegion(rect: Rectangle) =
        CachedRegion(
            region.mapRectangleToRegion(rect),
            cachedImage.getSubimage(rect.x, rect.y, rect.width, rect.height)
        )

    override fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<T> =
        region.mapFindResultToRegion(result)

    override fun click(random: Boolean) = region.click()

    override fun type(text: String) = region.type(text)

    /**
     * Copy constructor, the new cached region will be the snapshot at the time of copy
     */
    override fun copy(
        x: Pixels,
        y: Pixels,
        width: Pixels,
        height: Pixels,
        device: T,
        screen: Int
    ): CachedRegion<T> {
        return region.copy(x, y, width, height, device, screen).asCachedRegion()
    }
}

fun <T : IDevice> Region<T>.asCachedRegion(): CachedRegion<T> = CachedRegion(this)