package com.example.escaperoom.data.repository

import com.example.escaperoom.data.model.SessionState
import com.example.escaperoom.data.network.RetrofitInstance
import com.example.escaperoom.data.network.SayRequest
import com.example.escaperoom.data.network.SayResponse

/**
 * The only class that knows Retrofit exists. Same role TodoRepository played in
 * SnapDo. Activities never call the network; the ViewModel calls this.
 */
class GameRepository {

    suspend fun createSession(): SessionState =
        RetrofitInstance.api.createSession()

    suspend fun say(sessionId: String, text: String, imageBase64: String?): SayResponse =
        RetrofitInstance.api.say(sessionId, SayRequest(text, imageBase64))
}
