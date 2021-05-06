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
) : Region<Desktop, DesktopRegion>(x, y, width, height, device, screen) {
    companion object {
        private val robot by lazy { Robot() }
    }

    override fun capture(): BufferedImage {
        val capture = robot.createScreenCapture(this).getSubimage(x, y, width, height)
        if (device.screens.contains(this)) {
            _lastScreenCapture = System.currentTimeMillis() to capture
        }
        return capture
    }

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
        screen: Int
    ): DesktopRegion {
        return DesktopRegion(x, y, width, height, device, screen)
    }
}