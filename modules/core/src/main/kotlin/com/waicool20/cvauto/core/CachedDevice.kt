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
