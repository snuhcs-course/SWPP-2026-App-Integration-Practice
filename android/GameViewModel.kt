package com.swpp.escaperoom.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swpp.escaperoom.data.GameRepository
import com.swpp.escaperoom.data.SessionState
import kotlinx.coroutines.launch

/**
 * Seam 2 — the async boundary.
 *
 * MVVM, exactly as last year: the Activity observes LiveData, the ViewModel owns the
 * coroutine, the Repository owns the network call. The Activity never touches Retrofit.
 *
 * The happy path is not the interesting part. The interesting part is what the UI shows
 * while the Enigma is thinking — a VLM turn takes 2 to 5 seconds — and what it shows
 * when the server cannot be reached.
 */
data class Line(val speaker: String, val text: String, val hadPhoto: Boolean = false)

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableLiveData<SessionState>()
    val state: LiveData<SessionState> = _state

    private val _transcript = MutableLiveData<List<Line>>(emptyList())
    val transcript: LiveData<List<Line>> = _transcript

    private val _thinking = MutableLiveData(false)
    val thinking: LiveData<Boolean> = _thinking

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init { startSession() }

    private fun startSession() {
        viewModelScope.launch {
            try {
                _state.value = repository.createSession()
            } catch (e: Exception) {
                _error.value = "Could not reach the server. Is BASE_URL your LAN IP?"
            }
        }
    }

    /**
     * TODO-6: send one turn — text, and optionally the photo from CameraActivity.
     *
     *   1. append Line("you", text, imageBase64 != null)
     *   2. _thinking.value = true
     *   3. val response = repository.say(sessionId, text, imageBase64)   // suspend
     *   4. append Line("enigma", response.reply)
     *   5. _state.value = response.state
     *   6. _thinking.value = false  — in a finally block, or one thrown exception
     *      leaves the spinner on screen for the rest of the game
     *
     * Six lines. The judgement is where try / catch / finally go, and what the user
     * sees for the five seconds in between.
     */
    fun say(text: String, imageBase64: String? = null) {
        viewModelScope.launch {
            // TODO-6
        }
    }

    private fun append(line: Line) {
        _transcript.value = (_transcript.value ?: emptyList()) + line
    }
}
