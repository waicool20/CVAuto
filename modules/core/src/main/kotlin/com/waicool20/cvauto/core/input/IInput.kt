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
