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

package com.waicool20.cvauto.core

import com.waicool20.cvauto.core.input.IInput

/**
 * Cached device, this variant of device is just used to wrap the device of a [CachedRegion],
 * getting the list of screens will return an empty list.
 */
class CachedDevice<T : IDevice<T, R>, R : Region<T, R>>(val device: IDevice<T, R>) :
    IDevice<CachedDevice<T, R>, CachedRegion<T, R>> {
    override val input: IInput get() = device.input
    override val screens: List<CachedRegion<T, R>> = emptyList()
}