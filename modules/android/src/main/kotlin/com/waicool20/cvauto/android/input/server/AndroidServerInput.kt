package com.waicool20.cvauto.android.input.server

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.input.IInput

class AndroidServerInput internal constructor(device: AndroidDevice) : IInput {
    companion object {
        const val INPUT_TOUCH = 0
        const val INPUT_KEY = 0
    }

    override val mouse = null
    override val keyboard = AndroidServerKeyboard(device)
    override val touchInterface = AndroidServerTouchInterface(device)
}
