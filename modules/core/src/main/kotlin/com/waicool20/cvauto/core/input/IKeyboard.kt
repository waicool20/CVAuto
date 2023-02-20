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

typealias CharactersPerSecond = Int

interface IKeyboard {
    interface Settings {
        var defaultTypingSpeed: CharactersPerSecond
        var typingSpeedVariance: Double
    }

    /**
     * Settings for this keyboard
     */
    val settings: Settings

    /**
     * List of the names of held keys eg. A, B, COMMA, ALT etc.
     */
    val heldKeys: List<String>

    /**
     * Send a key up event with given key
     *
     * @param keyName Name of key eg. A, B, COMMA, ALT etc.
     */
    fun keyUp(keyName: String)

    /**
     * Send a key down event with given key
     *
     * @param keyName Name of key eg. A, B, COMMA, ALT etc.
     */
    fun keyDown(keyName: String)

    /**
     * Checks if the key name is valid and supported
     *
     * @return true if the key can be typed into the device
     */
    fun checkSupport(keyName: String): Boolean

    /**
     * Types the given string
     *
     * @param string String to type
     */
    fun type(string: String, speed: CharactersPerSecond = settings.defaultTypingSpeed)
}
