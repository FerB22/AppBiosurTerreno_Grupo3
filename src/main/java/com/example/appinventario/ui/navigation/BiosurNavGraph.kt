package com.example.appinventario.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appinventario.model.Screen
import com.example.appinventario.ui.screens.ConfiguracionScreen
import com.example.appinventario.ui.screens.DetalleServicioScreen
import com.example.appinventario.ui.screens.HomeScreen
import com.example.appinventario.ui.screens.LoginScreen
import com.example.appinventario.ui.screens.RegistroServicioScreen
import com.example.appinventario.viewmodel.AuthViewModel
import com.example.appinventario.viewmodel.ServicioViewModel

data class DestinoNavegacion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

val DestinosPrincipales = listOf(
    DestinoNavegacion(Screen.Home.route, "Órdenes", Icons.AutoMirrored.Filled.List),
    DestinoNavegacion(Screen.Registro.route, "Registro", Icons.Default.AddCircle),
    DestinoNavegacion(Screen.Configuracion.route, "Ajustes", Icons.Default.Settings)
)

/**
 * Estructura de navegación principal y adaptativa según WindowSizeClass.
 * - Teléfonos compactos -> Barra inferior (NavigationBar).
 * - Tabletas y pantallas extendidas -> Barra lateral (NavigationRail).
 */
@Composable
fun BiosurApp(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: ServicioViewModel,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val esPantallaExpandida = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val mostrarBarra = DestinosPrincipales.any { it.ruta == currentRoute }

    if (esPantallaExpandida) {
        // Diseño para Tableta / Pantalla horizontal (NavigationRail)
        Row(modifier = modifier.fillMaxSize()) {
            if (mostrarBarra) {
                NavigationRail {
                    DestinosPrincipales.forEach { destino ->
                        val seleccionado = currentRoute == destino.ruta
                        NavigationRailItem(
                            selected = seleccionado,
                            onClick = {
                                navController.navigate(destino.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destino.icono, contentDescription = destino.titulo) },
                            label = { Text(destino.titulo) }
                        )
                    }
                }
            }

            BiosurNavHost(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        // Diseño para Teléfono (NavigationBar inferior)
        Scaffold(
            bottomBar = {
                if (mostrarBarra) {
                    NavigationBar {
                        DestinosPrincipales.forEach { destino ->
                            val seleccionado = currentRoute == destino.ruta
                            NavigationBarItem(
                                selected = seleccionado,
                                onClick = {
                                    navController.navigate(destino.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destino.icono, contentDescription = destino.titulo) },
                                label = { Text(destino.titulo) }
                            )
                        }
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            BiosurNavHost(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun BiosurNavHost(
    navController: NavHostController,
    viewModel: ServicioViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val servicios by viewModel.servicios.collectAsState()
    val formularioUiState by viewModel.formularioState.collectAsState()
    val loginUiState by authViewModel.uiState.collectAsState()
    val modoTemaActual by authViewModel.modoTema.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        // Pantalla 0: Acceso institucional
        composable(Screen.Login.route) {
            LoginScreen(
                uiState = loginUiState,
                viewModel = authViewModel,
                onLoginExitoso = {
                    loginUiState.usuarioAutenticado?.let { user ->
                        viewModel.onSupervisorChange(user.nombre)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla 1: Historial de Órdenes
        composable(Screen.Home.route) {
            HomeScreen(
                servicios = servicios,
                onServicioClick = { id ->
                    navController.navigate(Screen.Detalle.createRoute(id))
                },
                onNuevoServicioClick = {
                    navController.navigate(Screen.Registro.route)
                }
            )
        }

        // Pantalla 2: Formulario de Nueva Faena
        composable(Screen.Registro.route) {
            RegistroServicioScreen(
                uiState = formularioUiState,
                viewModel = viewModel,
                onVolverClick = {
                    navController.popBackStack()
                },
                onGuardadoExitoso = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla 3: Detalle y Costos
        composable(
            route = Screen.Detalle.route,
            arguments = listOf(navArgument("servicioId") { type = NavType.LongType })
        ) { backStackEntry ->
            val servicioId = backStackEntry.arguments?.getLong("servicioId") ?: 0L
            val servicio = servicios.find { it.id == servicioId }

            DetalleServicioScreen(
                servicio = servicio,
                onVolverClick = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla 4: Ajustes, Preferencias y Sesión Institucional
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(
                supervisorInicial = formularioUiState.supervisor,
                usuarioActivo = loginUiState.usuarioAutenticado,
                modoTemaActual = modoTemaActual,
                onModoTemaChange = { nuevoModo ->
                    authViewModel.onModoTemaChange(nuevoModo)
                },
                onSupervisorGuardado = { nuevoSupervisor ->
                    viewModel.onSupervisorChange(nuevoSupervisor)
                },
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}