package com.example.appinventario.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/** Respuesta de la API externa pública Open-Meteo. */
data class ClimaRespuestaDto(
    val current: ClimaActualDto?
)

data class ClimaActualDto(
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("wind_speed_10m") val viento: Double?,
    @SerializedName("weather_code") val codigoClima: Int?
)

/** Contrato Retrofit de la API externa (independiente de los microservicios propios). */
interface ClimaApiService {

    @GET("v1/forecast")
    suspend fun obtenerClimaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String = "temperature_2m,wind_speed_10m,weather_code",
        @Query("timezone") zonaHoraria: String = "auto"
    ): ClimaRespuestaDto
}
