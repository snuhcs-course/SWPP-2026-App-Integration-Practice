package com.example.escaperoom.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.escaperoom.data.model.Line
import com.example.escaperoom.data.model.SessionState
import com.example.escaperoom.data.repository.GameRepository
import kotlinx.coroutines.launch

/**
 * Seam 2 — the async boundary.
 *
 * The Activity observes LiveData, the ViewModel owns the coroutine, and the Repository
 * owns the network call. The Activity never touches Retrofit.
 *
 * The happy path is not the interesting part. The interesting part is what the UI
 * shows while the Enigma is working across one or more model calls, and what it shows
 * when the server cannot be reached.
 */
class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _state = MutableLiveData<SessionState>()
    val state: LiveData<SessionState> = _state

    private val _transcript = MutableLiveData<List<Line>>(emptyList())
    val transcript: LiveData<List<Line>> = _transcript

    private val _thinking = MutableLiveData(false)
    val thinking: LiveData<Boolean> = _thinking

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        startSession()
    }

    private fun startSession() {
        viewModelScope.launch {
            try {
                _state.value = repository.createSession()
                append(Line("enigma", "A lock, and a door. Speak, or show me something."))
            } catch (e: Exception) {
                Log.e("GameViewModel", "createSession failed", e)
                _error.value = "Could not reach the server.\n\n" +
                        "1. is it running?   python manage.py runserver 0.0.0.0:8000\n" +
                        "2. is BASE_URL your machine's LAN IP, not 127.0.0.1?\n" +
                        "3. same Wi-Fi?"
            }
        }
    }

    /**
     * TODO-6: send one turn — text, and optionally the photo from CameraActivity.
     *
     * Ignore the request until a session exists. Once it does, show the player's line
     * immediately (including whether it had a photo), expose a thinking state for exactly
     * the lifetime of the suspend network call, then append the Enigma reply and publish
     * the returned session state. Network failures must be logged and shown through
     * `error`, and no failure may leave the thinking indicator stuck on.
     */
    fun say(text: String, imageBase64: String? = null) {
        // TODO-6: today, read through the answer below and understand the shape
        // (show the line immediately, thinking=true for the suspend call, reply +
        // state on success, error on failure, thinking=false in finally no matter
        // what) rather than deriving it from scratch.
        //
        // val sessionId = _state.value?.session_id ?: return
        // viewModelScope.launch {
        //     append(Line("you", text, imageBase64 != null))
        //     _thinking.value = true                 // the turn takes 2-5 seconds
        //     try {
        //         val response = repository.say(sessionId, text, imageBase64)
        //         append(Line("enigma", response.reply))
        //         _state.value = response.state
        //     } catch (e: Exception) {
        //         Log.e("GameViewModel", "say failed", e)
        //         _error.value = "The Enigma did not answer. Is the server running?"
        //     } finally {
        //         _thinking.value = false            // <- the whole exercise
        //     }
        // }
    }

    private fun append(line: Line) {
        _transcript.value = (_transcript.value ?: emptyList()) + line
    }
}

/** Supplies the repository dependency when Android creates the ViewModel. */
class GameViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
