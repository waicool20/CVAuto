package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateMatching
import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.util.matching.ITemplateMatcher.FindResult
import com.waicool20.cvauto.util.asGrayF32
import com.waicool20.cvauto.util.blurred
import com.waicool20.cvauto.util.scale
import java.awt.Rectangle
import kotlin.math.roundToInt

/**
 * Default template matcher, good for most purposes. Uses the normalised squared difference algorithm from
 * [TemplateDiffSquaredNorm]
 */
class DefaultTemplateMatcher : ITemplateMatcher {
    data class Settings(
        private var _matchWidth: Double = 0.0,
        /**
         * Default threshold in case it isn't specified in the template
         */
        var defaultThreshold: Double = 0.9,
        /**
         * Filter overlapping match results
         */
        var filterOverlap: Boolean = true,
        /**
         * Non-zero value will let the matcher blur the image before matching,
         */
        var blurRadius: Int = 2
    ) {
        /**
         * Images get scaled down to this width while maintaining ratio during matching,
         * A smaller value will lead to faster matches but with poorer accuracy.
         * Set this to 0 or negative number to disable
         *
         */
        var matchWidth: Double
            get() = _matchWidth
            set(value) {
                _matchWidth = value
                imageCache.clear()
            }
    }

    companion object {
        // TODO Template Image equal size bug, https://github.com/lessthanoptimal/BoofCV/issues/144
        private val matchingAlgo get() = DefaultTemplateIntensityImage(TemplateDiffSquaredNorm())
        private val imageCache = mutableMapOf<ITemplate, GrayF32>()
    }

    val settings = Settings()

    override fun findBest(template: ITemplate, image: GrayF32): FindResult? {
        return findBest(template, image, 1).firstOrNull()
    }

    override fun findBest(template: ITemplate, image: GrayF32, count: Int): List<FindResult> {
        val scaleFactor = if (settings.matchWidth > 0) {
            settings.matchWidth / image.width
        } else 1.0
        val scaledImage = image.scale(scaleFactor).blurred(settings.blurRadius)

        val lTemplate = template.load()
        val bTemplate = imageCache.getOrPut(template) {
            lTemplate.asGrayF32().scale(scaleFactor).blurred(settings.blurRadius)
        }

        val matcher = TemplateMatching(matchingAlgo)
        matcher.setImage(scaledImage)
        //TODO Support Masks?
        matcher.setTemplate(bTemplate, null, count)
        matcher.process()

        val results = matcher.results.toList().mapNotNull {
            val adjustedScore = it.score + 1
            val threshold = template.threshold ?: settings.defaultThreshold
            if (adjustedScore < threshold) return@mapNotNull null
            FindResult(
                Rectangle(
                    (it.x / scaleFactor).roundToInt(),
                    (it.y / scaleFactor).roundToInt(),
                    lTemplate.width,
                    lTemplate.height
                ),
                adjustedScore
            )
        }
        if (settings.filterOverlap) return results.removeOverlaps()
        return results
    }

    private fun List<FindResult>.removeOverlaps(): List<FindResult> {
        val copy = toMutableList()
        for (r in copy) {
            for (r1 in copy) {
                if (r == r1) continue
                if (r.rectangle.intersects(r1.rectangle)) {
                    if (r.score < r1.score) {
                        copy.remove(r)
                    } else {
                        copy.remove(r1)
                    }
                }
            }
        }
        return copy
    }
}