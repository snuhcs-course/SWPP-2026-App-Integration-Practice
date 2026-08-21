package com.example.escaperoom.data.model

/**
 * What the server is willing to tell the phone. This mirrors public_state() in
 * server/game.py field for field.
 *
 * Note what is NOT here: passcode, riddles. `entered` is safe — those are keys the
 * player pressed themselves, in front of the camera. If you ever see a passcode
 * field arrive on this side, the server is leaking; go and read public_state().
 */
data class SessionState(
    val session_id: String,
    val turns: Int,
    val turns_left: Int,
    val hints_given: Int,
    val entered: List<Int>,
    val digits_remaining: Int,
    val attempts: Int,
    val escaped: Boolean
)

/** One line of the transcript. */
data class Line(
    val speaker: String,
    val text: String,
    val hadPhoto: Boolean = false
)
