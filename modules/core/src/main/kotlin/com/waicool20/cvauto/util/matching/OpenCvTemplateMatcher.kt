/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.cvauto.util.matching

import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.util.at
import com.waicool20.cvauto.util.toMat
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
