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
        operator fun get(code: Long) = values().find { it.code == code }
    }
}
