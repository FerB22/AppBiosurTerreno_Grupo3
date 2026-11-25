package com.example.appinventario

import com.example.appinventario.model.ModoTema
import com.example.appinventario.model.RolUsuario
import com.example.appinventario.model.Usuario
import com.example.appinventario.repository.LoginRemotoResult
import com.example.appinventario.repository.UsuarioRemoteRepository
import com.example.appinventario.util.UserPreferencesManager
import com.example.appinventario.viewmodel.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 * Pruebas unitarias de [AuthViewModel]: validación local (Default Deny) y autenticación contra
 * el microservicio ms-usuarios, simulado con MockK, incluyendo el respaldo sin conexión.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var preferencias: UserPreferencesManager
    private lateinit var remoto: UsuarioRemoteRepository

    private val emailValido = "fernando.barra@biosur.cl"
    private val passwordValida = "biosur2026"

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        preferencias = mockk(relaxed = true)
        every { preferencias.modoTema } returns flowOf(ModoTema.SISTEMA)
        every { preferencias.sesionActiva } returns flowOf(false)
        remoto = mockk()
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun AuthViewModel.ingresar(email: String = emailValido, password: String = passwordValida) {
        onEmailChange(email)
        onPasswordChange(password)
        iniciarSesion()
    }

    @Test
    fun iniciarSesion_servidorAceptaCuenta_usaUsuarioRemotoYPersisteSesion() {
        val usuarioServidor = Usuario(emailValido, "Fernando Barra Muñoz", RolUsuario.SUPERVISOR_TERRENO)
        coEvery { remoto.login(emailValido, "Fernando Barra") } returns LoginRemotoResult.Exitoso(usuarioServidor)
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar()

        val estado = vm.uiState.value
        assertTrue(estado.loginExitoso)
        assertTrue(estado.sincronizadoConServidor)
        assertFalse(estado.isLoading)
        assertEquals("Fernando Barra Muñoz", estado.usuarioAutenticado?.nombre)
        coVerify(exactly = 1) { preferencias.guardarSesionUsuario(emailValido) }
    }

    @Test
    fun iniciarSesion_servidorRechazaCuenta_muestraErrorYNoPersisteSesion() {
        coEvery { remoto.login(any(), any()) } returns LoginRemotoResult.Rechazado("El servidor denegó el acceso")
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar()

        val estado = vm.uiState.value
        assertFalse(estado.loginExitoso)
        assertFalse(estado.isLoading)
        assertEquals("El servidor denegó el acceso", estado.errorGeneral)
        assertNull(estado.usuarioAutenticado)
        coVerify(exactly = 0) { preferencias.guardarSesionUsuario(any()) }
    }

    @Test
    fun iniciarSesion_sinConexion_continuaConRespaldoLocal() {
        coEvery { remoto.login(any(), any()) } returns LoginRemotoResult.SinConexion
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar()

        val estado = vm.uiState.value
        assertTrue(estado.loginExitoso)
        assertFalse(estado.sincronizadoConServidor)
        assertEquals("Fernando Barra", estado.usuarioAutenticado?.nombre)
        assertEquals(RolUsuario.SUPERVISOR_TERRENO, estado.usuarioAutenticado?.rol)
        coVerify(exactly = 1) { preferencias.guardarSesionUsuario(emailValido) }
    }

    @Test
    fun iniciarSesion_sinRepositorioRemoto_autenticaSoloConValidacionLocal() {
        val vm = AuthViewModel(preferencias)

        vm.ingresar(email = "gilda.orellana@administracion.biosur.cl")

        val estado = vm.uiState.value
        assertTrue(estado.loginExitoso)
        assertEquals(RolUsuario.ADMINISTRADOR, estado.usuarioAutenticado?.rol)
    }

    @Test
    fun iniciarSesion_dominioNoAutorizado_deniegaYNoConsultaAlServidor() {
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar(email = "intruso@gmail.com")

        val estado = vm.uiState.value
        assertFalse(estado.loginExitoso)
        assertNotNull(estado.errorEmail)
        assertNotNull(estado.errorGeneral)
        coVerify(exactly = 0) { remoto.login(any(), any()) }
    }

    @Test
    fun iniciarSesion_passwordCorta_muestraErrorYNoConsultaAlServidor() {
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar(password = "123")

        val estado = vm.uiState.value
        assertFalse(estado.loginExitoso)
        assertNotNull(estado.errorPassword)
        coVerify(exactly = 0) { remoto.login(any(), any()) }
    }

    @Test
    fun iniciarSesion_passwordEnBlanco_muestraError() {
        val vm = AuthViewModel(preferencias, remoto)

        vm.ingresar(password = "      ")

        assertNotNull(vm.uiState.value.errorPassword)
        assertFalse(vm.uiState.value.loginExitoso)
    }

    @Test
    fun onEmailChange_infiereRolSegunDominio() {
        val vm = AuthViewModel(preferencias)

        vm.onEmailChange("x@biosur.cl")
        assertEquals(RolUsuario.SUPERVISOR_TERRENO, vm.uiState.value.rolDetectado)

        vm.onEmailChange("x@administracion.biosur.cl")
        assertEquals(RolUsuario.ADMINISTRADOR, vm.uiState.value.rolDetectado)

        vm.onEmailChange("x@otro.com")
        assertNull(vm.uiState.value.rolDetectado)
    }

    @Test
    fun usarCuentaDemo_cargaCorreoYContrasenaDeDemostracion() {
        val vm = AuthViewModel(preferencias)

        vm.usarCuentaDemo("esteban.medina@biosur.cl")

        assertEquals("esteban.medina@biosur.cl", vm.uiState.value.email)
        assertEquals("biosur2026", vm.uiState.value.password)
    }

    @Test
    fun onTogglePasswordVisibility_alternaLaVisibilidad() {
        val vm = AuthViewModel(preferencias)

        assertFalse(vm.uiState.value.passwordVisible)
        vm.onTogglePasswordVisibility()
        assertTrue(vm.uiState.value.passwordVisible)
        vm.onTogglePasswordVisibility()
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test
    fun onLoginHandled_reiniciaLaBanderaDeLoginExitoso() {
        val vm = AuthViewModel(preferencias)
        vm.ingresar()
        assertTrue(vm.uiState.value.loginExitoso)

        vm.onLoginHandled()

        assertFalse(vm.uiState.value.loginExitoso)
    }

    @Test
    fun cerrarSesion_limpiaElEstadoYLaSesionPersistida() {
        val vm = AuthViewModel(preferencias)
        vm.ingresar()

        vm.cerrarSesion()

        assertEquals("", vm.uiState.value.email)
        assertNull(vm.uiState.value.usuarioAutenticado)
        coVerify(exactly = 1) { preferencias.cerrarSesion() }
    }

    @Test
    fun onModoTemaChange_guardaLaPreferenciaEnDataStore() {
        val vm = AuthViewModel(preferencias)

        vm.onModoTemaChange(ModoTema.OSCURO)

        coVerify(exactly = 1) { preferencias.guardarModoTema(ModoTema.OSCURO) }
    }
}
