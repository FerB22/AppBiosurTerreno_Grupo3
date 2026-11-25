package com.example.appinventario.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Cuerpo de la solicitud `POST /api/usuarios/login` del microservicio `ms-usuarios`. */
data class LoginRequestDto(
    val email: String,
    val nombre: String? = null
)

/** Usuario corporativo devuelto por `ms-usuarios`. */
data class UsuarioDto(
    val email: String,
    val nombre: String,
    val rol: String,
    val activo: Boolean = true
)

/** Contrato Retrofit del microservicio propio de usuarios (Spring Boot, puerto 8082). */
interface UsuarioApiService {

    @POST("api/usuarios/login")
    suspend fun login(@Body solicitud: LoginRequestDto): UsuarioDto

    @GET("api/usuarios")
    suspend fun listar(): List<UsuarioDto>
}
