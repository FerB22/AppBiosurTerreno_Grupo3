package com.example.appinventario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.appinventario.ui.navigation.BiosurApp
import com.example.appinventario.ui.theme.AppInventarioTheme
import com.example.appinventario.viewmodel.AuthViewModel
import com.example.appinventario.viewmodel.ServicioViewModel

/**
 * Actividad principal del MVP Biosur Terreno.
 * Configura Jetpack Compose, el tema corporativo y el cálculo dinámico de WindowSizeClass.
 */
class MainActivity : ComponentActivity() {

    private val servicioViewModel: ServicioViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppInventarioTheme {
                BiosurApp(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    viewModel = servicioViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
