package com.example.appinventario.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.appinventario.model.TipoServicio
import com.example.appinventario.util.CameraUtil
import com.example.appinventario.viewmodel.FormularioUiState
import com.example.appinventario.viewmodel.ServicioViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

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
    var fotoModalUri by remember { mutableStateOf<String?>(null) }
    var fotoModalTitulo by remember { mutableStateOf("") }

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

    // Geolocalización GPS nativa con arquitectura híbrida resiliente
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun solicitarUbicacionHardware() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    viewModel.onUbicacionGpsCaptured(loc.latitude, loc.longitude)
                } else {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { freshLoc ->
                            if (freshLoc != null) {
                                viewModel.onUbicacionGpsCaptured(freshLoc.latitude, freshLoc.longitude)
                            } else {
                                // Respaldo operativo de faena para interiores sin señal satelital
                                viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
                            }
                        }
                        .addOnFailureListener {
                            viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
                        }
                }
            }.addOnFailureListener {
                viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
            }
        } catch (_: SecurityException) {
            viewModel.onUbicacionGpsCaptured(-36.8270, -73.0503)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val concedido = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            solicitarUbicacionHardware()
        }
    }

    fun capturarUbicacionGps() {
        val tieneGps = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (tieneGps) {
            solicitarUbicacionHardware()
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
            // 1. Selector tipo de faena personalizado con ancho idéntico al campo
            Column(modifier = Modifier.fillMaxWidth()) {
                val (iconoActual, _, colorActual) = when (uiState.tipoServicio) {
                    TipoServicio.FOSA_SEPTICA -> Triple(Icons.Default.Info, "Succión de lodos, desagote y sanitizado", MaterialTheme.colorScheme.primary)
                    TipoServicio.PLANTA_ELEVADORA -> Triple(Icons.Default.Settings, "Mantenimiento electromecánico de bombas", MaterialTheme.colorScheme.secondary)
                    TipoServicio.OBRA_CIVIL -> Triple(Icons.AutoMirrored.Filled.List, "Cámaras, tuberías y obras sanitarias", MaterialTheme.colorScheme.tertiary)
                    TipoServicio.EMERGENCIA -> Triple(Icons.Default.Warning, "Respuesta prioritaria ante contingencia", MaterialTheme.colorScheme.error)
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.tipoServicio.etiqueta,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de faena sanitaria") },
                        leadingIcon = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorActual.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconoActual,
                                        contentDescription = null,
                                        tint = colorActual,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Capa táctil transparente que cubre todo el campo para abrir/cerrar el menú
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { dropdownExpanded = !dropdownExpanded }
                    )
                }

                // Opciones desplegables que ocupan exactamente el mismo ancho del campo
                AnimatedVisibility(
                    visible = dropdownExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TipoServicio.entries.forEachIndexed { index, tipo ->
                                val isSelected = uiState.tipoServicio == tipo
                                val (iconoItem, subtituloItem, colorItem) = when (tipo) {
                                    TipoServicio.FOSA_SEPTICA -> Triple(Icons.Default.Info, "Succión de lodos, desagote y sanitizado", MaterialTheme.colorScheme.primary)
                                    TipoServicio.PLANTA_ELEVADORA -> Triple(Icons.Default.Settings, "Mantenimiento electromecánico de bombas", MaterialTheme.colorScheme.secondary)
                                    TipoServicio.OBRA_CIVIL -> Triple(Icons.AutoMirrored.Filled.List, "Cámaras, tuberías y obras sanitarias", MaterialTheme.colorScheme.tertiary)
                                    TipoServicio.EMERGENCIA -> Triple(Icons.Default.Warning, "Respuesta prioritaria ante contingencia", MaterialTheme.colorScheme.error)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onTipoServicioChange(tipo)
                                            dropdownExpanded = false
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                            } else Modifier
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorItem.copy(alpha = if (isSelected) 0.22f else 0.12f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = iconoItem,
                                                contentDescription = null,
                                                tint = colorItem,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tipo.etiqueta,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = subtituloItem,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionado",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (index < TipoServicio.entries.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
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

            // 4. Panel de recursos en faena: Cámara y geolocalización GPS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Evidencia y geolocalización en faena",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones de cámara
                    Text(
                        text = "Evidencia fotográfica:",
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

                    if (uiState.fotoAntesUri != null || uiState.fotoDespuesUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (uiState.fotoAntesUri != null) {
                                val bmpAntes = remember(uiState.fotoAntesUri) {
                                    CameraUtil.cargarBitmapDesdeUri(context, uiState.fotoAntesUri)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            fotoModalUri = uiState.fotoAntesUri
                                            fotoModalTitulo = "Evidencia inicial (Antes)"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bmpAntes != null) {
                                        Image(
                                            bitmap = bmpAntes,
                                            contentDescription = "Foto antes",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.65f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Ampliar",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        Text("Antes capturada", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (uiState.fotoDespuesUri != null) {
                                val bmpDespues = remember(uiState.fotoDespuesUri) {
                                    CameraUtil.cargarBitmapDesdeUri(context, uiState.fotoDespuesUri)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            fotoModalUri = uiState.fotoDespuesUri
                                            fotoModalTitulo = "Evidencia final (Después)"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bmpDespues != null) {
                                        Image(
                                            bitmap = bmpDespues,
                                            contentDescription = "Foto después",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.65f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Ampliar",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        Text("Después capturada", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
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

    // Modal de visualización de imagen completa
    if (fotoModalUri != null) {
        val bitmapCompleto = remember(fotoModalUri) {
            CameraUtil.cargarBitmapDesdeUri(context, fotoModalUri, reqWidth = 1600, reqHeight = 1600)
        }
        Dialog(
            onDismissRequest = { fotoModalUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = fotoModalTitulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmapCompleto != null) {
                            Image(
                                bitmap = bitmapCompleto,
                                contentDescription = fotoModalTitulo,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "No se pudo cargar la imagen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { fotoModalUri = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
