package com.swpp.escaperoom.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Seam 1 — the contract between the phone and Django.
 *
 * Every field below must match the JSON that views.py produces. When it does not,
 * Retrofit hands you a null and the bug surfaces three layers away, in the UI.
 * That is why this file and views.py are read side by side in Exercise 3.
 */
interface EscapeApi {

    @POST("escape/sessions/")
    suspend fun createSession(): SessionState

    @GET("escape/sessions/{id}/")
    suspend fun getState(@Path("id") sessionId: String): SessionState

    /** One endpoint, with or without a photo. imageBase64 is null on a normal turn. */
    @POST("escape/sessions/{id}/say/")
    suspend fun say(
        @Path("id") sessionId: String,
        @Body body: SayRequest
    ): SayResponse
}

data class SayRequest(
    val text: String,
    val image_base64: String? = null      // snake_case: it has to match Django
)

data class SessionState(
    val session_id: String,
    val turns: Int,
    val turns_left: Int,
    val hints_given: Int,
    val entered: List<Int>,          // keys pressed so far - the player showed them
    val digits_remaining: Int,
    val attempts: Int,
    val escaped: Boolean
    // Note what is absent: passcode, riddles. If you ever see them here, the server
    // is leaking - go and look at public_state().
)

data class SayResponse(
    val reply: String,
    val state: SessionState,
    val used_camera: Boolean
)
