package com.example.appinventario.util

/** Utilidades puras para interpretar los datos de la API externa de clima. */
object ClimaUtil {

    /** Traduce un código meteorológico WMO de Open-Meteo en una descripción legible. */
    fun describirCodigo(codigo: Int?): String = when (codigo) {
        null -> "Sin datos"
        0 -> "Despejado"
        1, 2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        in 51..57 -> "Llovizna"
        in 61..67 -> "Lluvia"
        in 71..77 -> "Nieve"
        in 80..82 -> "Chubascos"
        in 95..99 -> "Tormenta eléctrica"
        else -> "Condición variable"
    }
}
