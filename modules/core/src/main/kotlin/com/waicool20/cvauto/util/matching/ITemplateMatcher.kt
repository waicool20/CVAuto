package com.waicool20.cvauto.util.matching

import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.core.Pixels
import com.waicool20.cvauto.core.template.ITemplate
import java.awt.Rectangle

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
         * Images get scaled down to this width while maintaining ratio during matching,
         * A smaller value will lead to faster matches but with poorer accuracy.
         * Set this to 0 or negative number to disable
         *
         */
        open var matchWidth: Pixels = 0,
        /**
         * Default threshold in case it isn't specified in the template
         */
        open var defaultThreshold: Double = 0.9,
        /**
         * Filter overlapping match results
         */
        open var filterOverlap: Boolean = true,
        /**
         * Non-zero value will let the matcher blur the image before matching,
         */
        open var blurRadius: Int = 2,
        /**
         * Matching algorithm to use
         */
        open var evaluator: () -> EvaluatorMethodAdapter = { TemplateCosEffNorm() }
    )

    val settings: Settings

    /**
     * Finds the best match for the given template inside the given image
     *
     * @param template Template to use for matching
     * @param image Base image to use for matching
     * @return Results of the match operation
     */
    fun findBest(template: ITemplate, image: GrayF32): FindResult?

    /**
     * Finds the best match for the given template inside the given image
     *
     * @param template Template to use for matching
     * @param image Base image to use for matching
     * @param count Max number of matches to find
     * @return Results of the match operation
     */
    fun findBest(template: ITemplate, image: GrayF32, count: Int): List<FindResult>
}