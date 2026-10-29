package com.example.appinventario.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.appinventario.model.ModoTema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {

    companion object {
        private val CLAVE_MODO_TEMA = stringPreferencesKey("modo_tema")
        private val CLAVE_SESION_ACTIVA = booleanPreferencesKey("sesion_activa")
        private val CLAVE_EMAIL_USUARIO = stringPreferencesKey("email_usuario")
    }

    // Leer el modo de tema guardado
    val modoTema: Flow<ModoTema> = context.dataStore.data.map { preferences ->
        val temaString = preferences[CLAVE_MODO_TEMA] ?: ModoTema.SISTEMA.name
        try {
            ModoTema.valueOf(temaString)
        } catch (e: IllegalArgumentException) {
            ModoTema.SISTEMA
        }
    }

    // Guardar la preferencia del tema
    suspend fun guardarModoTema(modo: ModoTema) {
        context.dataStore.edit { preferences ->
            preferences[CLAVE_MODO_TEMA] = modo.name
        }
    }

    // Leer el estado de la sesión activa
    val sesionActiva: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CLAVE_SESION_ACTIVA] ?: false
    }

    // Leer el email del usuario en sesión
    val emailUsuario: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CLAVE_EMAIL_USUARIO]
    }

    // Guardar sesión de usuario
    suspend fun guardarSesionUsuario(email: String) {
        context.dataStore.edit { preferences ->
            preferences[CLAVE_SESION_ACTIVA] = true
            preferences[CLAVE_EMAIL_USUARIO] = email
        }
    }

    // Limpiar sesión al cerrar
    suspend fun cerrarSesion() {
        context.dataStore.edit { preferences ->
            preferences[CLAVE_SESION_ACTIVA] = false
            preferences.remove(CLAVE_EMAIL_USUARIO)
        }
    }
}