/*
 * The MIT License (MIT)
 *
 * Copyright (c) waicool20
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.waicool20.cvauto.core.template

import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Path
import javax.imageio.IIOException
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

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
            return checkPaths.map { it.resolve(path) }.firstOrNull { it.exists() } ?: Path(path)
        }
    }

    constructor(path: String, threshold: Double? = null) :
        this(resolvePath(path), threshold)

    override val source: URI = path.toUri()

    override fun load(): BufferedImage {
        try {
            return ImageIO.read(path.toFile())
        } catch (e: IIOException) {
            throw Exception("Error reading file: ${path.absolutePathString()}", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is FileTemplate && path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

typealias FT = FileTemplate
