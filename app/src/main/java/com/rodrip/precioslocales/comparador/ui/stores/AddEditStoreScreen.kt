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
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStoreScreen(
    viewModel: StoreViewModel,
    storeId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }

    val isEdit = storeId != null

    LaunchedEffect(storeId) {
        if (isEdit) {
            viewModel.getStoreById(storeId!!).let { store ->
                if (store != null) {
                    name = store.name
                    address = store.address
                    hours = store.hours
                    latitude = store.latitude
                    longitude = store.longitude
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation(context) { location ->
                latitude = location.latitude
                longitude = location.longitude
            }
        }
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
            ExtendedFloatingActionButton(
                onClick = {
                    if (name.isBlank()) return@ExtendedFloatingActionButton
                    viewModel.saveStore(
                        id = storeId ?: 0L,
                        name = name,
                        address = address,
                        hours = hours,
                        latitude = latitude,
                        longitude = longitude,
                        onSuccess = onNavigateBack
                    )
                },
                icon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                text = { Text("Guardar") }
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
                label = { Text("Nombre del Local") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Obtener GPS")
                        }
                    }

                    if (latitude != 0.0 || longitude != 0.0) {
                        Text("Lat: $latitude", style = MaterialTheme.typography.bodySmall)
                        Text("Lon: $longitude", style = MaterialTheme.typography.bodySmall)
                    }

                    // Mini-map placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.LightGray, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Mapa (Próximamente)\n$latitude, $longitude", color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location: Location? ->
            location?.let { onLocationReceived(it) }
        }
}