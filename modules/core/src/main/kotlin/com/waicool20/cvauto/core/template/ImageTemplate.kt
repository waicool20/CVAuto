package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI

class ImageTemplate(
    private val image: BufferedImage,
    override val threshold: Double? = null
) : ITemplate {
    override val source: URI = URI.create("image://${hashCode()}")

    override fun load(): BufferedImage {
        return image
    }

    override fun equals(other: Any?): Boolean {
        return other is ImageTemplate && image == other.image
    }

    override fun hashCode(): Int {
        return image.hashCode()
    }
}

typealias IT = ImageTemplate
