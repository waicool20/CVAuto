package com.waicool20.cvauto.android.input

/**
 * Represents a linux kernel event, see
 * [Linux Input Events](https://github.com/torvalds/linux/blob/master/include/uapi/linux/input-event-codes.h)
 * for more information.
 *
 * @param code Code value of this event
 */
enum class InputEvent(val code: Long) {
    /* Synchronization Events */
    SYN_REPORT(0),
    SYN_CONFIG(1),
    SYN_MT_REPORT(2),
    SYN_DROPPED(3),
    SYN_MAX(0xf),
    SYN_CNT(SYN_MAX.code + 1),

    /* Touch Events */
    ABS_MT_SLOT(0x2f),
    ABS_MT_TOUCH_MAJOR(0x30),
    ABS_MT_TOUCH_MINOR(0x31),
    ABS_MT_WIDTH_MAJOR(0x32),
    ABS_MT_WIDTH_MINOR(0x33),
    ABS_MT_ORIENTATION(0x34),
    ABS_MT_POSITION_X(0x35),
    ABS_MT_POSITION_Y(0x36),
    ABS_MT_TOOL_TYPE(0x37),
    ABS_MT_BLOB_ID(0x38),
    ABS_MT_TRACKING_ID(0x39),
    ABS_MT_PRESSURE(0x3a),
    ABS_MT_DISTANCE(0x3b),
    ABS_MT_TOOL_X(0x3c),
    ABS_MT_TOOL_Y(0x3d),

    /* Tool Events */
    BTN_TOUCH(0x14a),
    BTN_TOOL_FINGER(0x145),

    /* Key Events */
    KEY_UP(0),
    KEY_DOWN(1),
    ACTION_DOWN(0),
    ACTION_UP(1);

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
