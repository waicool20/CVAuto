package com.waicool20.cvauto.core.input

interface IKeyboard {
    val heldKeys: List<String>

    fun keyUp(keyName: String)
    fun keyDown(keyName: String)

    fun checkSupport(keyName: String): Boolean
}
