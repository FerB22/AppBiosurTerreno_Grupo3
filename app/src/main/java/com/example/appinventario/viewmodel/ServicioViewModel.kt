package com.example.appinventario.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinventario.model.EstadoServicio
import com.example.appinventario.model.ModoTema
import com.example.appinventario.model.Servicio
import com.example.appinventario.model.TipoServicio
import com.example.appinventario.repository.ServicioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val errorCostoMateriales: String? = null,
    val supervisor: String = "Fernando Barra Muñoz",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoAntesUri: String? = null,
    val fotoDespuesUri: String? = null,
    val isFormValido: Boolean = false,
    val guardadoExitoso: Boolean = false
)

/**
 * ViewModel desacoplado con persistencia fuera de línea integrada mediante Room.
 */
class ServicioViewModel(private val repository: ServicioRepository) : ViewModel() {

    // Modo de tema visual
    private val _modoTema = MutableStateFlow(ModoTema.SISTEMA)
    val modoTema: StateFlow<ModoTema> = _modoTema.asStateFlow()

    // Lista reactiva conectada directamente al repositorio Room
    val servicios: StateFlow<List<Servicio>> = repository.todosLosServicios
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        val costoDouble = nuevoCosto.toDoubleOrNull()
        val error = if (nuevoCosto.isBlank()) {
            "Ingrese el costo estimado o 0"
        } else if (costoDouble == null || costoDouble < 0.0) {
            "El costo debe ser un número igual o mayor a 0"
        } else if (costoDouble > 50000000.0) {
            "El costo excede el límite operativo de faena"
        } else {
            null
        }

        _formularioState.update { state ->
            val updated = state.copy(
                costoMateriales = nuevoCosto,
                errorCostoMateriales = error
            )
            updated.copy(isFormValido = calcularValidez(updated))
        }
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
        val costo = state.costoMateriales.toDoubleOrNull()
        val costoValido = state.costoMateriales.isNotBlank() && costo != null && costo >= 0.0
        return descValida && horasValidas && costoValido
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
                id = 0, // Room auto-generará la clave primaria al insertar
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

            // Inserción en la base de datos persistente mediante el repositorio
            repository.guardarServicio(nuevoServicio)
            _formularioState.update { FormularioUiState() } // Reiniciar formulario
            onSuccess()
        }
    }
}