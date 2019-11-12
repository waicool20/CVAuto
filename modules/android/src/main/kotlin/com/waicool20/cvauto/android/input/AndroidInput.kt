package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.input.IInput

inline class DeviceFile(val path: String)

class AndroidInput internal constructor(device: AndroidDevice) : IInput {
    override val mouse = null
    override val keyboard = AndroidKeyboard.getForDevice(device)
    override val touchInterface = AndroidTouchInterface.getForDevice(device)
}
