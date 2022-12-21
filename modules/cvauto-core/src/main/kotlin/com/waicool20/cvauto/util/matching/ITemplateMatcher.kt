package com.waicool20.cvauto.util.matching

import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.template.ITemplate
import java.awt.Rectangle
import java.awt.image.BufferedImage

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
        /**
         * Filter overlapping match results
         */
        open var filterOverlap: Boolean = true
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
}
