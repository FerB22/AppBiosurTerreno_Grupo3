package com.example.appinventario

import com.example.appinventario.data.remote.UsuarioApiService
import com.example.appinventario.data.remote.UsuarioDto
import com.example.appinventario.model.RolUsuario
import com.example.appinventario.repository.LoginRemotoResult
import com.example.appinventario.repository.UsuarioRemoteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Pruebas de [UsuarioRemoteRepository]: traducción de respuestas HTTP del backend en resultados de dominio. */
class UsuarioRemoteRepositoryTest {

    private lateinit var api: UsuarioApiService
    private lateinit var repositorio: UsuarioRemoteRepository

    @Before
    fun preparar() {
        api = mockk()
        repositorio = UsuarioRemoteRepository(api)
    }

    private fun errorHttp(codigo: Int) =
        HttpException(Response.error<UsuarioDto>(codigo, ResponseBody.create(null, "")))

    @Test
    fun login_respuestaValida_retornaUsuarioConRolDelServidor() = runTest {
        coEvery { api.login(any()) } returns
            UsuarioDto("gilda.orellana@administracion.biosur.cl", "Gilda Orellana", "ADMINISTRADOR", true)

        val resultado = repositorio.login("gilda.orellana@administracion.biosur.cl", "Gilda Orellana")

        assertTrue(resultado is LoginRemotoResult.Exitoso)
        val usuario = (resultado as LoginRemotoResult.Exitoso).usuario
        assertEquals(RolUsuario.ADMINISTRADOR, usuario.rol)
        assertEquals("Gilda Orellana", usuario.nombre)
    }

    @Test
    fun login_rolDesconocido_retornaRechazado() = runTest {
        coEvery { api.login(any()) } returns UsuarioDto("a@biosur.cl", "A", "SUPERUSUARIO", true)

        assertTrue(repositorio.login("a@biosur.cl", null) is LoginRemotoResult.Rechazado)
    }

    @Test
    fun login_http403_retornaRechazado() = runTest {
        coEvery { api.login(any()) } throws errorHttp(403)

        assertTrue(repositorio.login("a@biosur.cl", null) is LoginRemotoResult.Rechazado)
    }

    @Test
    fun login_http401_retornaRechazado() = runTest {
        coEvery { api.login(any()) } throws errorHttp(401)

        assertTrue(repositorio.login("a@biosur.cl", null) is LoginRemotoResult.Rechazado)
    }

    @Test
    fun login_http500_habilitaRespaldoLocal() = runTest {
        coEvery { api.login(any()) } throws errorHttp(500)

        assertEquals(LoginRemotoResult.SinConexion, repositorio.login("a@biosur.cl", null))
    }

    @Test
    fun login_fallaDeRed_habilitaRespaldoLocal() = runTest {
        coEvery { api.login(any()) } throws IOException("Sin red")

        assertEquals(LoginRemotoResult.SinConexion, repositorio.login("a@biosur.cl", null))
    }
}
