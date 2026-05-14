package com.rodrip.precioslocales.comparador.ui.stores

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rodrip.precioslocales.comparador.data.model.UruguayGeo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStoreScreen(
    viewModel: StoreViewModel,
    storeId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = storeId != null

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }

    // Selectores de zona
    var selectedDepartment by remember { mutableStateOf("") }
    var selectedLocalidad by remember { mutableStateOf("") }
    var localidadPersonalizada by remember { mutableStateOf("") }
    var departmentExpanded by remember { mutableStateOf(false) }
    var localidadExpanded by remember { mutableStateOf(false) }

    val checkState by viewModel.checkState.collectAsState()

    // Pre-cargar datos al editar
    LaunchedEffect(storeId) {
        if (isEdit) {
            viewModel.getStoreById(storeId!!)?.let { store ->
                name = store.name
                address = store.address
                hours = store.hours
                latitude = store.latitude
                longitude = store.longitude
                selectedDepartment = store.department
                val predefined = UruguayGeo.getLocalidades(store.department)
                if (store.localidad in predefined) {
                    selectedLocalidad = store.localidad
                } else if (store.localidad.isNotEmpty()) {
                    selectedLocalidad = UruguayGeo.OTRA
                    localidadPersonalizada = store.localidad
                }
            }
        }
    }

    // Cuando el check resulta Clear → guardado automático
    LaunchedEffect(checkState) {
        if (checkState is StoreCheckState.Clear) {
            viewModel.confirmSave((checkState as StoreCheckState.Clear).pendingStore) { onNavigateBack() }
        }
    }

    // Localidades disponibles según el departamento elegido
    val localidades = remember(selectedDepartment) { UruguayGeo.getLocalidades(selectedDepartment) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocation(context) { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
            }
        }
    }

    // ── Diálogo de duplicados ─────────────────────────────────────────────────
    when (val state = checkState) {
        is StoreCheckState.LocalDuplicate -> {
            DuplicateAlertDialog(
                title = "Local ya existe localmente",
                storeName = state.existing.name,
                storeAddress = state.existing.address,
                distanceMeters = state.distanceMeters,
                onUseExisting = {
                    // Editar el existente en lugar de crear uno nuevo
                    viewModel.resetCheckState()
                    onNavigateBack()
                },
                onCreateAnyway = {
                    viewModel.confirmSave(state.pendingStore) { onNavigateBack() }
                },
                onDismiss = { viewModel.resetCheckState() }
            )
        }
        is StoreCheckState.RemoteDuplicate -> {
            DuplicateAlertDialog(
                title = "Local encontrado en la nube",
                storeName = state.dto.name,
                storeAddress = state.dto.address,
                distanceMeters = state.distanceMeters,
                onUseExisting = {
                    // Descargar el local de Firestore y vincularlo
                    viewModel.confirmSave(
                        state.pendingStore.copy(
                            name = state.dto.name,
                            address = state.dto.address,
                            hours = state.dto.hours,
                            department = state.dto.department,
                            localidad = state.dto.localidad,
                            latitude = state.dto.latitude,
                            longitude = state.dto.longitude
                        ),
                        linkedRemoteId = state.dto.remoteId
                    ) { onNavigateBack() }
                },
                onCreateAnyway = {
                    viewModel.confirmSave(state.pendingStore) { onNavigateBack() }
                },
                onDismiss = { viewModel.resetCheckState() }
            )
        }
        else -> { /* Idle, Checking, Saved — sin diálogo */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar Local" else "Nuevo Local") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            val isSaving = checkState is StoreCheckState.Checking
            ExtendedFloatingActionButton(
                onClick = {
                    if (name.isBlank() || selectedDepartment.isBlank()) return@ExtendedFloatingActionButton
                    val finalLocalidad =
                        if (selectedLocalidad == UruguayGeo.OTRA) localidadPersonalizada
                        else selectedLocalidad
                    viewModel.checkBeforeSave(
                        id = storeId ?: 0L,
                        name = name,
                        address = address,
                        hours = hours,
                        department = selectedDepartment,
                        localidad = finalLocalidad,
                        latitude = latitude,
                        longitude = longitude
                    )
                },
                icon = {
                    if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Save, contentDescription = null)
                },
                text = { Text(if (isSaving) "Verificando…" else "Guardar") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Local *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isBlank()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Horarios (ej: 09:00 - 20:00)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Selector de Departamento ───────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = departmentExpanded,
                onExpandedChange = { departmentExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedDepartment,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Departamento *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(departmentExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    isError = selectedDepartment.isBlank()
                )
                ExposedDropdownMenu(
                    expanded = departmentExpanded,
                    onDismissRequest = { departmentExpanded = false }
                ) {
                    UruguayGeo.getDepartmentNames().forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept) },
                            onClick = {
                                selectedDepartment = dept
                                selectedLocalidad = ""
                                localidadPersonalizada = ""
                                departmentExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Selector de Localidad (depende del departamento) ───────────────
            if (selectedDepartment.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = localidadExpanded,
                    onExpandedChange = { localidadExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLocalidad,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Localidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(localidadExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = localidadExpanded,
                        onDismissRequest = { localidadExpanded = false }
                    ) {
                        localidades.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    selectedLocalidad = loc
                                    if (loc != UruguayGeo.OTRA) localidadPersonalizada = ""
                                    localidadExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campo libre cuando se elige "Otra…"
                if (selectedLocalidad == UruguayGeo.OTRA) {
                    OutlinedTextField(
                        value = localidadPersonalizada,
                        onValueChange = { localidadPersonalizada = it },
                        label = { Text("Nombre de la localidad") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Ej: Villa San Rafael") }
                    )
                }
            }

            // ── GPS ────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ubicación GPS", style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Obtener GPS")
                        }
                    }
                    if (latitude != 0.0 || longitude != 0.0) {
                        Text("Lat: $latitude", style = MaterialTheme.typography.bodySmall)
                        Text("Lon: $longitude", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.LightGray, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (latitude != 0.0 || longitude != 0.0) "$latitude, $longitude"
                            else "Sin coordenadas",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ─── Diálogo de duplicados ────────────────────────────────────────────────────

@Composable
private fun DuplicateAlertDialog(
    title: String,
    storeName: String,
    storeAddress: String,
    distanceMeters: Double,
    onUseExisting: () -> Unit,
    onCreateAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Se encontró un local similar a ${UruguayGeo.formatDistance(distanceMeters)} de distancia:")
                Spacer(Modifier.height(4.dp))
                Text("📍 $storeName", style = MaterialTheme.typography.bodyMedium)
                if (storeAddress.isNotEmpty())
                    Text(storeAddress, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onUseExisting) { Text("Usar este local") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCreateAnyway) { Text("Crear de todas formas") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location: Location? -> location?.let { onLocationReceived(it) } }
}