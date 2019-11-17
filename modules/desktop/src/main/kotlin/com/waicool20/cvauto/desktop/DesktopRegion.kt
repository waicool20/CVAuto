package com.waicool20.cvauto.desktop

import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.util.matching.ITemplateMatcher
import java.awt.Rectangle
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
    screen: Int
) : Region<Desktop>(x, y, width, height, device, screen) {
    companion object {
        private val robot by lazy { Robot() }
    }

    override fun capture(): BufferedImage {
        return robot.createScreenCapture(this).getSubimage(x, y, width, height)
    }

    override fun mapRectangleToRegion(rect: Rectangle): Region<Desktop> {
        return DesktopRegion(rect.x + x, rect.y + y, rect.width, rect.height, device, screen)
    }

    override fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<Desktop> {
        return RegionFindResult(mapRectangleToRegion(result.rectangle), result.score)
    }

    override fun click(random: Boolean) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun type(text: String) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun clone(): Any {
        return DesktopRegion(x, y, width, height, device, screen)
    }
}