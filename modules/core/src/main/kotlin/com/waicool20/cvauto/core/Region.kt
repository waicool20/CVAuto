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

package com.waicool20.cvauto.core

import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.core.template.ITemplateMatcher
import com.waicool20.cvauto.core.template.OpenCvTemplateMatcher
import com.waicool20.cvauto.core.util.area
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Represents a Region on a [IDevice]
 *
 * @param x x Coordinate of the top left corner of the region
 * @param y y Coordinate of the top left corner of the region
 * @param width Width of the region
 * @param height Height of the region
 * @param device Device that the region belongs to
 * @param screen Screen index
 * @property device Device that the region belongs to
 * @property screen Screen index
 */
abstract class Region<D : IDevice<D, E, R>, E : IDisplay<D, E, R>, R : Region<D, E, R>>(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    val device: D,
    val display: E,
    protected var frozenCapture: Capture? = null
) : Rectangle(x, y, width, height), Comparable<Region<D, E, R>> {
    class CaptureIOException(cause: Throwable) : Exception(cause)

    /**
     * Represents the results of a find operation run on the device
     *
     * @param region The region containing the matched location
     * @param score The similarity score given by the matcher
     */
    data class RegionFindResult<D : IDevice<D, E, R>, E : IDisplay<D, E, R>, R : Region<D, E, R>>(
        val region: R,
        val score: kotlin.Double
    )

    companion object {
        val DEFAULT_MATCHER = OpenCvTemplateMatcher()
    }

    /**
     * Template matcher that will be used for find operations, can be overridden with custom matcher,
     * otherwise [Region.DEFAULT_MATCHER] will be used
     */
    var matcher: ITemplateMatcher = DEFAULT_MATCHER

    /**
     * Checks if this region is frozen, a frozen region will always return the same capture when
     * it was frozen
     */
    val frozen: Boolean
        get() = this.frozenCapture != null

    /**
     * Captures the image of the region
     */
    fun capture(): Capture {
        val c = frozenCapture ?: display.capture()
        return c.copy(img = c.img.getSubimage(x, y, width, height))
    }

    /**
     * Maps a rectangle on to this region
     *
     * @param rect Rectangle to map
     * @return New [Region] with mapped rectangle
     */
    fun mapRectangleToRegion(rect: Rectangle): R {
        return copy(rect.x + x, rect.y + y, rect.width, rect.height, device, display)
    }

    /**
     * Maps a [ITemplateMatcher.FindResult] onto this region
     *
     * @param result [ITemplateMatcher.FindResult] to map
     * @return New [Region] with mapped result
     */
    fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<D, E, R> {
        return RegionFindResult(mapRectangleToRegion(result.rectangle), result.score)
    }

    /**
     * Checks if this region is the root region
     */
    fun isRootRegion(): Boolean {
        return this == display.region || contains(display.region)
    }

    /**
     * Gets a smaller region that is contained in this region.
     *
     * @param x x Offset relative to the x coordinate of this region
     * @param y y Offset relative to the y coordinate of this region
     * @param width Width of subregion
     * @param height Height of sub-region
     * @return New [Region] representing this sub-region
     * @throws IllegalArgumentException if new sub-region is not contained in the current region
     */
    fun subRegion(x: Pixels, y: Pixels, width: Pixels, height: Pixels): R {
        val r = mapRectangleToRegion(Rectangle(x, y, width, height))
        require(contains(r)) { "Sub-region must be smaller than the current region" }
        return r
    }

    /**
     * Finds the best match for the given template in the current region
     *
     * @param template Template to use for matching
     * @return Results of the find operation
     */
    fun findBest(template: ITemplate): RegionFindResult<D, E, R>? {
        return findBest(template, 1).firstOrNull()
    }

    /**
     * Finds the best <[count]> matches for the given template in the current region
     *
     * @param template Template to use for matching
     * @param count Max number of matches to find
     * @return Results of the find operation
     */
    fun findBest(template: ITemplate, count: Int): List<RegionFindResult<D, E, R>> {
        return matcher.findBest(template, capture().img, count).map(::mapFindResultToRegion)
    }

    /**
     * Checks if template exists on screen, useful if you don't need the result of the find
     * operation. Inverse of [doesntHave]
     *
     * @param template Template to use for matching
     * @return true if given template exists on screen
     */
    fun has(template: ITemplate): Boolean = findBest(template) != null

    /**
     * Checks if template doesn't exist on screen, useful if you don't need the result
     * of the find operation. Inverse of [has]
     *
     * @param template Template to use for matching
     * @return true if given template doesn't exist on screen
     */
    fun doesntHave(template: ITemplate): Boolean = !has(template)

    /**
     * Waits until the given template shows up on screen.
     * This is the suspending version, use [waitHasBlocking] if blocking operation is needed.
     * Inverse of [waitDoesntHave]
     *
     * @param template Template to use for matching
     * @param timeout Find operation will end after [timeout] millis, timeout is disabled
     *                if the value is zero or a non-positive value
     * @return Region that matched with template or null if the find operation timed out
     */
    suspend fun waitHas(template: ITemplate, timeout: Millis = -1): R? {
        val start = System.currentTimeMillis()
        var region: R? = null
        while (coroutineContext.isActive && region == null) {
            region = findBest(template)?.region
            if (timeout > 0 && System.currentTimeMillis() - start >= timeout) return null
        }
        return region
    }

    /**
     * Waits until the given template vanishes off-screen.
     * This is the suspending version, use [waitDoesntHaveBlocking] if blocking operation is needed.
     * Inverse of [waitHas]
     *
     * @param template Template to use for matching
     * @param timeout Find operation will end after [timeout] millis, timeout is disabled
     *                if the value is zero or a non-positive value
     */
    suspend fun waitDoesntHave(template: ITemplate, timeout: Millis = -1) {
        val start = System.currentTimeMillis()
        var region: R? = findBest(template)?.region ?: return
        while (coroutineContext.isActive && region != null) {
            region = findBest(template)?.region
            if (timeout > 0 && System.currentTimeMillis() - start >= timeout) return
        }
    }

    /**
     * @see waitHas
     */
    fun waitHasBlocking(template: ITemplate, timeout: Millis = -1): R? =
        runBlocking(Dispatchers.IO) { waitHas(template, timeout) }

    /**
     * @see waitDoesntHave
     */
    fun waitDoesntHaveBlocking(template: ITemplate, timeout: Millis = -1) =
        runBlocking(Dispatchers.IO) { waitDoesntHave(template, timeout) }

    /**
     * Pick color from the given coordinates
     */
    fun pickColor(x: Pixels, y: Pixels): Color {
        return Color(capture().img.getRGB(x, y))
    }

    /**
     * General compare function
     *
     * @param other Other region to compare with
     * @return >0 if this region is larger than the other;
     * 0 if area is the same;
     * <0 if this region is smaller than the other
     */
    override fun compareTo(other: Region<D, E, R>): Int {
        return area - other.area
    }

    fun randomPoint(): Point {
        var dx = Random.nextDouble(width * 0.45)
        var dy = Random.nextDouble(height * 0.45)
        if (Random.nextBoolean()) dx = -dx
        if (Random.nextBoolean()) dy = -dy
        return Point((centerX + dx).roundToInt(), (centerY + dy).roundToInt())
    }

    /**
     * Copy constructor
     */
    abstract fun copy(
        x: Pixels = this.x,
        y: Pixels = this.y,
        width: Pixels = this.width,
        height: Pixels = this.height,
        device: D = this.device,
        display: E = this.display
    ): R

    @Deprecated("Use copy instead", ReplaceWith("copy()"))
    final override fun clone(): Any = error("Use Region<T, R>.copy()")

    /**
     * Freezes this region, which will cache the current capture of the region to be used for future
     * use
     */
    fun freeze(): R {
        if (this === display.region) {
            return this.copy().freeze()
        }
        this.frozenCapture = display.capture()
        @Suppress("UNCHECKED_CAST")
        return this as R
    }

    //<editor-fold desc="Quick input shortcuts">

    /**
     * Clicks this region
     * For more complex operations please use the devices respective [IInput]
     *
     * @param random Whether this clicks a random point in this region, defaults true
     */
    abstract fun click(random: Boolean = true)

    /**
     * Clicks this region, while a condition holds true
     * For more complex operations please use the devices respective [IInput]
     *
     * @param random Whether this clicks a random point in this region, defaults true
     * @param period Delay between each check/click
     * @param timeout When timeout is reached this function will stop clicking, timeout is disabled
     *                if the value is zero or a non-positive value
     * @param condition Boolean condition function
     * @throws TimeoutCancellationException If timeout is enabled and function times out
     */
    suspend fun clickWhile(
        random: Boolean = true,
        period: Millis = 100,
        timeout: Millis = -1,
        condition: Region<D, E, R>.() -> Boolean
    ) {
        val start = System.currentTimeMillis()
        while (coroutineContext.isActive && this.condition()) {
            click(random)
            if (timeout > 0 && System.currentTimeMillis() - start >= timeout) return
            delay(period)
        }
    }

    /**
     * Clicks the template if it exists, while a condition holds true
     * For more complex operations please use the devices respective [IInput]
     *
     * @param template Template to use
     * @param random Whether this clicks a random point in this region, defaults true
     * @param period Delay between each check/click
     * @param timeout When timeout is reached this function will stop clicking, use -1 to disable timeout
     * @param condition Boolean condition function
     * @throws TimeoutCancellationException If timeout is enabled and function times out
     */
    suspend fun clickTemplateWhile(
        template: ITemplate,
        random: Boolean = true,
        period: Millis = 100,
        timeout: Millis = -1,
        condition: Region<D, E, R>.(ITemplate) -> Boolean
    ) {
        val r = findBest(template)?.region ?: return
        r.clickWhile(random, period, timeout) { condition(this, template) }
    }

    /**
     * Type text in this region
     * For more complex operations please use the devices respective [IInput]
     *
     * @param text Text to type
     */
    abstract fun type(text: String)

    //</editor-fold>
}

/**
 * Provided as a shorthand for [Region<*, *>]
 */
typealias AnyRegion = Region<*, *, *>
