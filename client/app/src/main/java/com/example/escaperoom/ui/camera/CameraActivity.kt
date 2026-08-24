package com.example.escaperoom.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.escaperoom.R
import com.example.escaperoom.util.ImageUtils
import com.example.escaperoom.util.padForSystemBars
import java.io.File

/**
 * The keypad. Point it at the polygon you want to press and hit the shutter —
 * a hexagon presses 6.
 *
 * This screen does not call the network. It encodes the photo and hands the string
 * back through setResult.
 * MainActivity gives it to the ViewModel, and the ViewModel talks to the server.
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var capturedImage: ImageView
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var capturedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val container = findViewById<FrameLayout>(R.id.cameraPreview)
        previewView = PreviewView(this)
        container.addView(previewView)

        capturedImage = findViewById(R.id.capturedImage)
        // Only the button rows dodge the cutout; the preview stays full-bleed.
        findViewById<android.view.View>(R.id.topButtons).padForSystemBars(
            (16 * resources.displayMetrics.density).toInt()
        )
        findViewById<android.view.View>(R.id.bottomButtons).padForSystemBars(
            (16 * resources.displayMetrics.density).toInt()
        )
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnUse = findViewById<Button>(R.id.btnUsePhoto)
        val btnRetake = findViewById<Button>(R.id.btnRetake)
        val btnSwitch = findViewById<Button>(R.id.btnSwitchCamera)
        val btnBack = findViewById<Button>(R.id.btnGoBack)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1000)
        } else {
            startCamera()
        }

        btnCapture.setOnClickListener {
            val capture = imageCapture
            if (capture == null) {
                Toast.makeText(this, "No camera bound yet — is TODO-4 done?",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val photoFile = File(externalCacheDir, "shape_${System.currentTimeMillis()}.jpg")
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        capturedFile = photoFile
                        capturedImage.setImageURI(
                            output.savedUri ?: android.net.Uri.fromFile(photoFile)
                        )
                        capturedImage.visibility = ImageView.VISIBLE
                        previewView.visibility = PreviewView.GONE

                        btnCapture.visibility = Button.GONE
                        btnUse.visibility = Button.VISIBLE
                        btnRetake.visibility = Button.VISIBLE
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Toast.makeText(applicationContext,
                            "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        btnRetake.setOnClickListener {
            capturedFile = null
            capturedImage.visibility = ImageView.GONE
            previewView.visibility = PreviewView.VISIBLE
            btnCapture.visibility = Button.VISIBLE
            btnUse.visibility = Button.GONE
            btnRetake.visibility = Button.GONE
            startCamera()
        }

        // Encode here, send nowhere. Seam 2 stays intact.
        btnUse.setOnClickListener {
            val file = capturedFile ?: return@setOnClickListener
            val b64 = ImageUtils.fileToBase64(file)            // TODO-5 lives here
            android.util.Log.d("CameraActivity", "payload ${ImageUtils.sizeKb(b64)} KB")
            setResult(RESULT_OK, intent.putExtra("image_base64", b64))
            finish()
        }

        btnSwitch.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            startCamera()
        }

        btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /**
     * TODO-4: bind a working preview and still-image capture for `lensFacing`.
     *
     * The preview must render into `previewView`, the shutter must receive a non-null
     * `imageCapture`, and calling this method again after switching lenses must replace
     * the old CameraX bindings instead of stacking another set on the lifecycle. Camera
     * provider callbacks belong on the main executor.
     */
    private fun startCamera() {
        // TODO-4
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }
}

/*
 * Emulator: Settings > Camera > Back = VirtualScene, then point it at a shape on your
 * screen. figures/shapes/ in the lab repo has one clean PNG per key.
 */
