package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateMatching
import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.util.*
import com.waicool20.cvauto.util.matching.ITemplateMatcher.FindResult
import java.awt.Rectangle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Default template matcher, good for most purposes. Uses the normalised squared difference algorithm from
 * [TemplateSqDiffNorm]
 */
class DefaultTemplateMatcher : ITemplateMatcher {
    class Settings : ITemplateMatcher.Settings() {
        override var matchWidth = 0
            set(value) {
                field = value
                imageCache.clear()
            }
    }

    companion object {
        private val imageCache = mutableMapOf<ITemplate, GrayF32>()
    }

    // TODO Template Image equal size bug, https://github.com/lessthanoptimal/BoofCV/issues/144
    private val matchingAlgo get() = DefaultTemplateIntensityImage(settings.evaluator())

    override val settings = Settings()

    override fun findBest(template: ITemplate, image: GrayF32): FindResult? {
        return findBest(template, image, 1).firstOrNull()
    }

    override fun findBest(template: ITemplate, image: GrayF32, count: Int): List<FindResult> {
        val scaleFactor = if (settings.matchWidth > 0) {
            min(1.0, settings.matchWidth.toDouble() / image.width)
        } else 1.0
        val scaledImage = image.scale(scaleFactor).blurred(settings.blurRadius)

        val templateF32 = imageCache.getOrPut(template) { template.load().asGrayF32() }
        val templateF32Scaled = templateF32.scale(scaleFactor).blurred(settings.blurRadius)

        val threshold = template.threshold ?: settings.defaultThreshold
        var results = doMatch(templateF32Scaled, scaledImage, count)
            .getFindResults(threshold, scaleFactor, templateF32.width, templateF32.height)

        if (scaleFactor != 1.0) {
            val bounds = Rectangle(0, 0, image.width, image.height)
            // Filter really bad results
            results = results.filter { it.score > threshold * 0.75 }
                // Get a rectangle representing the approximate match area and grow it to allow for some error
                .map { it.rectangle.apply { grow(50, 50) }.cropIntoRect(bounds) }
                // Map it to the image
                .map { it to image.subimage(it.x1, it.y1, it.x2, it.y2) }
                // Do the fine match using the subimage
                .flatMap { (subRect, subImage) ->
                    doMatch(templateF32, subImage, 1)
                        .getFindResults(threshold, 1.0, templateF32.width, templateF32.height)
                        .onEach {
                            it.rectangle.x = it.rectangle.x + subRect.x
                            it.rectangle.y = it.rectangle.y + subRect.y
                        }
                }
        }

        return if (settings.filterOverlap) results.removeOverlaps() else results
    }

    private fun doMatch(template: GrayF32, image: GrayF32, count: Int): TemplateMatching<GrayF32> {
        return TemplateMatching(matchingAlgo).apply {
            setImage(image)
            //TODO Support Masks?
            setMinimumSeparation(min(template.width, template.height))
            setTemplate(template, null, count)
            process()
        }
    }

    private fun TemplateMatching<GrayF32>.getFindResults(
        threshold: Double,
        scaleFactor: Double,
        width: Int,
        height: Int
    ): List<FindResult> {
        return results.toList().mapNotNull {
            if (scaleFactor == 1.0 && it.score < threshold) return@mapNotNull null
            FindResult(
                Rectangle((it.x / scaleFactor).roundToInt(), (it.y / scaleFactor).roundToInt(), width, height),
                it.score
            )
        }
    }

    private fun List<FindResult>.removeOverlaps(): List<FindResult> {
        val toRemoveIndices = mutableListOf<Int>()
        for ((i, r1) in this.withIndex()) {
            for ((j, r2) in this.withIndex()) {
                if (i == j) continue
                if (r1.rectangle.intersects(r2.rectangle)) {
                    if (r1.score < r2.score) {
                        toRemoveIndices.add(i)
                    } else {
                        toRemoveIndices.add(j)
                    }
                }
            }
        }
        return filterIndexed { i, _ -> !toRemoveIndices.contains(i) }
    }
}