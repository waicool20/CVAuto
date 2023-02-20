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

package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI

/**
 * Template that will be used for matching on screen elements
 */
interface ITemplate {
    /**
     * URI of the source for the image that this template is based on, since this is a URI
     * it can represent anything from a local file to an url of image on the internet.
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
