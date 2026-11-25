package com.example.appinventario.repository

import com.example.appinventario.data.remote.ClimaApiService
import com.example.appinventario.util.ClimaUtil
import kotlinx.coroutines.CancellationException

/** Condiciones meteorológicas actuales en el punto de faena. */
data class ClimaActual(
    val temperaturaC: Double,
    val vientoKmH: Double,
    val descripcion: String
)

/** Repositorio de la API externa de clima, aislado de Room y de los microservicios propios. */
class ClimaRepository(private val api: ClimaApiService) {

    suspend fun obtenerClima(latitud: Double, longitud: Double): Result<ClimaActual> {
        return try {
            val actual = api.obtenerClimaActual(latitud, longitud).current
            val temperatura = actual?.temperatura
            if (actual == null || temperatura == null) {
                Result.failure(IllegalStateException("La API externa no entregó datos del clima"))
            } else {
                Result.success(
                    ClimaActual(
                        temperaturaC = temperatura,
                        vientoKmH = actual.viento ?: 0.0,
                        descripcion = ClimaUtil.describirCodigo(actual.codigoClima)
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
