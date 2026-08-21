package com.example.escaperoom.data.network

import com.example.escaperoom.data.model.SessionState
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Seam 1 — the contract. Every field name here must match the JSON that
 * escaperoom/views.py returns, exactly. A renamed field does not throw: Gson
 * quietly leaves it null, and you find out three layers later.
 *
 * image_base64 is nullable. One endpoint, with or without a photo — the phone does
 * not need a second route just because the user pressed the shutter.
 */
data class SayRequest(
    val text: String,
    val image_base64: String? = null
)

data class SayResponse(
    val reply: String,
    val state: SessionState,
    val used_camera: Boolean
)

interface ApiService {

    @POST("escape/sessions/")
    suspend fun createSession(): SessionState

    @GET("escape/sessions/{id}/")
    suspend fun getState(@Path("id") sessionId: String): SessionState

    @POST("escape/sessions/{id}/say/")
    suspend fun say(
        @Path("id") sessionId: String,
        @Body request: SayRequest
    ): SayResponse
}
