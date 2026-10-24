package com.example.appinventario.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinventario.model.EstadoServicio
import com.example.appinventario.model.ModoTema
import com.example.appinventario.model.Servicio
import com.example.appinventario.model.TipoServicio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estado inmutable de la interfaz de registro de faenas en terreno.
 */
data class FormularioUiState(
    val tipoServicio: TipoServicio = TipoServicio.FOSA_SEPTICA,
    val descripcion: String = "",
    val errorDescripcion: String? = null,
    val horasTrabajadas: String = "",
    val errorHoras: String? = null,
    val costoMateriales: String = "35000",
    val supervisor: String = "Fernando Barra Muñoz",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoAntesUri: String? = null,
    val fotoDespuesUri: String? = null,
    val isFormValido: Boolean = false,
    val guardadoExitoso: Boolean = false
)

/**
 * ViewModel que desacopla la lógica de negocio y las validaciones de la interfaz visual.
 * Gestiona el estado reactivo mediante StateFlow.
 */
class ServicioViewModel : ViewModel() {

    // Modo de tema visual (Claro, Según sistema, Oscuro)
    private val _modoTema = MutableStateFlow(ModoTema.SISTEMA)
    val modoTema: StateFlow<ModoTema> = _modoTema.asStateFlow()

    // Lista en memoria para operación offline inmediata (se integrará con el repositorio Room de Esteban)
    private val _servicios = MutableStateFlow<List<Servicio>>(generarDatosIniciales())
    val servicios: StateFlow<List<Servicio>> = _servicios.asStateFlow()

    private val _formularioState = MutableStateFlow(FormularioUiState())
    val formularioState: StateFlow<FormularioUiState> = _formularioState.asStateFlow()

    fun onModoTemaChange(nuevoModo: ModoTema) {
        _modoTema.update { nuevoModo }
    }

    fun onTipoServicioChange(nuevoTipo: TipoServicio) {
        _formularioState.update { it.copy(tipoServicio = nuevoTipo) }
    }

    fun onDescripcionChange(nuevaDescripcion: String) {
        val error = if (nuevaDescripcion.isBlank()) {
            "La descripción técnica no puede estar vacía"
        } else if (nuevaDescripcion.trim().length < 10) {
            "Detalle al menos 10 caracteres sobre la labor"
        } else {
            null
        }

        _formularioState.update { state ->
            val updated = state.copy(
                descripcion = nuevaDescripcion,
                errorDescripcion = error
            )
            updated.copy(isFormValido = calcularValidez(updated))
        }
    }

    fun onHorasChange(nuevasHoras: String) {
        val horasDouble = nuevasHoras.toDoubleOrNull()
        val error = if (nuevasHoras.isBlank()) {
            "Ingrese las horas hombre trabajadas"
        } else if (horasDouble == null || horasDouble <= 0.0) {
            "Las horas deben ser mayores que 0"
        } else if (horasDouble > 24.0) {
            "El turno no puede exceder las 24 horas"
        } else {
            null
        }

        _formularioState.update { state ->
            val updated = state.copy(
                horasTrabajadas = nuevasHoras,
                errorHoras = error
            )
            updated.copy(isFormValido = calcularValidez(updated))
        }
    }

    fun onCostoMaterialesChange(nuevoCosto: String) {
        _formularioState.update { it.copy(costoMateriales = nuevoCosto) }
    }

    fun onSupervisorChange(nuevoSupervisor: String) {
        _formularioState.update { it.copy(supervisor = nuevoSupervisor) }
    }

    fun onFotoAntesCaptured(uri: String) {
        _formularioState.update { it.copy(fotoAntesUri = uri) }
    }

    fun onFotoDespuesCaptured(uri: String) {
        _formularioState.update { it.copy(fotoDespuesUri = uri) }
    }

    fun onUbicacionGpsCaptured(latitud: Double, longitud: Double) {
        _formularioState.update { it.copy(latitud = latitud, longitud = longitud) }
    }

    private fun calcularValidez(state: FormularioUiState): Boolean {
        val descValida = state.descripcion.isNotBlank() && state.descripcion.trim().length >= 10
        val horas = state.horasTrabajadas.toDoubleOrNull()
        val horasValidas = horas != null && horas in 0.1..24.0
        return descValida && horasValidas
    }

