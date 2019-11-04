package com.waicool20.cvauto.util.matching

import boofcv.struct.image.GrayF32
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