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

package com.waicool20.cvauto.core.util.matching

import com.waicool20.cvauto.core.template.ITemplate
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.*

/**
 * Interface class to implement for custom template matchers
 */
interface ITemplateMatcher {
    /**
     * Represents the results of a matching operation
     *
     * @param rectangle The rectangle containing the template
     * @param score The similarity score given by the matcher
     */
    data class FindResult(val rectangle: Rectangle, val score: Double)

    open class Settings(
        /**
         * Default threshold in case it isn't specified in the template
         */
        open var defaultThreshold: Double = 0.9,
    )

    val settings: Settings

    /**
     * Finds the best match for the given template inside the given image
     *
     * @param template Template to use for matching
     * @param image Base image to use for matching
     * @return Results of the match operation
     */
    fun findBest(template: ITemplate, image: BufferedImage): FindResult?

    /**
     * Finds the best match for the given template inside the given image
     *
     * @param template Template to use for matching
     * @param image Base image to use for matching
     * @param count Max number of matches to find
     * @return Results of the match operation
     */
    fun findBest(template: ITemplate, image: BufferedImage, count: Int): List<FindResult>

    /**
     * This method removes any overlapping [FindResult], giving priority to the [FindResult]
     * with the highest score.
     *
     * @return List of [FindResult] without overlaps, returned list is not sorted by score
     */
    fun List<FindResult>.removeOverlaps(): List<FindResult> {
        val result = mutableListOf<FindResult>()
        val overlapping = TreeSet<FindResult> { r1, r2 -> r2.score.compareTo(r1.score) }

        // Loop through results sorted by rectangle x coordinate
        for (current in sortedBy { it.rectangle.x }) {
            // Look in overlapping for the first rect that intersects with current
            // The first one that is returned is the highest scoring one
            // because overlapping is a TreeSet sorted by score
            val highestScoringOverlap = overlapping.asSequence()
                .filter { it.rectangle.intersects(current.rectangle) }
                .firstOrNull()

            if (highestScoringOverlap == null) {
                // If it's null, then that means this rectangle is the current highest scoring
                // rectangle, so we add it to the result and overlapping collection
                overlapping.add(current)
                result.add(current)
            } else if (current.score > highestScoringOverlap.score) {
                // If it's not null and the current score is higher than the existing rectangle
                // then we give priority to the current one and remove the previous high score holder
                overlapping.remove(highestScoringOverlap)
                result.remove(highestScoringOverlap)
                overlapping.add(current)
                result.add(current)
            }
        }
        return result
    }
}
