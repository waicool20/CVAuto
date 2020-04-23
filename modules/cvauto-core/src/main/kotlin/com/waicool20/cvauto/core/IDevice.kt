package com.waicool20.cvauto.core

import com.waicool20.cvauto.core.input.IInput

/**
 * Represents a Device with input and a set of screens
 */
interface IDevice {
    /**
     * The inputs that belong to this screen
     */
    val input: IInput

    /**
     * The list of screens that belong to this device, the first screen in the list
     * is the main screen.
     */
    val screens: List<Region<out IDevice>>
}
