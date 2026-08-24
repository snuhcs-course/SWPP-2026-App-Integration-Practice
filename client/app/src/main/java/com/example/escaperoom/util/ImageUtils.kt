package com.example.escaperoom.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/**
 * Seam 1, the part nobody expects — payload size.
 *
 * A phone camera gives you roughly 4000 x 3000. As JPEG that is about 4 MB; Base64
 * adds another 33%, so you POST ~5.5 MB of text. On mobile data the request appears
 * to hang, then OkHttp times out, and it looks like a network bug. Django answers
 * 413 long before the model ever sees it.
 *
 *     4000 x 3000 JPEG  ->  ~5.5 MB base64      server says 413
 *     1024 x  768 JPEG  ->  ~180 KB base64      fine
 *
 * Downscaling costs the vision model nothing it can use: it is counting the sides of
 * a polygon, not reading fine print.
 */
object ImageUtils {

    private const val MAX_EDGE = 1024
    private const val JPEG_QUALITY = 80

    /**
     * TODO-5: turn the captured file into a Base64 string the server will accept.
     *
     * Decode the captured image, preserve its aspect ratio while limiting the longest
     * edge to MAX_EDGE, and encode a JPEG at JPEG_QUALITY. The returned Base64 must be a
     * single unwrapped string. Return only the raw payload: the server, not the client,
     * adds any media-type prefix.
     */
    fun fileToBase64(file: File): String {
        // TODO-5
        return ""
    }

    /** Keeps the aspect ratio. Given for free — the interesting part is above. */
    fun downscale(src: Bitmap, maxEdge: Int = MAX_EDGE): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src, (src.width * ratio).toInt(), (src.height * ratio).toInt(), true
        )
    }

    /** Log this before you POST. Expect about 180, not 5500. */
    fun sizeKb(base64: String): Int = base64.length / 1024
}
