package com.rodrip.precioslocales.comparador.ui.products

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.rodrip.precioslocales.comparador.ui.components.BarcodeScannerView
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: ProductViewModel,
    initialStoreId: Long?,
    productId: Long?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var weightQuantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var tempPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    
    var showPhotoOptions by rememberSaveable { mutableStateOf(false) }
    var isProcessingImage by rememberSaveable { mutableStateOf(false) }
    
    var showScanner by rememberSaveable { mutableStateOf(false) }
    val isEdit = productId != null

    val stores by viewModel.stores.collectAsState()
    var selectedStoreId by rememberSaveable { mutableStateOf(initialStoreId) }
    var expandedStoreMenu by rememberSaveable { mutableStateOf(false) }

    // remoteId / remotePhotoUrl del producto seleccionado desde la nube (para preservarlo al guardar)
    var suggestionRemoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestionRemotePhotoUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val scannedProductState by viewModel.scannedProductState.collectAsState()
    val nameSuggestions by viewModel.nameSuggestions.collectAsState()
    val selectedNameSuggestion by viewModel.selectedNameSuggestion.collectAsState()

    // Estado del dropdown de autocompletado por nombre
    var showNameSuggestions by rememberSaveable { mutableStateOf(false) }
    // Flag para saber si los datos fueron cargados desde una sugerencia remota
    var suggestionLoadedFromRemote by rememberSaveable { mutableStateOf(false) }

    val segmenter = remember {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    // Cerramos el segmentador cuando se destruye el componente
    DisposableEffect(Unit) {
        onDispose {
            segmenter.close()
        }
    }

    LaunchedEffect(productId) {
        if (isEdit && name.isEmpty()) {
            viewModel.getProductById(productId!!).let { product ->
                if (product != null) {
                    name = product.name
                    barcode = product.barcode ?: ""
                    weightQuantity = product.weightQuantity
                    photoUri = product.photoUri ?: product.remotePhotoUrl
                }
            }
        }
    }

    LaunchedEffect(scannedProductState) {
        scannedProductState.product?.let { product ->
            name = product.name
            weightQuantity = product.weightQuantity
            photoUri = product.photoUri ?: product.remotePhotoUrl
            viewModel.clearScannedProduct()
        }
    }

    // Reacciona a la selección de una sugerencia por nombre
    LaunchedEffect(selectedNameSuggestion) {
        selectedNameSuggestion?.let { product ->
            name = product.name
            barcode = product.barcode ?: barcode  // conserva barcode local si ya había uno
            weightQuantity = product.weightQuantity
            photoUri = product.photoUri ?: product.remotePhotoUrl
            suggestionLoadedFromRemote = product.remoteId != null
            suggestionRemoteId = product.remoteId
            suggestionRemotePhotoUrl = product.remotePhotoUrl
            showNameSuggestions = false
            viewModel.clearSelectedNameSuggestion()
        }
    }

    // Muestra el dropdown cuando hay sugerencias y el campo tiene foco (>=4 chars)
    LaunchedEffect(nameSuggestions) {
        showNameSuggestions = nameSuggestions.isNotEmpty()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanner = true
        }
    }

    fun processSubjectSegmentation(sourceUri: Uri) {
        // Feedback visual inmediato: mostramos la foto original mientras la IA trabaja
        photoUri = sourceUri.toString()
        isProcessingImage = true
        
        try {
            val image = InputImage.fromFilePath(context, sourceUri)
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    val foregroundBitmap = result.foregroundBitmap
                    if (foregroundBitmap != null) {
                        val file = File(context.cacheDir, "product_${UUID.randomUUID()}.jpg")
                        file.outputStream().use { out ->
                            foregroundBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        // Actualizamos con la versión recortada automáticamente
                        photoUri = Uri.fromFile(file).toString()
                    }
                    isProcessingImage = false
                }
                .addOnFailureListener {
                    isProcessingImage = false
                }
        } catch (e: Exception) {
            isProcessingImage = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            processSubjectSegmentation(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { processSubjectSegmentation(it.toUri()) }
        }
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Elegir imagen") },
            text = {
                Column {
                    TextButton(onClick = {
                        showPhotoOptions = false
                        val file = File(context.cacheDir, "temp_photo_${UUID.randomUUID()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        tempPhotoUri = uri.toString()
                        takePictureLauncher.launch(uri)
                    }) { Text("Cámara") }
                    
                    TextButton(onClick = {
                        showPhotoOptions = false
                        galleryLauncher.launch("image/*")
                    }) { Text("Galería") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptions = false }) { Text("Cancelar") }
            }
        )
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
                    if (name.isBlank() || price.isBlank() || selectedStoreId == null || isProcessingImage) return@ExtendedFloatingActionButton
                    viewModel.saveProductWithPrice(
                        storeId = selectedStoreId!!,
                        productId = productId ?: 0L,
                        barcode = barcode.takeIf { it.isNotBlank() },
                        name = name,
                        weightQuantity = weightQuantity,
                        photoUri = photoUri,
                        price = price.toDoubleOrNull() ?: 0.0,
                        remoteId = suggestionRemoteId,
                        remotePhotoUrl = suggestionRemotePhotoUrl,
                        onSuccess = onNavigateBack
                    )
                },
                icon = { 
                    if (isProcessingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                    }
                },
                text = { Text(if (isProcessingImage) "Procesando..." else "Guardar") }
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
            if (scannedProductState.source == ProductSource.REMOTE || suggestionLoadedFromRemote) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Datos cargados desde la nube · Solo falta el precio y el local",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = {
                    if (!isProcessingImage) showPhotoOptions = true
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (photoUri != null) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                            if (isProcessingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }
                    } else if (isProcessingImage) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tocar para tomar foto")
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedStoreMenu,
                onExpandedChange = { expandedStoreMenu = !expandedStoreMenu }
            ) {
                OutlinedTextField(
                    value = stores.find { it.id == selectedStoreId }?.name ?: "Seleccionar Local",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Local Comercial") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStoreMenu) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedStoreMenu,
                    onDismissRequest = { expandedStoreMenu = false }
                ) {
                    stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store.name) },
                            onClick = {
                                selectedStoreId = store.id
                                expandedStoreMenu = false
                            }
                        )
                    }
                }
            }

            // ── Autocompletado de nombre ──────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showNameSuggestions && nameSuggestions.isNotEmpty(),
                onExpandedChange = { /* controlado por la query */ }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        name = value
                        suggestionLoadedFromRemote = false
                        viewModel.onNameQueryChanged(value)
                        if (value.length < 4) showNameSuggestions = false
                    },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (name.length >= 4 && nameSuggestions.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = showNameSuggestions
                            )
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = showNameSuggestions && nameSuggestions.isNotEmpty(),
                    onDismissRequest = { showNameSuggestions = false }
                ) {
                    nameSuggestions.forEach { product ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Miniatura del producto
                                    val imageModel = product.photoUri ?: product.remotePhotoUrl
                                    if (imageModel != null) {
                                        AsyncImage(
                                            model = imageModel,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            product.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (product.weightQuantity.isNotBlank()) {
                                            Text(
                                                product.weightQuantity,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (product.remoteId != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Cloud,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Nube",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectNameSuggestion(product)
                            }
                        )
                    }
                }
            }

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
