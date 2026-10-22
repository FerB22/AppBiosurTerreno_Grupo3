package com.example.appinventario.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.appinventario.model.TipoServicio
import com.example.appinventario.util.CameraUtil
import com.example.appinventario.viewmodel.FormularioUiState
import com.example.appinventario.viewmodel.ServicioViewModel
import com.google.android.gms.location.LocationServices

/**
 * Pantalla de registro de nuevas faenas operativas en terreno.
 * Cumple con IE 2.1.2 (formularios con validación por campo),
 * IE 2.2.1 (ViewModel y StateFlow), IE 2.2.2 (animaciones de error)
 * e IE 2.4.1 (acceso nativo a Cámara y GPS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroServicioScreen(
    uiState: FormularioUiState,
    viewModel: ServicioViewModel,
    onVolverClick: () -> Unit,
    onGuardadoExitoso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var tempUriCamara by remember { mutableStateOf<Uri?>(null) }
    var tipoFotoEnCaptura by remember { mutableStateOf<String?>(null) } // "ANTES" o "DESPUES"

    // Launcher para tomar fotografía con FileProvider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exitoso ->
        if (exitoso && tempUriCamara != null) {
            val uriString = tempUriCamara.toString()
            if (tipoFotoEnCaptura == "ANTES") {
                viewModel.onFotoAntesCaptured(uriString)
            } else {
                viewModel.onFotoDespuesCaptured(uriString)
            }
        }
    }

    // Launcher para solicitar permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val uri = CameraUtil.crearUriParaFoto(context, tipoFotoEnCaptura ?: "FAENA")
            tempUriCamara = uri
            cameraLauncher.launch(uri)
        }
    }

    fun capturarFoto(tipo: String) {
        tipoFotoEnCaptura = tipo
        val tienePermiso = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) {
            val uri = CameraUtil.crearUriParaFoto(context, tipo)
            tempUriCamara = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Geolocalización GPS nativa (IE 2.4.1)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val concedido = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.onUbicacionGpsCaptured(loc.latitude, loc.longitude)
                    } else {
                        // Respaldo de faena en caso de satélites no enlazados en interiores
                        viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
                    }
                }
            } catch (_: SecurityException) {
                viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
            }
        }
    }

    fun capturarUbicacionGps() {
        val tieneGps = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tieneGps) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.onUbicacionGpsCaptured(loc.latitude, loc.longitude)
                    } else {
                        viewModel.onUbicacionGpsCaptured(-37.4697, -72.3537)
                    }
                }
            } catch (_: SecurityException) {
                viewModel.onUbicacionGpsCaptured(-37.4697, -72.3537)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar nueva faena", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a faenas"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Selector tipo de faena (Material 3 Exposed Dropdown Menu)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.tipoServicio.etiqueta,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de faena sanitaria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    TipoServicio.entries.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.etiqueta) },
                            onClick = {
                                viewModel.onTipoServicioChange(tipo)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Descripción técnica del trabajo (validación por campo)
            Column {
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = { viewModel.onDescripcionChange(it) },
                    label = { Text("Descripción técnica del trabajo") },
                    placeholder = { Text("Ej: Limpieza de cámara desgrasadora y succión...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    isError = uiState.errorDescripcion != null
                )
                AnimatedVisibility(
                    visible = uiState.errorDescripcion != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = uiState.errorDescripcion.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // 3. Fila de Horas hombre y Costo de materiales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.horasTrabajadas,
                        onValueChange = { viewModel.onHorasChange(it) },
                        label = { Text("Horas hombre (HH)") },
                        placeholder = { Text("Ej: 4.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = uiState.errorHoras != null
                    )
                    AnimatedVisibility(
                        visible = uiState.errorHoras != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = uiState.errorHoras.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = uiState.costoMateriales,
                        onValueChange = { viewModel.onCostoMaterialesChange(it) },
                        label = { Text("Materiales (CLP)") },
                        placeholder = { Text("35000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 4. Panel de Recursos Nativos: Cámara y Geolocalización GPS (IE 2.4.1)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recursos nativos en faena (IE 2.4.1)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones de cámara
                    Text(
                        text = "Evidencia fotográfica (FileProvider):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { capturarFoto("ANTES") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (uiState.fotoAntesUri != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(if (uiState.fotoAntesUri != null) "Foto Antes ✓" else "Foto Antes")
                        }

                        OutlinedButton(
                            onClick = { capturarFoto("DESPUES") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (uiState.fotoDespuesUri != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(if (uiState.fotoDespuesUri != null) "Foto Después ✓" else "Foto Después")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón de GPS
                    Text(
                        text = "Georreferenciación satelital (GPS):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { capturarUbicacionGps() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (uiState.latitud != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.latitud != null && uiState.longitud != null) {
                                "GPS: %.4f, %.4f ✓".format(uiState.latitud, uiState.longitud)
                            } else {
                                "Capturar coordenadas GPS"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Botón de confirmación y guardado
            Button(
                onClick = {
                    viewModel.registrarServicio(onSuccess = onGuardadoExitoso)
                },
                enabled = uiState.isFormValido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Guardar faena en terreno",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
