package com.waicool20.cvauto.desktop

import com.waicool20.cvauto.core.IDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.input.IInput
import java.awt.GraphicsEnvironment

/**
 * Singleton [Desktop] device object, represents the local desktop environment
 */
object Desktop : IDevice {
    override val input: IInput get() = TODO()
    override val screens: List<DesktopRegion>
        get() = GraphicsEnvironment.getLocalGraphicsEnvironment().run {
            screenDevices.sortedByDescending { it == defaultScreenDevice }
                .mapIndexed { i, device ->
                    val r = device.defaultConfiguration.bounds
                    DesktopRegion(r.x, r.y, r.width, r.height, this@Desktop, i)
                }
        }
}