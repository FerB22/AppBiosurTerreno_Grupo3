package com.example.appinventario.viewmodel

import androidx.lifecycle.ViewModel
import com.example.appinventario.model.RolUsuario
import com.example.appinventario.model.Usuario
import com.example.appinventario.util.AuthSecurityValidator
import com.example.appinventario.util.ValidacionCredencialesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
 * ViewModel que gestiona la autenticación de usuarios aplicando:
 * 1. Denegación por defecto (Default Deny): ningún acceso es concedido salvo coincidencia estricta en lista blanca.
 * 2. Menor privilegio (Least Privilege): la cuenta adquiere únicamente el rol correspondiente a su dominio.
 */
class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
        _uiState.update {
            LoginUiState()
        }
    }
}
