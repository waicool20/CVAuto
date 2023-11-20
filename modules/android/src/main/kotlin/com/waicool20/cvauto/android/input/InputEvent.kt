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

package com.waicool20.cvauto.android.input

/**
 * Represents a linux kernel event, see
 * [Linux Input Events](https://github.com/torvalds/linux/blob/master/include/uapi/linux/input-event-codes.h)
 * for more information.
 *
 * @param code Code value of this event
 */
enum class InputEvent(val code: Long) {
    /* Key Events */
    KEY_ACTION_DOWN(0),
    KEY_ACTION_UP(1),

    /* Motion Events */
    MOTION_ACTION_DOWN(0),
    MOTION_ACTION_UP(1),
    MOTION_ACTION_MOVE(2),

    /* Motion Button */
    MOTION_BUTTON_PRIMARY(0x001),
    MOTION_BUTTON_SECONDARY(0x010),
    MOTION_BUTTON_TERTIARY(0x100);

    companion object {
        /**
         * Finds an InputEvent with the given code.
         *
         * @param code The code to look for
         * @return [InputEvent]
         */
        operator fun get(code: Long) = entries.find { it.code == code }
    }
}
