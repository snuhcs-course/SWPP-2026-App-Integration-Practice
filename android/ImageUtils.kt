package com.swpp.escaperoom.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Seam 1, the part nobody expects — payload size.
 *
 * A modern phone camera gives you roughly 4000 x 3000. Encoded as JPEG that is about
 * 4 MB; Base64 adds another 33%, so you POST ~5.5 MB of text. On mobile data the
 * request appears to hang, then OkHttp times out, and it looks like a network bug.
 * Django will answer 413 long before the model ever sees it.
 *
 * Downscaling to 1024px costs you nothing the vision model can use: it is counting
 * the sides of a polygon, not reading fine print.
 *
 *     4000 x 3000 JPEG  ->  ~5.5 MB base64      server says 413
 *     1024 x  768 JPEG  ->  ~180 KB base64      fine
 */
object ImageUtils {

    private const val MAX_EDGE = 1024
    private const val JPEG_QUALITY = 80

    /**
     * TODO-5: turn the captured file into a Base64 string the server will accept.
     *
     *   1. BitmapFactory.decodeFile(file.absolutePath)
     *   2. downscale so that the longer edge is at most MAX_EDGE  (see downscale below)
     *   3. compress to JPEG at JPEG_QUALITY into a ByteArrayOutputStream
     *   4. Base64.encodeToString(bytes, Base64.NO_WRAP)
     *
     * NO_WRAP matters. The default inserts newlines every 76 characters and the JSON
     * body you send becomes invalid.
     */
    fun fileToBase64(file: File): String {
        // TODO-5
        return ""
    }

    /** Keeps the aspect ratio. Given for free — the interesting part is TODO-5. */
    fun downscale(src: Bitmap, maxEdge: Int = MAX_EDGE): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true
        )
    }

    /** Handy while debugging TODO-5: log this before you POST. */
    fun sizeKb(base64: String): Int = base64.length / 1024
}
