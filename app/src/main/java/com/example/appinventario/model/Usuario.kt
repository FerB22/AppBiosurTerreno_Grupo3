package com.example.appinventario.model

/**
 * Roles de usuario dentro del sistema según el principio de menor privilegio (Least Privilege).
 * Cada rol otorga únicamente las capacidades operativas requeridas para la labor respectiva.
 */
enum class RolUsuario(
    val titulo: String,
    val descripcion: String,
    val dominioAutorizado: String
) {
    SUPERVISOR_TERRENO(
        titulo = "Supervisor de terreno",
        descripcion = "Registro de servicios sanitarios, captura satelital GPS y evidencia fotográfica.",
        dominioAutorizado = "biosur.cl"
    ),
    ADMINISTRADOR(
        titulo = "Administración central",
        descripcion = "Supervisión integral de faenas, control de costos y auditoría técnica de servicios.",
        dominioAutorizado = "administracion.biosur.cl"
    )
}

/**
 * Representación inmutable del usuario autenticado en la sesión activa.
 */
data class Usuario(
    val email: String,
    val nombre: String,
    val rol: RolUsuario
)
