package com.waicool20.cvauto.util.matching

import boofcv.alg.feature.detect.template.TemplateIntensityImage_MT
import boofcv.struct.image.ImageBase

/**
 * Temporary fix for bug where matching fails when image and template are equal size
 * @see <a href="https://github.com/lessthanoptimal/BoofCV/issues/144">Github Issue</a>
 */
@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
class DefaultTemplateIntensityImage<T: ImageBase<T>>(method: EvaluatorMethod<T>): TemplateIntensityImage_MT<T>(method) {
    override fun process(template: T) {
        this.template = template
        mask = null
        intensity.reshape(image.width, image.height)
        var w = image.width - template.width
        var h = image.height - template.height

        if (w == 0) w++
        if (h == 0) h++

        borderX0 = template.width / 2
        borderY0 = template.height / 2
        borderX1 = template.width - borderX0
        borderY1 = template.height - borderY0
        method.initialize(this)
        processInner(w, h)
        // deference to avoid causing a memory leak
        this.template = null
        mask = null
    }
}