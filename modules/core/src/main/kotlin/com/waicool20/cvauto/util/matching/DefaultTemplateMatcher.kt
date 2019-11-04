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
    companion object {
        private const val MATCHING_RES_WIDTH = 500.0
        private val matchingAlgo = TemplateIntensityImage_MT(TemplateDiffSquaredNorm())
    }

    private val imageCache = mutableMapOf<ITemplate, GrayF32>()


    override fun findBest(template: ITemplate, image: GrayF32): ITemplateMatcher.FindResult? {
        return findBest(template, image, 1).firstOrNull()
    }

    override fun findBest(template: ITemplate, image: GrayF32, count: Int): List<ITemplateMatcher.FindResult> {
        val scaleFactor = MATCHING_RES_WIDTH / image.width
        val scaledImage = image.scale(scaleFactor)
        val lTemplate = template.load()
        val bTemplate = imageCache.getOrPut(template) { lTemplate.asGrayF32().scale(scaleFactor) }


        val matcher = TemplateMatching(matchingAlgo)
        matcher.setImage(scaledImage)
        //TODO Support Masks?
        matcher.setTemplate(bTemplate, null, count)
        matcher.process()

        return matcher.results.toList().mapNotNull {
            val adjustedScore = it.score + 1
            if (adjustedScore < template.threshold) return@mapNotNull null
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
    }
}