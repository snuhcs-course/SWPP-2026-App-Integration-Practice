package com.swpp.escaperoom.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.swpp.escaperoom.R
import com.swpp.escaperoom.util.ImageUtils
import java.io.File

/**
 * The camera screen. This is the keypad: point it at the polygon you want to press
 * and hit the shutter. A hexagon presses 6.
 *
 * Same CameraX shape as last year, so this should feel familiar. Two TODOs.
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        previewView = findViewById(R.id.previewView)

        startCamera()

        findViewById<Button>(R.id.btnSwitch).setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            startCamera()
        }
        findViewById<Button>(R.id.btnShutter).setOnClickListener { capture() }
    }

    /**
     * TODO-4: bring up the preview.
     *
     *   1. ProcessCameraProvider.getInstance(this) — it returns a ListenableFuture
     *   2. addListener(..., ContextCompat.getMainExecutor(this))
     *   3. build a Preview and call setSurfaceProvider(previewView.surfaceProvider)
     *   4. build an ImageCapture and keep it in `imageCapture`
     *   5. provider.unbindAll(), then bindToLifecycle(this, selector, preview, imageCapture)
     *
     * unbindAll() is the line students forget. Without it, switching the lens binds a
     * second use case to the same lifecycle and CameraX throws.
     */
    private fun startCamera() {
        // TODO-4
    }

    /**
     * Capture, encode, hand back. The Activity does not talk to Retrofit — it returns
     * the string and the ViewModel sends it. Seam 2 stays intact.
     */
    private fun capture() {
        val capture = imageCapture ?: return
        val file = File(cacheDir, "shape.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val b64 = ImageUtils.fileToBase64(file)      // TODO-5 lives here
                    setResult(RESULT_OK, intent.putExtra("image_base64", b64))
                    finish()
                }

                override fun onError(e: ImageCaptureException) {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        )
    }
}

/*
 * AndroidManifest.xml
 *   <uses-permission android:name="android.permission.CAMERA"/>
 *   <uses-permission android:name="android.permission.INTERNET"/>
 *
 * The emulator has a fake camera. Set Camera > Back to "VirtualScene" and you can
 * point it at a wall poster — or just show it a shape on your screen.
 */
