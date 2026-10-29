package com.example.appinventario

import com.example.appinventario.model.RolUsuario
import com.example.appinventario.util.AuthSecurityValidator
import com.example.appinventario.util.ValidacionCredencialesResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batería de pruebas unitarias automatizadas para [AuthSecurityValidator].
 *
 * Valida de forma aislada e independiente de la interfaz gráfica el desacoplamiento lógico (IE 2.2.1),
 * el principio de denegación por defecto (Default Deny), la sanitización con .isBlank()
 * y la asignación de privilegios operativos mínimos (Least Privilege).
 */
class AuthSecurityValidatorTest {

    @Test
    fun validarEmail_cadenaVaciaOCompuestaPorEspacios_deniegaAcceso() {
        val resultadoVacio = AuthSecurityValidator.validarEmail("")
        val resultadoEspacios = AuthSecurityValidator.validarEmail("    ")

        assertTrue("Debe denegar acceso ante cadena vacía", resultadoVacio is ValidacionCredencialesResult.Denegado)
        assertTrue("Debe denegar acceso ante cadena de solo espacios (.isBlank)", resultadoEspacios is ValidacionCredencialesResult.Denegado)
    }

    @Test
    fun validarEmail_dominioNoAutorizado_deniegaAccesoPorDefaultDeny() {
        // Ningún dominio externo o no registrado en la lista blanca debe ser admitido
        val resultadoGmail = AuthSecurityValidator.validarEmail("f.barra@gmail.com")
        val resultadoAtaque = AuthSecurityValidator.validarEmail("operario@hacker.org")

        assertTrue(resultadoGmail is ValidacionCredencialesResult.Denegado)
        assertTrue(resultadoAtaque is ValidacionCredencialesResult.Denegado)

        val mensaje = (resultadoGmail as ValidacionCredencialesResult.Denegado).mensaje
        assertTrue("El mensaje debe evidenciar la política de denegación por defecto", mensaje.contains("denegado por defecto"))
    }

    @Test
    fun validarEmail_dominioBiosurSupervisor_asignaMenorPrivilegioSupervisor() {
        val resultado = AuthSecurityValidator.validarEmail("f.barra@biosur.cl")

        assertTrue("Debe autenticar con éxito un correo del dominio oficial biosur.cl", resultado is ValidacionCredencialesResult.Exitoso)
        val exitoso = resultado as ValidacionCredencialesResult.Exitoso
        assertEquals("f.barra@biosur.cl", exitoso.emailSanitizado)
        assertEquals(RolUsuario.SUPERVISOR_TERRENO, exitoso.rol)
    }

    @Test
    fun validarEmail_dominioAdministracion_asignaMenorPrivilegioAdministrador() {
        val resultado = AuthSecurityValidator.validarEmail("gerencia@administracion.biosur.cl")

        assertTrue(resultado is ValidacionCredencialesResult.Exitoso)
        val exitoso = resultado as ValidacionCredencialesResult.Exitoso
        assertEquals(RolUsuario.ADMINISTRADOR, exitoso.rol)
    }

    @Test
    fun validarPassword_enBlancoOCorta_retornaError() {
        val errorEnBlanco = AuthSecurityValidator.validarPassword("   ")
        val errorCorta = AuthSecurityValidator.validarPassword("123")
        val exitoCorrecta = AuthSecurityValidator.validarPassword("Admin123!")

        assertEquals("La contraseña no puede estar en blanco.", errorEnBlanco)
        assertEquals("La contraseña debe tener al menos 6 caracteres.", errorCorta)
        assertEquals(null, exitoCorrecta)
    }

    @Test
    fun inferirNombreDesdeEmail_formatoEstandar_capitalizaCorrectamente() {
        val nombre = AuthSecurityValidator.inferirNombreDesdeEmail("fernando.barra@biosur.cl")
        assertEquals("Fernando Barra", nombre)
    }
}
