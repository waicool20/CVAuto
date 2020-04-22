package com.waicool20.cvauto.android.input

object KeyUtils {
    private val shiftChars = "~!@#$%^&*()_+{}|:\"<>?"

    /**
     * Checks if a given character needs shift to be pressed to be typed
     *
     * @param char Character to check
     * @return true if shift is needed
     */
    fun requiresShift(char: Char) = char.isUpperCase() || shiftChars.contains(char)
}