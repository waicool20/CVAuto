/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.cvauto.util

import java.awt.image.BufferedImage

/**
 * Creates a new buffered image with the same attributes but different width and height
 * Use this for custom type buffered images
 *
 * @param width Width of new buffered image, defaults to original width
 * @param height Height of new buffered image, defaults to original height
 */
fun BufferedImage.createCompatibleCopy(
    width: Int = this.width,
    height: Int = this.height
): BufferedImage {
    return BufferedImage(
        colorModel,
        raster.createCompatibleWritableRaster(width, height),
        isAlphaPremultiplied,
        null
    )
}
