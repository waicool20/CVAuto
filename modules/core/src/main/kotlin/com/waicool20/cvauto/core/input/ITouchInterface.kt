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

    /**
     * List of touches on the device
     */
    val touches: List<Touch>

    /**
     * Sends a touch up event
     *
     * @param slot slot id for the touch
     */
    fun touchUp(slot: Int)

    /**
     * Sends a touch down event
     *
     * @param slot slot id for the touch
     */
    fun touchDown(slot: Int)

    /**
     * Sends a touch move event
     *
     * @param slot slot id for the touch
     * @param x x coordinate to move to
     * @param y y coordinate to move to
     */
    fun touchMove(slot: Int, x: Int, y: Int)

    /**
     * Commits the touch events
     */
    fun eventSync()
}