    fun registrarServicio(onSuccess: () -> Unit) {
        val current = _formularioState.value
        if (!current.isFormValido) return

        viewModelScope.launch {
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val horas = current.horasTrabajadas.toDoubleOrNull() ?: 1.0
            val costoMat = current.costoMateriales.toDoubleOrNull() ?: 0.0
            val valorHora = 35000.0 // 35 000 CLP/h tarifa base técnica
            val costoTotal = (horas * valorHora) + costoMat

            val nuevoServicio = Servicio(
                id = System.currentTimeMillis(),
                fecha = fechaActual,
                horaInicio = horaActual,
                horaTermino = horaActual,
                supervisor = current.supervisor,
                tipoServicio = current.tipoServicio,
                descripcion = current.descripcion.trim(),
                horasTrabajadas = horas,
                costoEstimado = costoTotal,
                latitud = current.latitud,
                longitud = current.longitud,
                fotoAntesUri = current.fotoAntesUri,
                fotoDespuesUri = current.fotoDespuesUri,
                estado = EstadoServicio.FINALIZADO
            )

            _servicios.update { listOf(nuevoServicio) + it }
            _formularioState.update { FormularioUiState() } // Reiniciar formulario
            onSuccess()
        }
    }

    private fun generarDatosIniciales(): List<Servicio> = listOf(
        Servicio(
            id = 1L,
            fecha = "15/10/2026",
            horaInicio = "08:30",
            horaTermino = "12:30",
            supervisor = "Fernando Barra Muñoz",
            tipoServicio = TipoServicio.FOSA_SEPTICA,
            descripcion = "Vaciado y succión de lodos en Planta Industrial Arauco. Volumen extraído: 15 m³.",
            horasTrabajadas = 4.0,
            costoEstimado = 210000.0, // 4 * 35 000 + 70 000
            latitud = -36.8270,
            longitud = -73.0503,
            estado = EstadoServicio.FINALIZADO
        ),
        Servicio(
            id = 2L,
            fecha = "15/10/2026",
            horaInicio = "14:00",
            horaTermino = "17:00",
            supervisor = "Fernando Barra Muñoz",
            tipoServicio = TipoServicio.PLANTA_ELEVADORA,
            descripcion = "Revisión de bomba sumergible en Planta Elevadora de Aguas Servidas (PEAS) Ñuble.",
            horasTrabajadas = 3.0,
            costoEstimado = 135000.0,
            latitud = -36.6066,
            longitud = -72.1034,
            estado = EstadoServicio.EN_PROCESO
        ),
        Servicio(
            id = 3L,
            fecha = "14/10/2026",
            horaInicio = "10:15",
            horaTermino = "14:45",
            supervisor = "Esteban Medina",
            tipoServicio = TipoServicio.EMERGENCIA,
            descripcion = "Desborde sanitario en faena Los Ángeles. Reemplazo de válvula check de retención.",
            horasTrabajadas = 4.5,
            costoEstimado = 225000.0,
            latitud = -37.4697,
            longitud = -72.3537,
            estado = EstadoServicio.FINALIZADO
        ),
        Servicio(
            id = 4L,
            fecha = "16/10/2026",
            horaInicio = "09:00",
            horaTermino = "15:30",
            supervisor = "Fernando Barra Muñoz",
            tipoServicio = TipoServicio.OBRA_CIVIL,
            descripcion = "Construcción de cámara desgrasadora e instalación de tubería PVC de 110 mm en Parque Industrial Coronel.",
            horasTrabajadas = 6.5,
            costoEstimado = 297500.0,
            latitud = -37.0167,
            longitud = -73.1333,
            estado = EstadoServicio.PENDIENTE_REVISION
        ),
        Servicio(
            id = 5L,
            fecha = "16/10/2026",
            horaInicio = "11:00",
            horaTermino = "16:00",
            supervisor = "Esteban Medina",
            tipoServicio = TipoServicio.PLANTA_ELEVADORA,
            descripcion = "Inspección preventiva y cambio de sellos mecánicos en estación elevadora Talcahuano Puerto.",
            horasTrabajadas = 5.0,
            costoEstimado = 225000.0,
            latitud = -36.7249,
            longitud = -73.1168,
            estado = EstadoServicio.EN_PROCESO
        )
    )
}
