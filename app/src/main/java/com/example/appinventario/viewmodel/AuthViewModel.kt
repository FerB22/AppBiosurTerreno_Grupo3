package com.example.appinventario.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinventario.model.ModoTema
import com.example.appinventario.model.RolUsuario
import com.example.appinventario.model.Usuario
import com.example.appinventario.util.AuthSecurityValidator
import com.example.appinventario.util.UserPreferencesManager
import com.example.appinventario.util.ValidacionCredencialesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la interfaz de inicio de sesión.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,
    val rolDetectado: RolUsuario? = null,
    val isLoading: Boolean = false,
    val loginExitoso: Boolean = false,
    val usuarioAutenticado: Usuario? = null
)

/**
 * ViewModel que gestiona la autenticación de usuarios y la persistencia de preferencias/sesión con DataStore.
 */
class AuthViewModel(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Lectura asíncrona y reactiva de preferencias mediante DataStore
    val modoTema: StateFlow<ModoTema> = preferencesManager.modoTema
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ModoTema.SISTEMA
        )

    val sesionActiva: StateFlow<Boolean> = preferencesManager.sesionActiva
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onModoTemaChange(nuevoModo: ModoTema) {
        viewModelScope.launch {
            preferencesManager.guardarModoTema(nuevoModo)
        }
    }

    fun onEmailChange(nuevoEmail: String) {
        val rolInferido = when {
            nuevoEmail.endsWith("@biosur.cl", ignoreCase = true) -> RolUsuario.SUPERVISOR_TERRENO
            nuevoEmail.endsWith("@administracion.biosur.cl", ignoreCase = true) -> RolUsuario.ADMINISTRADOR
            else -> null
        }

        _uiState.update { state ->
            state.copy(
                email = nuevoEmail,
                errorEmail = null,
                errorGeneral = null,
                rolDetectado = rolInferido
            )
        }
    }

    fun onPasswordChange(nuevaPassword: String) {
        _uiState.update { state ->
            state.copy(
                password = nuevaPassword,
                errorPassword = null,
                errorGeneral = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { state ->
            state.copy(passwordVisible = !state.passwordVisible)
        }
    }

    fun usarCuentaDemo(emailDemo: String) {
        onEmailChange(emailDemo)
        onPasswordChange("biosur2026")
    }

    fun iniciarSesion() {
        val emailActual = _uiState.value.email
        val passwordActual = _uiState.value.password

        // 1. Evaluación de Correo bajo política de Denegación por Defecto (Default Deny)
        val validacionEmail = AuthSecurityValidator.validarEmail(emailActual)
        val errorPassword = AuthSecurityValidator.validarPassword(passwordActual)

        when (validacionEmail) {
            is ValidacionCredencialesResult.Denegado -> {
                _uiState.update { state ->
                    state.copy(
                        errorEmail = validacionEmail.mensaje,
                        errorPassword = errorPassword,
                        errorGeneral = "Acceso denegado. Verifique las credenciales institucionales."
                    )
                }
                return
            }
            is ValidacionCredencialesResult.Exitoso -> {
                if (errorPassword != null) {
                    _uiState.update { state ->
                        state.copy(
                            errorEmail = null,
                            errorPassword = errorPassword,
                            errorGeneral = null
                        )
                    }
                    return
                }

                // Autenticación concedida con Menor Privilegio (Least Privilege)
                val usuarioLogueado = Usuario(
                    email = validacionEmail.emailSanitizado,
                    nombre = AuthSecurityValidator.inferirNombreDesdeEmail(validacionEmail.emailSanitizado),
                    rol = validacionEmail.rol
                )

                // Persistencia de la sesión en DataStore
                viewModelScope.launch {
                    preferencesManager.guardarSesionUsuario(usuarioLogueado.email)
                }

                _uiState.update { state ->
                    state.copy(
                        errorEmail = null,
                        errorPassword = null,
                        errorGeneral = null,
                        usuarioAutenticado = usuarioLogueado,
                        loginExitoso = true
                    )
                }
            }
        }
    }

    fun onLoginHandled() {
        _uiState.update { state ->
            state.copy(loginExitoso = false)
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            preferencesManager.cerrarSesion()
        }
        _uiState.update {
            LoginUiState()
        }
    }
}