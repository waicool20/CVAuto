package com.waicool20.cvauto.core.input

interface ITouchInterface {
    /**
     * Represents the state of a touch
     *
     * @param cursorX x coordinate of this touch
     * @param cursorY y coordinate of this touch
     * @param isTouching True if screen is touched at this coordinate
     */
    data class Touch(
        val slot: Int,
        var cursorX: Int = 0,
        var cursorY: Int = 0,
        var isTouching: Boolean = false
    )

    val touches: List<Touch>
    fun touchUp(slot: Int)
    fun touchDown(slot: Int)
    fun touchMove(slot: Int, x: Int, y: Int)
}
