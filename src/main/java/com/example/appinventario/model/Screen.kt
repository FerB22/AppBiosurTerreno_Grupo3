package com.example.appinventario.model

/**
 * Rutas y destinos sellados de navegación para Jetpack Compose.
 */
sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "Acceso institucional")
    data object Home : Screen("home", "Historial")
    data object Registro : Screen("registro", "Nuevo servicio")
    data object Detalle : Screen("detalle/{servicioId}", "Detalle del servicio") {
        fun createRoute(servicioId: Long): String = "detalle/$servicioId"
    }
    data object Configuracion : Screen("configuracion", "Ajustes")
}
