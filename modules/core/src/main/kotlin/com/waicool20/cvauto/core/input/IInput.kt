package com.waicool20.cvauto.core.input

import com.waicool20.cvauto.core.IDevice

/**
 * Input interfaces to a [IDevice], they can be used to control the device.
 */
interface IInput {
    /**
     * Mouse input for the [IDevice], null if it is not supported
     */
    val mouse: IMouse?

    /**
     * Keyboard input for the [IDevice], null if it is not supported
     */
    val keyboard: IKeyboard?

    /**
     * Touch input for the [IDevice], null if it is not supported
     */
    val touchInterface: ITouchInterface?

    /**
     * Helper method for checking mouse support
     *
     * @return true if mouse is supported
     */
    fun hasMouseSupport(): Boolean = mouse != null

    /**
     * Helper method for checking keyboard support
     *
     * @return true if keyboard is supported
     */
    fun hasKeyboardSupport(): Boolean = keyboard != null

    /**
     * Helper method for checking touch support
     *
     * @return true if touch is supported
     */
    fun hasTouchSupport(): Boolean = touchInterface != null
}