package com.waicool20.cvauto.core.input

interface IKeyboard {
    /**
     * List of the names of held keys eg. A, B, COMMA, ALT etc.
     */
    val heldKeys: List<String>

    /**
     * Send a key up event with given key
     *
     * @param keyName Name of key eg. A, B, COMMA, ALT etc.
     */
    fun keyUp(keyName: String)

    /**
     * Send a key down event with given key
     *
     * @param keyName Name of key eg. A, B, COMMA, ALT etc.
     */
    fun keyDown(keyName: String)

    /**
     * Checks if the key name is valid and supported
     *
     * @return true if the key can be typed into the device
     */
    fun checkSupport(keyName: String): Boolean

    /**
     * Types the given string
     *
     * @param string String to type
     */
    fun type(string: String)
}
