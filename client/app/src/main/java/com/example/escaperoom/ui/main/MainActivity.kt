package com.example.escaperoom.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.escaperoom.R
import com.example.escaperoom.data.repository.GameRepository
import com.example.escaperoom.ui.camera.CameraActivity
import com.example.escaperoom.util.padForSystemBars

/**
 * The room. A transcript, a text box, and a camera button.
 *
 * CameraActivity hands back a Base64 string and this Activity passes it to the
 * ViewModel. Keeping network access in the repository lets the camera and network
 * boundaries be tested separately.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TranscriptAdapter

    private val viewModel: GameViewModel by viewModels {
        GameViewModelFactory(GameRepository())
    }

    /** CameraActivity returns the encoded photo; we send it with the next line. */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val b64 = result.data?.getStringExtra("image_base64")
            if (b64.isNullOrEmpty()) {
                Toast.makeText(this, "The camera returned nothing — is TODO-5 done?",
                    Toast.LENGTH_LONG).show()
            } else {
                viewModel.say("I am holding it up to the camera.", b64)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 16dp of our own padding on top of whatever the system is covering.
        findViewById<View>(R.id.root).padForSystemBars(
            (16 * resources.displayMetrics.density).toInt()
        )

        val recycler = findViewById<RecyclerView>(R.id.recyclerTranscript)
        adapter = TranscriptAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        val input = findViewById<EditText>(R.id.etInput)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val btnCamera = findViewById<Button>(R.id.btnCamera)
        val spinner = findViewById<ProgressBar>(R.id.progressThinking)
        val status = findViewById<TextView>(R.id.tvStatus)
        val emptyHint = findViewById<TextView>(R.id.tvEmptyHint)

        btnSend.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.say(text)
                input.setText("")
            }
        }

        btnCamera.setOnClickListener {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
        }

        viewModel.transcript.observe(this) { lines ->
            adapter.updateData(lines)
            if (lines.isNotEmpty()) {
                emptyHint.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                recycler.scrollToPosition(lines.size - 1)
            }
        }

        viewModel.thinking.observe(this) { thinking ->
            spinner.visibility = if (thinking) View.VISIBLE else View.GONE
            btnSend.isEnabled = !thinking
            btnCamera.isEnabled = !thinking          // two taps, two coroutines
        }

        viewModel.state.observe(this) { s ->
            if (s.escaped) {
                // Reserve the primary color for the successful escape state.
                status.text = "the door is open  ·  ${s.turns} turns"
                status.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                val pressed = if (s.entered.isEmpty()) "—" else s.entered.joinToString("")
                status.text = "$pressed  ·  ${s.digits_remaining} keys left  ·  " +
                        "${s.turns_left} turns"
                status.setTextColor(ContextCompat.getColor(this, R.color.muted))
            }
        }

        // An error has to survive on screen. A Toast that vanishes after three
        // seconds is how a student spends twenty minutes on a wrong BASE_URL.
        viewModel.error.observe(this) { message ->
            if (message != null) {
                status.text = getString(R.string.no_server)
                status.setTextColor(ContextCompat.getColor(this, R.color.accent_rose))
                emptyHint.text = message
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
