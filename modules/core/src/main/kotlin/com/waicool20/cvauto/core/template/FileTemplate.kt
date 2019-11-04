package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Represents a file based template
 *
 * @param path File path
 */
class FileTemplate(
    private val path: Path,
    override val threshold: Double
) : ITemplate {
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
