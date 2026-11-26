package com.example.appinventario

import com.example.appinventario.model.EstadoServicio
import com.example.appinventario.model.ModoTema
import com.example.appinventario.model.Servicio
import com.example.appinventario.model.TipoServicio
import com.example.appinventario.repository.ClimaActual
import com.example.appinventario.repository.ClimaRepository
import com.example.appinventario.repository.ServicioRepository
import com.example.appinventario.viewmodel.ClimaUiState
import com.example.appinventario.viewmodel.ServicioViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de [ServicioViewModel]: validaciones por campo, cálculo de costos,
 * persistencia mediante el repositorio y consumo de la API externa de clima.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServicioViewModelTest {

    private lateinit var repositorio: ServicioRepository
    private lateinit var climaRepo: ClimaRepository

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repositorio = mockk(relaxed = true)
        every { repositorio.todosLosServicios } returns flowOf(emptyList())
        climaRepo = mockk()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearVm(conClima: Boolean = true) =
        ServicioViewModel(repositorio, if (conClima) climaRepo else null)

    private fun ServicioViewModel.llenarFormularioValido() {
        onDescripcionChange("Limpieza integral de fosa séptica")
        onHorasChange("4")
    }

    // ---------- Validaciones por campo ----------

    @Test
    fun onDescripcionChange_enBlanco_muestraError() {
        val vm = crearVm()
        vm.onDescripcionChange("   ")
        assertNotNull(vm.formularioState.value.errorDescripcion)
        assertFalse(vm.formularioState.value.isFormValido)
    }

    @Test
    fun onDescripcionChange_menosDeDiezCaracteres_muestraError() {
        val vm = crearVm()
        vm.onDescripcionChange("Corto")
        assertNotNull(vm.formularioState.value.errorDescripcion)
    }

    @Test
    fun onDescripcionChange_valida_limpiaElError() {
        val vm = crearVm()
        vm.onDescripcionChange("Corto")
        vm.onDescripcionChange("Descripción técnica suficiente")
        assertNull(vm.formularioState.value.errorDescripcion)
    }

    @Test
    fun onHorasChange_valoresInvalidos_muestranError() {
        val vm = crearVm()
        listOf("", "abc", "0", "-3", "25").forEach { entrada ->
            vm.onHorasChange(entrada)
            assertNotNull("Debe rechazar '$entrada'", vm.formularioState.value.errorHoras)
        }
    }

    @Test
    fun onHorasChange_valorEnRango_esValido() {
        val vm = crearVm()
        vm.onHorasChange("4.5")
        assertNull(vm.formularioState.value.errorHoras)
        assertEquals("4.5", vm.formularioState.value.horasTrabajadas)
    }

    @Test
    fun onCostoMaterialesChange_valoresInvalidos_muestranError() {
        val vm = crearVm()
        listOf("", "xyz", "-1", "50000001").forEach { entrada ->
            vm.onCostoMaterialesChange(entrada)
            assertNotNull("Debe rechazar '$entrada'", vm.formularioState.value.errorCostoMateriales)
        }
    }

    @Test
    fun onCostoMaterialesChange_cero_esValido() {
        val vm = crearVm()
        vm.onCostoMaterialesChange("0")
        assertNull(vm.formularioState.value.errorCostoMateriales)
    }

    @Test
    fun formulario_conTodosLosCamposValidos_quedaHabilitado() {
        val vm = crearVm()
        vm.llenarFormularioValido()
        assertTrue(vm.formularioState.value.isFormValido)
    }

    // ---------- Estado del formulario ----------

    @Test
    fun actualizacionesSimples_modificanElEstadoDelFormulario() {
        val vm = crearVm()
        vm.onTipoServicioChange(TipoServicio.EMERGENCIA)
        vm.onSupervisorChange("Esteban Medina")
        vm.onFotoAntesCaptured("content://antes")
        vm.onFotoDespuesCaptured("content://despues")
        vm.onUbicacionGpsCaptured(-37.5, -72.6)

        val estado = vm.formularioState.value
        assertEquals(TipoServicio.EMERGENCIA, estado.tipoServicio)
        assertEquals("Esteban Medina", estado.supervisor)
        assertEquals("content://antes", estado.fotoAntesUri)
        assertEquals("content://despues", estado.fotoDespuesUri)
        assertEquals(-37.5, estado.latitud!!, 0.0001)
        assertEquals(-72.6, estado.longitud!!, 0.0001)
    }

    @Test
    fun onModoTemaChange_actualizaElModoVisual() {
        val vm = crearVm()
        vm.onModoTemaChange(ModoTema.OSCURO)
        assertEquals(ModoTema.OSCURO, vm.modoTema.value)
    }

    // ---------- Persistencia ----------

    @Test
    fun registrarServicio_formularioValido_guardaCalculaCostoYReiniciaElFormulario() {
        val capturado = slot<Servicio>()
        coEvery { repositorio.guardarServicio(capture(capturado)) } returns Unit
        val vm = crearVm()
        vm.llenarFormularioValido()
        var notificado = false

        vm.registrarServicio { notificado = true }

        assertTrue(notificado)
        // 4 h × 35 000 CLP/h + 35 000 CLP de materiales por defecto
        assertEquals(175000.0, capturado.captured.costoEstimado, 0.001)
        assertEquals(4.0, capturado.captured.horasTrabajadas, 0.001)
        assertEquals(EstadoServicio.FINALIZADO, capturado.captured.estado)
        assertEquals("", vm.formularioState.value.descripcion)
    }

    @Test
    fun registrarServicio_formularioInvalido_noGuardaNada() {
        val vm = crearVm()
        var notificado = false

        vm.registrarServicio { notificado = true }

        assertFalse(notificado)
        coVerify(exactly = 0) { repositorio.guardarServicio(any()) }
    }

    @Test
    fun actualizarEstadoServicio_estadoDistinto_actualizaEnRepositorio() {
        val vm = crearVm()
        val orden = Servicio(
            id = 1,
            fecha = "25/11/2026",
            horaInicio = "08:00",
            horaTermino = "12:00",
            supervisor = "Fernando Barra",
            tipoServicio = TipoServicio.FOSA_SEPTICA,
            descripcion = "Limpieza de fosa séptica en sector sur",
            horasTrabajadas = 4.0,
            costoEstimado = 175000.0,
            estado = EstadoServicio.FINALIZADO
        )

        vm.actualizarEstadoServicio(orden, EstadoServicio.PENDIENTE_REVISION)

        coVerify(exactly = 1) {
            repositorio.actualizarServicio(orden.copy(estado = EstadoServicio.PENDIENTE_REVISION))
        }
    }

    @Test
    fun actualizarEstadoServicio_mismoEstado_noLlamaAlRepositorio() {
        val vm = crearVm()
        val orden = Servicio(
            id = 1,
            fecha = "25/11/2026",
            horaInicio = "08:00",
            horaTermino = "12:00",
            supervisor = "Fernando Barra",
            tipoServicio = TipoServicio.FOSA_SEPTICA,
            descripcion = "Limpieza de fosa séptica en sector sur",
            horasTrabajadas = 4.0,
            costoEstimado = 175000.0,
            estado = EstadoServicio.FINALIZADO
        )

        vm.actualizarEstadoServicio(orden, EstadoServicio.FINALIZADO)

        coVerify(exactly = 0) { repositorio.actualizarServicio(any()) }
    }

    @Test
    fun eliminarServicio_eliminaEnRepositorioYNotificaCallback() {
        val vm = crearVm()
        val orden = Servicio(
            id = 1,
            fecha = "25/11/2026",
            horaInicio = "08:00",
            horaTermino = "12:00",
            supervisor = "Fernando Barra",
            tipoServicio = TipoServicio.FOSA_SEPTICA,
            descripcion = "Limpieza de fosa séptica en sector sur",
            horasTrabajadas = 4.0,
            costoEstimado = 175000.0
        )
        var eliminado = false

        vm.eliminarServicio(orden) { eliminado = true }

        assertTrue(eliminado)
        coVerify(exactly = 1) { repositorio.eliminarServicio(orden) }
    }

    // ---------- API externa de clima ----------

    @Test
    fun cargarClima_conRespuestaExitosa_publicaEstadoDisponible() {
        coEvery { climaRepo.obtenerClima(any(), any()) } returns
            Result.success(ClimaActual(13.2, 8.0, "Lluvia"))
        val vm = crearVm()

        vm.cargarClima()

        val estado = vm.clima.value
        assertTrue(estado is ClimaUiState.Disponible)
        assertEquals("Lluvia", (estado as ClimaUiState.Disponible).descripcion)
    }

    @Test
    fun cargarClima_sinCoordenadas_usaLasPorDefecto() {
        coEvery { climaRepo.obtenerClima(any(), any()) } returns Result.success(ClimaActual(10.0, 5.0, "Despejado"))
        val vm = crearVm()

        vm.cargarClima()

        coVerify {
            climaRepo.obtenerClima(ServicioViewModel.LATITUD_DEFECTO, ServicioViewModel.LONGITUD_DEFECTO)
        }
    }

    @Test
    fun cargarClima_conCoordenadasDeFaena_lasUtiliza() {
        coEvery { climaRepo.obtenerClima(any(), any()) } returns Result.success(ClimaActual(10.0, 5.0, "Despejado"))
        val vm = crearVm()

        vm.cargarClima(-36.8, -73.0)

        coVerify { climaRepo.obtenerClima(-36.8, -73.0) }
    }

    @Test
    fun cargarClima_conFalla_publicaEstadoDeError() {
        coEvery { climaRepo.obtenerClima(any(), any()) } returns Result.failure(RuntimeException("Sin red"))
        val vm = crearVm()

        vm.cargarClima()

        assertTrue(vm.clima.value is ClimaUiState.Error)
    }

    @Test
    fun cargarClima_sinRepositorio_publicaEstadoDeError() {
        val vm = crearVm(conClima = false)

        vm.cargarClima()

        assertTrue(vm.clima.value is ClimaUiState.Error)
    }
}
