package com.example.appinventario

import com.example.appinventario.data.remote.ClimaActualDto
import com.example.appinventario.data.remote.ClimaApiService
import com.example.appinventario.data.remote.ClimaRespuestaDto
import com.example.appinventario.repository.ClimaRepository
import com.example.appinventario.util.ClimaUtil
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Pruebas de la integración con la API externa de clima (Open-Meteo), simulada con MockK. */
class ClimaRepositoryTest {

    private lateinit var api: ClimaApiService
    private lateinit var repositorio: ClimaRepository

    @Before
    fun preparar() {
        api = mockk()
        repositorio = ClimaRepository(api)
    }

    @Test
    fun obtenerClima_respuestaCompleta_traduceLosDatos() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } returns
            ClimaRespuestaDto(ClimaActualDto(temperatura = 14.5, viento = 12.0, codigoClima = 3))

        val resultado = repositorio.obtenerClima(-37.5, -72.6)

        assertTrue(resultado.isSuccess)
        val clima = resultado.getOrThrow()
        assertEquals(14.5, clima.temperaturaC, 0.001)
        assertEquals(12.0, clima.vientoKmH, 0.001)
        assertEquals("Nublado", clima.descripcion)
    }

    @Test
    fun obtenerClima_sinVientoNiCodigo_usaValoresPorDefecto() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } returns
            ClimaRespuestaDto(ClimaActualDto(temperatura = 20.0, viento = null, codigoClima = null))

        val clima = repositorio.obtenerClima(0.0, 0.0).getOrThrow()

        assertEquals(0.0, clima.vientoKmH, 0.001)
        assertEquals("Sin datos", clima.descripcion)
    }

    @Test
    fun obtenerClima_sinBloqueCurrent_retornaFalla() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } returns ClimaRespuestaDto(null)

        assertTrue(repositorio.obtenerClima(0.0, 0.0).isFailure)
    }

    @Test
    fun obtenerClima_sinTemperatura_retornaFalla() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } returns
            ClimaRespuestaDto(ClimaActualDto(temperatura = null, viento = 5.0, codigoClima = 0))

        assertTrue(repositorio.obtenerClima(0.0, 0.0).isFailure)
    }

    @Test
    fun obtenerClima_fallaDeRed_retornaFallaSinLanzarExcepcion() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } throws IOException("Sin red")

        assertFalse(repositorio.obtenerClima(0.0, 0.0).isSuccess)
    }

    @Test(expected = CancellationException::class)
    fun obtenerClima_cancelacion_sePropagaSinCapturarse() = runTest {
        coEvery { api.obtenerClimaActual(any(), any(), any(), any()) } throws CancellationException("cancelado")

        repositorio.obtenerClima(0.0, 0.0)
    }

    @Test
    fun describirCodigo_cubreLosRangosMeteorologicos() {
        assertEquals("Sin datos", ClimaUtil.describirCodigo(null))
        assertEquals("Despejado", ClimaUtil.describirCodigo(0))
        assertEquals("Parcialmente nublado", ClimaUtil.describirCodigo(2))
        assertEquals("Nublado", ClimaUtil.describirCodigo(3))
        assertEquals("Niebla", ClimaUtil.describirCodigo(45))
        assertEquals("Llovizna", ClimaUtil.describirCodigo(53))
        assertEquals("Lluvia", ClimaUtil.describirCodigo(63))
        assertEquals("Nieve", ClimaUtil.describirCodigo(73))
        assertEquals("Chubascos", ClimaUtil.describirCodigo(81))
        assertEquals("Tormenta eléctrica", ClimaUtil.describirCodigo(96))
        assertEquals("Condición variable", ClimaUtil.describirCodigo(42))
    }
}
