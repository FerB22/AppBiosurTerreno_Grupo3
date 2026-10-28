package com.example.appinventario.model

/**
 * Modelo inmutable de dominio representativo de una faena u orden de servicio en terreno.
 */
data class Servicio(
    val id: Long = 0,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val supervisor: String,
    val tipoServicio: TipoServicio,
    val descripcion: String,
    val horasTrabajadas: Double,
    val costoEstimado: Double,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoAntesUri: String? = null,
    val fotoDespuesUri: String? = null,
    val estado: EstadoServicio = EstadoServicio.FINALIZADO
)

/**
 * Tipos de servicios sanitarios operados por Biosur SpA.
 */
enum class TipoServicio(val etiqueta: String) {
    FOSA_SEPTICA("Limpieza de fosa séptica"),
    PLANTA_ELEVADORA("Mantenimiento de planta elevadora"),
    OBRA_CIVIL("Obra civil sanitaria"),
    EMERGENCIA("Atención de emergencia")
}

/**
 * Estados del flujo operativo de una orden en faena.
 */
enum class EstadoServicio(val etiqueta: String) {
    EN_PROCESO("En faena"),
    FINALIZADO("Finalizado"),
    PENDIENTE_REVISION("Pendiente de informe")
}
