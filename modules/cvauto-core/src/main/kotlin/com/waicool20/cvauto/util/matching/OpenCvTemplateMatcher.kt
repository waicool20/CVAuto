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
import com.waicool20.cvauto.util.removeChannels
import com.waicool20.cvauto.util.toMat
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgproc.Imgproc
import java.awt.Rectangle
import java.awt.image.BufferedImage

class OpenCvTemplateMatcher : ITemplateMatcher {
    class Settings() : ITemplateMatcher.Settings()

    companion object {
        private val imageCache = mutableMapOf<ITemplate, Mat>()
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

        val templateMat = imageCache.getOrPut(template) {
            template.load().toMat().apply {
                // Remove alpha channel
                if (channels() > 3) removeChannels(0)
                // Convert to grayscale
                if (channels() > 1) Imgproc.cvtColor(this, this, Imgproc.COLOR_RGB2GRAY)
            }
        }

        val threshold = template.threshold ?: settings.defaultThreshold

        val matches = doMatch(imageMat, templateMat, threshold)
        return if (settings.filterOverlap) {
            matches.removeOverlaps().take(count)
        } else matches.take(count)
    }

    private fun doMatch(
        image: Mat,
        template: Mat,
        threshold: Double
    ): List<ITemplateMatcher.FindResult> {
        val output = Mat()
        // Do the template matching
        Imgproc.matchTemplate(image, template, output, Imgproc.TM_CCOEFF_NORMED)
        // Do the thresholding, any points that are less than threshold get zeroed
        Imgproc.threshold(output, output, threshold, 1.0, Imgproc.THRESH_TOZERO)
        // Package each point into a FindResult
        val points = MatOfPoint()
        Core.findNonZero(output, points)

        val result = mutableListOf<ITemplateMatcher.FindResult>()

        for (point in points.toArray()) {
            val x = point.x.toInt()
            val y = point.y.toInt()
            result.add(
                ITemplateMatcher.FindResult(
                    Rectangle(x, y, template.width(), template.height()),
                    output.at<Float>(y, x).v.toDouble()
                )
            )
        }

        return result.sortedByDescending { it.score }
    }
}
