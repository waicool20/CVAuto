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

import com.waicool20.cvauto.core.Millis

interface ITouchInterface {
    interface Settings {
        var midTapDelay: Millis
        var postTapDelay: Millis
    }

    /**
     * Represents the state of a touch
     *
     * @param slot slot id for the touch
     * @param cursorX x coordinate of this touch
     * @param cursorY y coordinate of this touch
     * @param isTouching True if screen is touched at this coordinate
     */
    data class Touch(
        val slot: Int,
        var cursorX: Int = 0,
        var cursorY: Int = 0,
        var isTouching: Boolean = false
    )

    /**
     * Wrapper class to encapsulate data describing a swipe
     *
     * @param slot slot id for the touch
     * @param x1 x coordinate where the swipe starts
     * @param y1 y coordinate where the swipe starts
     * @param x2 x coordinate where the swipe ends
     * @param y2 y coordinate where the swipe ends
     */
    data class Swipe(
        val slot: Int,
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int
    )

    /**
     * Settings for this touch interface
     */
    val settings: Settings

    /**
     * List of touches on the device
     */
    val touches: List<Touch>

    /**
     * Sends a touch up event
     *
     * @param slot slot id for the touch
     */
    fun touchUp(slot: Int)

    /**
     * Sends a touch down event
     *
     * @param slot slot id for the touch
     */
    fun touchDown(slot: Int)

    /**
     * Sends a touch move event
     *
     * @param slot slot id for the touch
     * @param x x coordinate to move to
     * @param y y coordinate to move to
     */
    fun touchMove(slot: Int, x: Int, y: Int)

    /**
     * Commits the touch events
     */
    fun eventSync()

    /**
     * Send a tap to the given coordinates
     *
     * @param slot slot id for the tap
     * @param x x coordinate of tap
     * @param y y coordinate of tap
     */
    fun tap(slot: Int, x: Int, y: Int)

    /**
     * Send a swipe event
     *
     * @param duration How long the swipe should last in millis
     */
    fun swipe(swipe: Swipe, duration: Millis) = gesture(listOf(swipe), duration)

    /**
     * Send a multi swipe gesture event
     *
     * @param swipes List of swipes to do concurrently
     * @param duration How long the gesture should last in millis
     */
    fun gesture(swipes: List<Swipe>, duration: Millis)

    /**
     * Send a pinch event
     *
     * @param x x coordinate where pinch is centered
     * @param y y coordinate where pinch is centered
     * @param r1 Starting radius of pinch
     * @param r2 Ending radius of pinch
     * @param angle Angle of pinch
     * @param duration How long the pinch should last in millis
     */
    fun pinch(x: Int, y: Int, r1: Int, r2: Int, angle: Double, duration: Millis)
}
