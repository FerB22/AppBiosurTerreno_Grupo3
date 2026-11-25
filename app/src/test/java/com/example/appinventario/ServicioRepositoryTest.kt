package com.example.appinventario

import com.example.appinventario.model.Servicio
import com.example.appinventario.model.TipoServicio
import com.example.appinventario.repository.ServicioDao
import com.example.appinventario.repository.ServicioRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Pruebas de [ServicioRepository]: delegación hacia el DAO de Room (simulado con MockK). */
class ServicioRepositoryTest {

    private lateinit var dao: ServicioDao
    private lateinit var repositorio: ServicioRepository

    private val servicio = Servicio(
        id = 1,
        fecha = "25/11/2026",
        horaInicio = "08:00",
        horaTermino = "12:00",
        supervisor = "Fernando Barra Muñoz",
        tipoServicio = TipoServicio.FOSA_SEPTICA,
        descripcion = "Limpieza integral de fosa séptica",
        horasTrabajadas = 4.0,
        costoEstimado = 175000.0
    )

    @Before
    fun preparar() {
        dao = mockk(relaxed = true)
        every { dao.obtenerTodosLosServicios() } returns flowOf(listOf(servicio))
        repositorio = ServicioRepository(dao)
    }

    @Test
    fun todosLosServicios_emiteLaListaDelDao() = runTest {
        assertEquals(listOf(servicio), repositorio.todosLosServicios.first())
    }

    @Test
    fun obtenerPorId_devuelveElServicioDelDao() = runTest {
        coEvery { dao.obtenerServicioPorId(1) } returns servicio

        assertEquals(servicio, repositorio.obtenerPorId(1))
    }

    @Test
    fun obtenerPorId_inexistente_devuelveNulo() = runTest {
        coEvery { dao.obtenerServicioPorId(99) } returns null

        assertNull(repositorio.obtenerPorId(99))
    }

    @Test
    fun guardarServicio_insertaEnElDao() = runTest {
        repositorio.guardarServicio(servicio)

        coVerify(exactly = 1) { dao.insertarServicio(servicio) }
    }

    @Test
    fun actualizarServicio_actualizaEnElDao() = runTest {
        repositorio.actualizarServicio(servicio)

        coVerify(exactly = 1) { dao.actualizarServicio(servicio) }
    }

    @Test
    fun eliminarServicio_eliminaEnElDao() = runTest {
        repositorio.eliminarServicio(servicio)

        coVerify(exactly = 1) { dao.eliminarServicio(servicio) }
    }
}
