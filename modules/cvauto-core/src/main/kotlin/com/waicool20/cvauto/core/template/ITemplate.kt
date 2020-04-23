package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI

/**
 * Template that will be used for matching on screen elements
 */
interface ITemplate {
    /**
     * URI of the source for the image that this template is based on, since this is an URI
     * it can represent anything from a local file to a url of image on the internet.
     */
    val source: URI

    /**
     * Score threshold for the template, ranges from 0.0 to 1.0, 0.0 means absolutely nothing in common
     * while 1.0 means 100% match.
     */
    val threshold: Double?

    /**
     * This function is in charge of loading the image into memory
     *
     * @return [BufferedImage] representation of the template
     */
    fun load(): BufferedImage
}