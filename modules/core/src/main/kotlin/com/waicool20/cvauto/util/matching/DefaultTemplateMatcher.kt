package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateIntensityImage_MT
import boofcv.alg.feature.detect.template.TemplateMatching
import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.util.asGrayF32
import com.waicool20.cvauto.util.scale
import java.awt.Rectangle
import kotlin.math.roundToInt

/**
 * Default template matcher, good for most purposes. Uses the normalised squared difference algorithm from
 * [TemplateDiffSquaredNorm]
 */
class DefaultTemplateMatcher : ITemplateMatcher {
    data class Settings(
        private var _matchWidth: Double = 500.0,
        /**
         * Default threshold in case it isn't specified in the template
         */
        var defaultThreshold: Double = 0.9,
        var filterOverlap: Boolean = true
    ) {
        /**
         * Images get scaled down to this width while maintaining ratio during matching,
         * A smaller value will lead to faster matches but with poorer accuracy.
         */
        var matchWidth: Double = _matchWidth
            get() = _matchWidth
            set(value) {
                imageCache.clear()
                field = value
            }
    }

    companion object {
        // TODO Template Image equal size bug, https://github.com/lessthanoptimal/BoofCV/issues/144
        private val matchingAlgo = DefaultTemplateIntensityImage(TemplateDiffSquaredNorm())
        private val imageCache = mutableMapOf<ITemplate, GrayF32>()
    }

    val settings = Settings()

    override fun findBest(template: ITemplate, image: GrayF32): ITemplateMatcher.FindResult? {
        return findBest(template, image, 1).firstOrNull()
    }

    override fun findBest(template: ITemplate, image: GrayF32, count: Int): List<ITemplateMatcher.FindResult> {
        val scaleFactor: Double
        val scaledImage: GrayF32

        if (image.width > settings.matchWidth) {
            scaleFactor = settings.matchWidth / image.width
            scaledImage = image.scale(scaleFactor)
        } else {
            scaleFactor = 1.0
            scaledImage = image
        }

        val lTemplate = template.load()
        val bTemplate = imageCache.getOrPut(template) { lTemplate.asGrayF32().scale(scaleFactor) }

        val matcher = TemplateMatching(matchingAlgo)
        matcher.setImage(scaledImage)
        //TODO Support Masks?
        matcher.setTemplate(bTemplate, null, count)
        matcher.process()

        val results = matcher.results.toList().mapNotNull {
            val adjustedScore = it.score + 1
            val threshold = template.threshold ?: settings.defaultThreshold
            if (adjustedScore < threshold) return@mapNotNull null
            ITemplateMatcher.FindResult(
                Rectangle(
                    (it.x / scaleFactor).roundToInt(),
                    (it.y / scaleFactor).roundToInt(),
                    lTemplate.width,
                    lTemplate.height
                ),
                adjustedScore
            )
        }
        if (settings.filterOverlap) {
            val resCopy = results.toMutableList()
            for (r in results) {
                for (r1 in results) {
                    if (r == r1) continue
                    if (r.rectangle.intersects(r1.rectangle)) {
                        if (r.score < r1.score) {
                            resCopy.remove(r)
                        } else {
                            resCopy.remove(r1)
                        }
                    }
                }
            }
            return resCopy
        }
        return results
    }
}