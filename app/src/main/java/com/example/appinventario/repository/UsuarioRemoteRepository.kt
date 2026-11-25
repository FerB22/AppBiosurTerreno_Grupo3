package com.example.appinventario.repository

import com.example.appinventario.data.remote.LoginRequestDto
import com.example.appinventario.data.remote.UsuarioApiService
import com.example.appinventario.model.RolUsuario
import com.example.appinventario.model.Usuario
import retrofit2.HttpException
import java.io.IOException

/** Resultado de autenticar contra el microservicio `ms-usuarios`. */
sealed interface LoginRemotoResult {
    data class Exitoso(val usuario: Usuario) : LoginRemotoResult
    data class Rechazado(val mensaje: String) : LoginRemotoResult
    data object SinConexion : LoginRemotoResult
}

/**
 * Repositorio remoto de usuarios: traduce las respuestas HTTP del backend en resultados de dominio.
 * 401 y 403 se tratan como rechazo; cualquier falla de red habilita el respaldo local.
 */
class UsuarioRemoteRepository(private val api: UsuarioApiService) {

    suspend fun login(email: String, nombre: String?): LoginRemotoResult {
        return try {
            val dto = api.login(LoginRequestDto(email = email, nombre = nombre))
            val rol = RolUsuario.entries.firstOrNull { it.name == dto.rol }
                ?: return LoginRemotoResult.Rechazado("El servidor devolvió un rol no reconocido")
            LoginRemotoResult.Exitoso(Usuario(email = dto.email, nombre = dto.nombre, rol = rol))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                LoginRemotoResult.Rechazado("El servidor denegó el acceso a la cuenta corporativa")
            } else {
                LoginRemotoResult.SinConexion
            }
        } catch (e: IOException) {
            LoginRemotoResult.SinConexion
        }
    }
}
