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
import com.waicool20.cvauto.core.util.at
import com.waicool20.cvauto.core.util.toMat
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.awt.Rectangle
import java.awt.image.BufferedImage

class OpenCvTemplateMatcher : ITemplateMatcher {
    class Settings : ITemplateMatcher.Settings()
    private data class TemplateContainer(val template: Mat, val mask: Mat?)

    companion object {
        private val imageCache = mutableMapOf<ITemplate, TemplateContainer>()
    }

    override val settings = Settings()

    override fun findBest(template: ITemplate, image: BufferedImage): ITemplateMatcher.FindResult? {
        return findBest(template, image, 1).firstOrNull()
    }

    override fun findBest(
        template: ITemplate,
        image: BufferedImage,
        count: Int
    ): List<ITemplateMatcher.FindResult> {
        // Setup opencv Mats
        val imageMat = image.toMat()
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGB2GRAY)

        val templateContainer = imageCache.getOrPut(template) { prepareTemplate(template) }
        val threshold = template.threshold ?: settings.defaultThreshold

        return doMatch(imageMat, templateContainer, threshold).removeOverlaps()
            .sortedByDescending { it.score }.take(count)
    }

    private fun prepareTemplate(template: ITemplate): TemplateContainer {
        // load() typically returns a BufferedImage of type 4 byte ABGR for FileTemplate
        val templateBufferedImage = template.load()
        val sourceMat = templateBufferedImage.toMat()

        val templateMat = Mat(sourceMat.rows(), sourceMat.cols(), CvType.CV_8UC3)
        val maskMat = Mat(sourceMat.rows(), sourceMat.cols(), CvType.CV_8UC1)

        var hasMask = false

        val fromTo = if (sourceMat.channels() > 3) {
            hasMask = true
            // Mix up the channels so that mask ends up with the alpha
            // and template contains the rest of the channels
            // (A, B, G, R) -> (A), (B, G, R)
            // (0, 1, 2, 3) -> (0,   1, 2, 3)
            MatOfInt(0, 0, 1, 1, 2, 2, 3, 3)
        } else {
            // Copy the channels over to template mat
            // (B, G, R) -> (A), (B, G, R)
            // (0, 1, 2) -> (0,   1, 2, 3)
            MatOfInt(0, 1, 1, 2, 2, 3)
        }

        Core.mixChannels(listOf(sourceMat), listOf(maskMat, templateMat), fromTo)

        // Convert to grayscale
        if (templateMat.channels() > 1) {
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGR2GRAY)
        }

        return TemplateContainer(templateMat, maskMat.takeIf { hasMask })
    }

    private fun doMatch(
        image: Mat,
        templateContainer: TemplateContainer,
        threshold: Double
    ): List<ITemplateMatcher.FindResult> {
        val output = Mat()
        val (template, mask) = templateContainer
        // Do the template matching
        if (templateContainer.mask == null) {
            Imgproc.matchTemplate(image, template, output, Imgproc.TM_CCOEFF_NORMED)
        } else {
            Imgproc.matchTemplate(image, template, output, Imgproc.TM_CCOEFF_NORMED, mask)
        }
        // Do the thresholding, any points that are less than threshold get zeroed
        Imgproc.threshold(output, output, threshold, 1.0, Imgproc.THRESH_TOZERO)
        // Package each point into a FindResult
        val points = MatOfPoint()
        Core.findNonZero(output, points)

        val result = mutableListOf<ITemplateMatcher.FindResult>()

        for (point in points.toArray()) {
            val x = point.x.toInt()
            val y = point.y.toInt()
            val score = output.at<Float>(y, x).v.toDouble()
            if (score.isInfinite() || score.isNaN()) continue
            result.add(
                ITemplateMatcher.FindResult(
                    Rectangle(x, y, template.width(), template.height()),
                    score
                )
            )
        }

        return result
    }
}
