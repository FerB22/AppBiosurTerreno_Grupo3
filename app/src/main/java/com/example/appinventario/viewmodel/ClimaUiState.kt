package com.example.appinventario.viewmodel

/** Estados de la tarjeta de clima alimentada por la API externa. */
sealed interface ClimaUiState {
    data object Cargando : ClimaUiState
    data class Disponible(
        val temperaturaC: Double,
        val vientoKmH: Double,
        val descripcion: String
    ) : ClimaUiState
    data class Error(val mensaje: String) : ClimaUiState
}
