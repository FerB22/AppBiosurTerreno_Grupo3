package com.example.appinventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.appinventario.model.AppDatabase
import com.example.appinventario.model.ModoTema
import com.example.appinventario.repository.ServicioRepository
import com.example.appinventario.ui.navigation.BiosurApp
import com.example.appinventario.ui.theme.AppInventarioTheme
import com.example.appinventario.util.UserPreferencesManager
import com.example.appinventario.viewmodel.AppViewModelFactory
import com.example.appinventario.viewmodel.AuthViewModel
import com.example.appinventario.viewmodel.ServicioViewModel

/**
 * Actividad principal del MVP Biosur Terreno.
 * Configura Jetpack Compose, el tema corporativo, el cálculo dinámico de WindowSizeClass
 * y la inyección de dependencias de persistencia (Room + DataStore).
 */
class MainActivity : ComponentActivity() {

    // Instanciación de ViewModels mediante el Factory con persistencia de Room y DataStore
    private val servicioViewModel: ServicioViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ServicioRepository(database.servicioDao())
        val preferencesManager = UserPreferencesManager(applicationContext)
        AppViewModelFactory(repository, preferencesManager)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ServicioRepository(database.servicioDao())
        val preferencesManager = UserPreferencesManager(applicationContext)
        AppViewModelFactory(repository, preferencesManager)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            // Observación del modo de tema persistido mediante DataStore desde AuthViewModel
            val modoTema by authViewModel.modoTema.collectAsState()
            val esOscuro = when (modoTema) {
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
                ModoTema.SISTEMA -> isSystemInDarkTheme()
            }

            AppInventarioTheme(darkTheme = esOscuro) {
                BiosurApp(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    viewModel = servicioViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}