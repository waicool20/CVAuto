package com.waicool20.cvauto.android.input

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.input.IInput
import com.waicool20.cvauto.core.input.IKeyboard

inline class DeviceFile(val path: String)

class AndroidInput internal constructor(device: AndroidDevice) : IInput {
    override val mouse = null
    override val keyboard: IKeyboard?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val touchInterface = AndroidTouchInterface.getForDevice(device)
}
