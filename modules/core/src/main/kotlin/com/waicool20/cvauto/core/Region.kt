package com.waicool20.cvauto.core

import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.util.area
import com.waicool20.cvauto.util.asGrayF32
import com.waicool20.cvauto.util.matching.DefaultTemplateMatcher
import com.waicool20.cvauto.util.matching.ITemplateMatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Point
import java.awt.Rectangle
import java.awt.image.BufferedImage
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
abstract class Region<T : IDevice>(
    x: Pixels,
    y: Pixels,
    width: Pixels,
    height: Pixels,
    val device: T,
    val screen: Int
) : Rectangle(x, y, width, height) {
    /**
     * Represents the results of a find operation run on the device
     *
     * @param region The region containing the matched location
     * @param score The similarity score given by the matcher
     */
    data class RegionFindResult<T : IDevice>(val region: Region<T>, val score: kotlin.Double) {
        inline fun <reified R: Region<T>> region() = region as R
    }

    companion object {
        val DEFAULT_MATCHER = DefaultTemplateMatcher()
        var FIND_REFRESH: Millis = 32
    }

    /**
     * Last screen capture, should only be mutated by top-level screen regions
     */
    protected var _lastScreenCapture: Pair<Millis, BufferedImage>? = null

    private var _matcher: ITemplateMatcher? = null

    /**
     * Template matcher that will be used for find operations, can be overridden with custom matcher,
     * otherwise [Region.DEFAULT_MATCHER] will be used
     */
    var matcher: ITemplateMatcher
        get() = _matcher ?: DEFAULT_MATCHER
        set(value) {
            _matcher = value
        }

    /**
     * Captures the image of the region
     *
     * @return [BufferedImage] representation of the captured image
     */
    abstract fun capture(): BufferedImage

    /**
     * Maps a rectangle on to this region
     *
     * @param rect Rectangle to map
     * @return New [Region] with mapped rectangle
     */
    abstract fun mapRectangleToRegion(rect: Rectangle): Region<T>

    /**
     * Maps a [ITemplateMatcher.FindResult] onto this region
     *
     * @param result [ITemplateMatcher.FindResult] to map
     * @return New [Region] with mapped result
     */
    abstract fun mapFindResultToRegion(result: ITemplateMatcher.FindResult): RegionFindResult<T>

    /**
     * Gets the last screen capture of the region
     *
     * @return null if no capture was done before
     */
    fun getLastScreenCapture(): BufferedImage? {
        val screen = device.screens.getOrNull(screen)
        return if (this == screen) {
            _lastScreenCapture?.second
        } else {
            screen?.getLastScreenCapture()
        }
    }

    /**
     * Gets the last capture of this region
     *
     * @return null if not capture was done before
     */
    fun getLastCapture(): BufferedImage? {
        return getLastScreenCapture()?.getSubimage(x, y, width, height)
    }

    /**
     * Gets a smaller region that is contained in this region.
     *
     * @param x x Offset relative to the x coordinate of this region
     * @param y y Offset relative to the y coordinate of this region
     * @param width Width of sub region
     * @param height Height of sub region
     * @return New [Region] representing this sub region
     * @throws IllegalArgumentException if new sub region is not contained in the current region
     */
    fun subRegion(x: Pixels, y: Pixels, width: Pixels, height: Pixels): Region<T> {
        val r = mapRectangleToRegion(Rectangle(x, y, width, height))
        require(contains(r)) { "Sub-region must be smaller than the current region" }
        return r
    }

    /**
     * Gets a smaller region that is contained in this region.
     *
     * @param x x Offset relative to the x coordinate of this region
     * @param y y Offset relative to the y coordinate of this region
     * @param width Width of sub region
     * @param height Height of sub region
     * @return New [Region] representing this sub region
     * @throws IllegalArgumentException if new sub region is not contained in the current region
     */
    @JvmName("subRegionCast")
    inline fun <reified R: Region<T>> subRegion(x: Pixels, y: Pixels, width: Pixels, height: Pixels): R {
        return subRegion(x, y, width, height) as R
    }

    /**
     * Finds the best match for the given template in the current region
     *
     * @param template Template to use for matching
     * @return Results of the find operation
     */
    fun findBest(template: ITemplate): RegionFindResult<T>? {
        return findBest(template, 1).firstOrNull()
    }

    /**
     * Finds the best <[count]> matches for the given template in the current region
     *
     * @param template Template to use for matching
     * @param count Max number of matches to find
     * @return Results of the find operation
     */
    fun findBest(template: ITemplate, count: Int): List<RegionFindResult<T>> {
        val image = _lastScreenCapture?.let { (lastTime, img) ->
            if (System.currentTimeMillis() - lastTime > FIND_REFRESH) capture() else img
        } ?: capture()
        return matcher.findBest(template, image.asGrayF32(), count).map(::mapFindResultToRegion)
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
     * @param timeout Find operation will end after [timeout] millis
     * @return Region that matched with template or null if find operation timed out
     */
    suspend fun waitHas(template: ITemplate, timeout: Millis): Region<T>? {
        return withTimeoutOrNull(timeout) {
            var region: Region<T>? = null
            while (isActive && region == null) {
                region = findBest(template)?.region
            }
            region
        }
    }

    /**
     * Waits until the given template vanishes off screen.
     * This is the suspending version, use [waitDoesntHaveBlocking] if blocking operation is needed.
     * Inverse of [waitHas]
     *
     * @param template Template to use for matching
     * @param timeout Find operation will end after [timeout] millis
     * @return Region that matched with template or null if template couldn't be matched to begin with
     */
    suspend fun waitDoesntHave(template: ITemplate, timeout: Millis): Region<T>? {
        return withTimeoutOrNull(timeout) {
            var region: Region<T>? = findBest(template)?.region
            while (isActive && region != null) {
                region = findBest(template)?.region
            }
            region
        }
    }

    /**
     * @see waitHas
     */
    fun waitHasBlocking(template: ITemplate, timeout: Millis): Region<T>? = runBlocking {
        waitHas(template, timeout)
    }

    /**
     * @see waitDoesntHave
     */
    fun waitDoesntHaveBlocking(template: ITemplate, timeout: Millis): Region<T>? = runBlocking {
        waitDoesntHave(template, timeout)
    }

    /**
     * General compare function
     *
     * @param other Other region to compare with
     * @return >0 if this region is larger than the other;
     * 0 if area is the same;
     * <0 if this region is smaller than the other
     */
    fun compareTo(other: Region<T>): Int {
        return area - other.area
    }

    fun randomPoint(): Point {
        var dx = Random.nextDouble(width * 0.45)
        var dy = Random.nextDouble(height * 0.45)
        if (Random.nextBoolean()) dx = -dx
        if (Random.nextBoolean()) dy = -dy
        return Point((centerX + dx).roundToInt(), (centerY + dy).roundToInt())
    }

    //<editor-fold desc="Quick input shortcuts">

    /**
     * Clicks this region
     * For more complex operations please use the devices respective [IInput]
     *
     * @param random Whether or not this clicks a random point in this region, defaults true
     */
    abstract fun click(random: Boolean = true)

    /**
     * Clicks this region, while a condition holds true
     * For more complex operations please use the devices respective [IInput]
     *
     * @param random Whether or not this clicks a random point in this region, defaults true
     * @param period Delay between each check/click
     * @param timeout When timeout is reached this function will stop clicking, use -1 to disable timeout
     * @param condition Boolean condition function
     */
    suspend fun clickWhile(
        random: Boolean = true,
        period: Millis = 100,
        timeout: Millis = -1,
        condition: Region<T>.() -> Boolean
    ) {
        if (timeout > 0) {
            withTimeoutOrNull(timeout) {
                while (isActive && this@Region.condition()) {
                    click(random)
                    delay(period)
                }
            }
        } else {
            while (this.condition()) {
                click(random)
                delay(period)
            }
        }
    }

    /**
     * Clicks the template if it exists, while a condition holds true
     * For more complex operations please use the devices respective [IInput]
     *
     * @param template Template to use
     * @param random Whether or not this clicks a random point in this region, defaults true
     * @param period Delay between each check/click
     * @param timeout When timeout is reached this function will stop clicking, use -1 to disable timeout
     * @param condition Boolean condition function
     */
    suspend fun clickTemplateWhile(
        template: ITemplate,
        random: Boolean = true,
        period: Millis = 100,
        timeout: Millis = -1,
        condition: Region<T>.(ITemplate) -> Boolean
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