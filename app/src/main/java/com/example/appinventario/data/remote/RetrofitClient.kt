package com.example.appinventario.data.remote

import android.os.Build
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Fábrica central de clientes Retrofit de Biosur Terreno.
 *
 * - Microservicios propios (Spring Boot): `ms-usuarios` en el puerto 8082 y `ms-servicios` en el 8081.
 * - API externa pública: Open-Meteo (clima actual), sin clave de acceso.
 *
 * El emulador de Android Studio accede al computador anfitrión mediante `10.0.2.2`;
 * un teléfono físico conectado por USB lo hace con `adb reverse` y `localhost`.
 */
object RetrofitClient {

    private val esEmulador: Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true)

    private val hostLocal: String = if (esEmulador) "10.0.2.2" else "localhost"

    val URL_MS_USUARIOS: String get() = "http://$hostLocal:8082/"
    val URL_MS_SERVICIOS: String get() = "http://$hostLocal:8081/"
    const val URL_OPEN_METEO = "https://api.open-meteo.com/"

    // Tiempos de espera cortos: sin red, la app recurre de inmediato a su respaldo local (offline-first).
    private val clienteHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private fun crearRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val usuarioApi: UsuarioApiService by lazy {
        crearRetrofit(URL_MS_USUARIOS).create(UsuarioApiService::class.java)
    }

    val climaApi: ClimaApiService by lazy {
        crearRetrofit(URL_OPEN_METEO).create(ClimaApiService::class.java)
    }
}
