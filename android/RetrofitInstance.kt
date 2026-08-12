package com.swpp.escaperoom.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Seam 1 — where localhost stops being a thing.
 *
 * The emulator cannot reach "127.0.0.1"; that address belongs to the emulator itself.
 * Use your machine's LAN IP (mac: `ipconfig getifaddr en0`, win: `ipconfig`).
 * Android also blocks cleartext HTTP by default — see the note at the bottom.
 */
object RetrofitInstance {

    // TODO-3: replace with your own IP. Keep the trailing slash.
    private const val BASE_URL = "http://XXX.XXX.XXX.XXX:8000/"

    // An LLM turn is slow. The default 10s read timeout will cut it off mid-answer,
    // and the failure looks like a network bug rather than a timeout.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: EscapeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EscapeApi::class.java)
    }
}

/*
 * res/xml/network_security_config.xml  — required for plain HTTP in development:
 *
 *   <network-security-config>
 *     <domain-config cleartextTrafficPermitted="true">
 *       <domain includeSubdomains="true">XXX.XXX.XXX.XXX</domain>
 *     </domain-config>
 *   </network-security-config>
 *
 * AndroidManifest.xml:
 *   <application android:networkSecurityConfig="@xml/network_security_config" ... >
 *   <uses-permission android:name="android.permission.INTERNET"/>
 *   <uses-permission android:name="android.permission.CAMERA"/>
 */
