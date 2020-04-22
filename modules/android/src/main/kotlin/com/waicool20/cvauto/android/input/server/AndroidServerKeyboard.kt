package com.waicool20.cvauto.android.input.server

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.input.CharactersPerSecond
import com.waicool20.cvauto.core.input.IKeyboard

class AndroidServerKeyboard(
    private val device: AndroidDevice
): IKeyboard {
    data class AndroidServerKeyboardSettings(
        override var defaultTypingSpeed: CharactersPerSecond = 7,
        override var typingSpeedVariance: Double = 0.25
    ) : IKeyboard.Settings

    override val settings = AndroidServerKeyboardSettings()
    override val heldKeys: List<String>
        get() = TODO("Not yet implemented")

    override fun keyUp(keyName: String) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun keyDown(keyName: String) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun checkSupport(keyName: String): Boolean {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

    override fun type(string: String, speed: CharactersPerSecond) {
        throw UnsupportedOperationException("Not Implemented") // TODO Implement this function
    }

}