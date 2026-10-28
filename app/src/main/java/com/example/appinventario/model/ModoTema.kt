package com.example.appinventario.model

/**
 * Modos de apariencia visual para la interfaz del sistema.
 */
enum class ModoTema(val etiqueta: String, val descripcion: String) {
    CLARO(
        etiqueta = "Claro",
        descripcion = "Optimizado para alta legibilidad bajo luz solar directa en faena."
    ),
    SISTEMA(
        etiqueta = "Según sistema",
        descripcion = "Sincronizado automáticamente con la configuración del dispositivo."
    ),
    OSCURO(
        etiqueta = "Oscuro",
        descripcion = "Reduce la fatiga visual nocturna y optimiza el consumo de batería."
    )
}
