package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateIntensityImage
import boofcv.struct.image.GrayF32

/**
 * Base class for evaluator methods used by cvauto
 */
abstract class EvaluatorMethodAdapter : TemplateIntensityImage.EvaluatorMethod<GrayF32> {
    companion object {
        /*
        Reflection needed since these fields are package protected in the library,
        Caching reflected fields for faster access in the future
         */
        private val tiiFields by lazy {
            TemplateIntensityImage::class.java
        }
        private val templateField by lazy {
            tiiFields.getDeclaredField("template").apply { isAccessible = true }
        }
        private val imageField by lazy {
            tiiFields.getDeclaredField("image").apply { isAccessible = true }
        }
    }

    protected lateinit var template: GrayF32
    protected lateinit var image: GrayF32

    override fun initialize(owner: TemplateIntensityImage<GrayF32>) {
        template = templateField.get(owner) as GrayF32
        image = imageField.get(owner) as GrayF32
    }

    override fun evaluate(tl_x: Int, tl_y: Int): Float {
        throw UnsupportedOperationException("Not Implemented")
    }

    override fun evaluateMask(tl_x: Int, tl_y: Int): Float {
        throw UnsupportedOperationException("Not Implemented")
    }

    override fun isBorderProcessed() = false
    override fun isMaximize() = false
}