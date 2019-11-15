package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

/**
 * Represents a file based template
 *
 * @param path File path
 */
class FileTemplate(
    private val path: Path,
    override val threshold: Double? = null
) : ITemplate {
    constructor(path: String, threshold: Double? = null):
            this(Paths.get(path), threshold)

    override val source: URI = path.toUri()

    override fun load(): BufferedImage {
        return ImageIO.read(path.toFile())
    }

    override fun equals(other: Any?): Boolean {
        return other is FileTemplate && path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
