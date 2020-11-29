package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.IIOException
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
    companion object {
        val checkPaths: MutableList<Path> = mutableListOf()

        private fun resolvePath(path: String): Path {
            return checkPaths.map { it.resolve(path) }.firstOrNull { Files.exists(it) }
                ?: Paths.get(path)
        }
    }

    constructor(path: String, threshold: Double? = null):
            this(resolvePath(path), threshold)

    override val source: URI = path.toUri()

    override fun load(): BufferedImage {
        try {
            return ImageIO.read(path.toFile())
        } catch (e: IIOException) {
            throw Exception("Error reading file: ${path.toAbsolutePath()}", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is FileTemplate && path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
