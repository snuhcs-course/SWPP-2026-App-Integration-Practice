package com.example.escaperoom.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * targetSdk 36 means the app is edge-to-edge whether it asks to be or not, so the
 * system draws the status bar, the navigation bar and the punch-hole camera on top
 * of your layout. `android:fitsSystemWindows` handles the bars but not the display
 * cutout, which is how a title ends up sliced in half by the camera.
 *
 * This pads the view by whatever the system is covering, plus your own padding.
 * Include the IME so the input row rises with the keyboard instead of hiding under it.
 */
fun View.padForSystemBars(extra: Int = 0) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val i = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.displayCutout()
                or WindowInsetsCompat.Type.ime()
        )
        v.setPadding(i.left + extra, i.top + extra, i.right + extra, i.bottom + extra)
        WindowInsetsCompat.CONSUMED
    }
}
