package com.example.appinventario.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val iconoActivo: ImageVector,
    val iconoInactivo: ImageVector
)

val DestinosPrincipales = listOf(
    DestinoNavegacion(Screen.Home.route, "Órdenes", Icons.Filled.Home, Icons.Outlined.Home),
    DestinoNavegacion(Screen.Registro.route, "Registro", Icons.Filled.AddCircle, Icons.Outlined.Add),
    DestinoNavegacion(Screen.Configuracion.route, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * Estructura de navegación principal y adaptativa según WindowSizeClass.
 * Teléfonos compactos -> Barra inferior (NavigationBar).
 * Tabletas y pantallas extendidas -> Barra lateral (NavigationRail).
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val servicios by viewModel.servicios.collectAsState()

    val esPantallaExpandida = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val mostrarBarra = DestinosPrincipales.any { it.ruta == currentRoute }

    if (esPantallaExpandida) {
        // Diseño para Tableta / Pantalla horizontal (NavigationRail)
        Row(modifier = modifier.fillMaxSize()) {
            if (mostrarBarra) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
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
                            icon = {
                                IconoDestinoNav(
                                    destino = destino,
                                    seleccionado = seleccionado,
                                    totalOrdenes = servicios.size
                                )
                            },
                            label = {
                                Text(
                                    text = destino.titulo,
                                    fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                // Solo mostrar la barra inferior en destinos principales autenticados
                if (mostrarBarra) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
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
                                    icon = {
                                        IconoDestinoNav(
                                            destino = destino,
                                            seleccionado = seleccionado,
                                            totalOrdenes = servicios.size
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destino.titulo,
                                            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
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
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}

@Composable
private fun IconoDestinoNav(
    destino: DestinoNavegacion,
    seleccionado: Boolean,
    totalOrdenes: Int
) {
    val vector = if (seleccionado) destino.iconoActivo else destino.iconoInactivo
    if (destino.ruta == Screen.Home.route && totalOrdenes > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(text = if (totalOrdenes > 99) "99+" else "$totalOrdenes")
                }
            }
        ) {
            Icon(imageVector = vector, contentDescription = destino.titulo)
        }
    } else {
        Icon(imageVector = vector, contentDescription = destino.titulo)
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
    val clima by viewModel.clima.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        // Pantalla 0: Acceso institucional (Inicio de sesión con Denegación por Defecto)
        composable(Screen.Login.route) {
            LoginScreen(
                uiState = loginUiState,
                viewModel = authViewModel,
                onLoginExitoso = {
                    // Sincronizar el nombre y supervisor activo con base en el usuario autenticado
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
            // Consulta de la API externa de clima (usa el GPS de la última faena si existe)
            LaunchedEffect(Unit) {
                val ultimaConGps = servicios.firstOrNull { it.latitud != null && it.longitud != null }
                viewModel.cargarClima(ultimaConGps?.latitud, ultimaConGps?.longitud)
            }
            HomeScreen(
                servicios = servicios,
                clima = clima,
                onReintentarClima = { viewModel.cargarClima() },
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
                },
                onEstadoChange = { nuevoEstado ->
                    servicio?.let { viewModel.actualizarEstadoServicio(it, nuevoEstado) }
                },
                onEliminarClick = {
                    servicio?.let {
                        viewModel.eliminarServicio(it) {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        // Pantalla 4: Ajustes, Preferencias y Sesión Institucional
        composable(Screen.Configuracion.route) {
            val modoTema by authViewModel.modoTema.collectAsState()
            ConfiguracionScreen(
                supervisorInicial = formularioUiState.supervisor,
                usuarioActivo = loginUiState.usuarioAutenticado,
                modoTemaActual = modoTema,
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
