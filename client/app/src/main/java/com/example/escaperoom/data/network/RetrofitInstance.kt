package com.example.escaperoom.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Seam 1 — where localhost stops being a thing.
 *
 * The emulator cannot reach "127.0.0.1"; that address belongs to the emulator itself.
 * Use your machine's LAN IP (mac: `ipconfig getifaddr en0`, win: `ipconfig`), and make
 * sure the phone and the laptop are on the same Wi-Fi.
 *
 * Cleartext HTTP is already allowed: AndroidManifest sets usesCleartextTraffic="true".
 */
object RetrofitInstance {

    // TODO-3: replace with your own IP. Keep exactly one trailing slash.
    private const val BASE_URL = "http://XXX.XXX.XXX.XXX:8000/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC   // BODY prints 180 KB of base64
    }

    // TODO-3, the half everyone forgets. A turn takes 2-5 seconds because a model is
    // thinking. OkHttp's default read timeout is 10s, and when it fires the failure
    // looks like a network bug rather than a timeout.
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
