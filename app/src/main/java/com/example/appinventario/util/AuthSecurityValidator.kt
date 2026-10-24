package com.example.appinventario.util

import com.example.appinventario.model.RolUsuario

/**
 * Resultado de la validación de credenciales bajo la política de denegación por defecto.
 */
sealed class ValidacionCredencialesResult {
    data class Exitoso(val emailSanitizado: String, val rol: RolUsuario) : ValidacionCredencialesResult()
    data class Denegado(val mensaje: String) : ValidacionCredencialesResult()
}

/**
 * Validador de seguridad que aplica estrictamente el principio de denegación por defecto (Default Deny)
 * y asignación por menor privilegio (Least Privilege).
 *
 * Ninguna cuenta es admitida salvo que pertenezca explícitamente a los dos dominios institucionales autorizados.
 */
object AuthSecurityValidator {

    // Lista blanca estricta e inmutable de dominios corporativos autorizados
    val DOMINIOS_PERMITIDOS: Set<String> = setOf(
        "biosur.cl",
        "administracion.biosur.cl"
    )

    private val REGEX_PARTE_LOCAL = Regex("^[a-zA-Z0-9._%+-]+$")

    /**
     * Valida el correo institucional bajo el principio de denegación por defecto.
     * Retorna [ValidacionCredencialesResult.Exitoso] solo si el formato es riguroso y el dominio
     * se encuentra en la lista blanca; en cualquier otro caso deniega el acceso.
     */
    fun validarEmail(emailRaw: String): ValidacionCredencialesResult {
        if (emailRaw.isBlank()) {
            return ValidacionCredencialesResult.Denegado("El correo institucional no puede estar en blanco.")
        }

        val emailLimpio = emailRaw.trim().lowercase()

        if (emailLimpio.contains(" ")) {
            return ValidacionCredencialesResult.Denegado("El correo no puede contener espacios en blanco.")
        }

        val partes = emailLimpio.split("@")
        if (partes.size != 2) {
            return ValidacionCredencialesResult.Denegado("Estructura de correo no válida. Debe contener exactamente un signo '@'.")
        }

        val parteLocal = partes[0]
        val dominio = partes[1]

        if (parteLocal.isBlank() || !REGEX_PARTE_LOCAL.matches(parteLocal)) {
            return ValidacionCredencialesResult.Denegado("El nombre de usuario contiene caracteres no permitidos antes de la '@'.")
        }

        // Aplicación estricta de Denegación por Defecto (Default Deny)
        if (dominio !in DOMINIOS_PERMITIDOS) {
            return ValidacionCredencialesResult.Denegado(
                "Acceso denegado por defecto: El dominio institucional '@$dominio' no está autorizado. Ingrese una cuenta @biosur.cl o @administracion.biosur.cl."
            )
        }

        // Asignación de rol por Menor Privilegio (Least Privilege)
        val rolAsignado = when (dominio) {
            "biosur.cl" -> RolUsuario.SUPERVISOR_TERRENO
            "administracion.biosur.cl" -> RolUsuario.ADMINISTRADOR
            else -> return ValidacionCredencialesResult.Denegado("Dominio no mapeado a ningún privilegio operativo.")
        }

        return ValidacionCredencialesResult.Exitoso(emailLimpio, rolAsignado)
    }

    /**
     * Valida la contraseña requerida para el inicio de sesión.
     */
    fun validarPassword(password: String): String? {
        if (password.isBlank()) {
            return "La contraseña no puede estar en blanco."
        }
        if (password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres."
        }
        return null
    }

    /**
     * Obtiene un nombre legible para el usuario a partir del prefijo de su correo institucional.
     */
    fun inferirNombreDesdeEmail(email: String): String {
        val parteLocal = email.trim().substringBefore("@")
        if (parteLocal.isBlank()) return "Usuario Biosur"

        return parteLocal
            .split(".", "_", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { palabra ->
                palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
