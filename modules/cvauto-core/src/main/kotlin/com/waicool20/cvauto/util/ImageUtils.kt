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

import java.awt.color.ColorSpace
import java.awt.image.*

object ImageUtils {
    @Throws(NegativeArraySizeException::class)
    fun createByteRGBBufferedImage(
        width: Int,
        height: Int,
        hasAlpha: Boolean = false
    ): BufferedImage {
        val cs = ColorSpace.getInstance(ColorSpace.CS_sRGB)
        val cm: ColorModel
        val raster: WritableRaster
        if (hasAlpha) {
            cm = ComponentColorModel(cs, true, false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE)
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null)
        } else {
            cm = ComponentColorModel(cs, false, false, ColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE)
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3, null)
        }
        return BufferedImage(cm, raster, false, null)
    }
}
