package com.rodrip.precioslocales.comparador.ui.products

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.rodrip.precioslocales.comparador.ui.components.BarcodeScannerView
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ProductViewModel,
    storeId: Long,
    productId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var weightQuantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    var showScanner by remember { mutableStateOf(false) }
    val isEdit = productId != null

    val scannedProduct by viewModel.scannedProduct.collectAsState()

    LaunchedEffect(productId) {
        if (isEdit) {
            viewModel.getProductById(productId!!).let { product ->
                if (product != null) {
                    name = product.name
                    barcode = product.barcode ?: ""
                    weightQuantity = product.weightQuantity
                    photoUri = product.photoUri?.toUri()
                }
            }
        }
    }

    LaunchedEffect(scannedProduct) {
        scannedProduct?.let { product ->
            name = product.name
            weightQuantity = product.weightQuantity
            photoUri = product.photoUri?.toUri()
            viewModel.clearScannedProduct()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            // Handle failure if needed, maybe reset photoUri
        }
    }

    if (showScanner) {
        BarcodeScannerView(
            onBarcodeScanned = { code ->
                barcode = code
                viewModel.searchProductByBarcode(code)
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar Producto" else "Nuevo Producto") },
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
                    if (name.isBlank() || price.isBlank()) return@ExtendedFloatingActionButton
                    viewModel.saveProductWithPrice(
                        storeId = storeId,
                        productId = productId ?: 0L,
                        barcode = barcode.takeIf { it.isNotBlank() },
                        name = name,
                        weightQuantity = weightQuantity,
                        photoUri = photoUri?.toString(),
                        price = price.toDoubleOrNull() ?: 0.0,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = {
                    val file = File(context.cacheDir, "photo_${UUID.randomUUID()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    photoUri = uri
                    takePictureLauncher.launch(uri)
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tocar para tomar foto")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de Barras") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear")
                }
            }

            OutlinedTextField(
                value = weightQuantity,
                onValueChange = { weightQuantity = it },
                label = { Text("Peso / Cantidad (ej: 1kg, 500ml)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Precio actual") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("$ ") }
            )
        }
    }
}