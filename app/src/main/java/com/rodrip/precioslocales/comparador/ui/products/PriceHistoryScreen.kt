package com.rodrip.precioslocales.comparador.ui.products

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.local.entity.Producto
import com.rodrip.precioslocales.comparador.data.local.entity.RegistroPrecio
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ── Period filter enum ─────────────────────────────────────────────────────

enum class PeriodFilter(val label: String, val days: Int?) {
    SEVEN_DAYS("7 días", 7),
    ONE_MONTH("1 mes", 30),
    THREE_MONTHS("3 meses", 90),
    ALL("Todo", null)
}

// ── Internal data holders (avoid repeated allocations) ────────────────────

private data class PriceStats(
    val min: Double,
    val max: Double,
    val avg: Double,
    val trendDiff: Double
)

private data class ChartMetrics(
    val minPrice: Double,
    val maxPrice: Double,
    val dispMin: Double,
    val dispRange: Double,
    val gridLevels: List<Double>,
    val labelIndices: List<Int>
)

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistoryScreen(
    viewModel: ProductViewModel,
    productId: Long,
    storeId: Long,
    onNavigateBack: () -> Unit,
    onCompareStores: (() -> Unit)? = null
) {
    // ROOT FIX A: remember the StateFlow so it is NOT recreated on every recomposition.
    // Without this, each recompose creates a NEW stateIn() flow whose initialValue = null,
    // causing collectAsState() to reset to null and flash the loading/empty state repeatedly.
    val historyFlow = remember(productId, storeId) { viewModel.getPriceHistory(productId, storeId) }

    // history is:  null  → still loading (first Room query in flight)
    //              emptyList() → query finished, no records exist
    //              non-empty  → has data
    val history by historyFlow.collectAsState()

    var product by remember { mutableStateOf<Producto?>(null) }
    var store by remember { mutableStateOf<LocalComercial?>(null) }
    var selectedPeriod by remember { mutableStateOf(PeriodFilter.ALL) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(productId, storeId) {
        product = viewModel.getProductById(productId)
        store = viewModel.getStoreById(storeId)
    }

    // FIX #1: filteredHistory memoized. Returns null while history is still loading.
    val filteredHistory: List<RegistroPrecio>? = remember(history, selectedPeriod) {
        val h = history ?: return@remember null   // propagate loading state
        val days = selectedPeriod.days
        if (days == null) h
        else {
            val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
            h.filter { it.timestamp >= cutoff }
        }
    }

    // FIX #2: stable onDelete lambda — created once, captures only stable references.
    // Avoids allocating a NEW lambda per list item per recomposition.
    val onDeleteRecord: (RegistroPrecio) -> Unit = remember {
        { deleted ->
            viewModel.deletePriceRecord(deleted)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Registro eliminado",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restorePriceRecord(deleted)
                }
            }
        }
    }

    // FIX #3: previousPrices map — O(1) lookup per item instead of O(n) indexOf per item.
    val previousPrices: Map<Long, Double?> = remember(filteredHistory) {
        val h = filteredHistory ?: return@remember emptyMap()
        buildMap {
            h.forEachIndexed { idx, record ->
                put(record.id, h.getOrNull(idx + 1)?.price)
            }
        }
    }

    // FIX #4: sort for chart done once here, not inside every item{} recomposition.
    val chartRecords = remember(filteredHistory) {
        val h = filteredHistory ?: return@remember emptyList()
        if (h.size >= 2) h.sortedBy { it.timestamp } else emptyList()
    }

    // FIX #5: stable PaddingValues — avoids new object on every recomposition.
    val hasCompareButton = onCompareStores != null
    val listContentPadding = remember(hasCompareButton) {
        PaddingValues(bottom = if (hasCompareButton) 88.dp else 16.dp)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Precios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (onCompareStores != null) {
                FloatingActionButton(onClick = onCompareStores) {
                    Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = "Comparar con otros locales")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (product != null && store != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = product!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "en ${store!!.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = store!!.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodFilter.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (filteredHistory == null) {
                // ROOT FIX B: null = still loading. Show spinner instead of empty state,
                // preventing the false "No hay registros" flash on first composition.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.History, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text("No hay registros para este período", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding   // FIX #5: stable reference
                ) {
                    item { StatsSummaryCard(filteredHistory) }  // non-null guaranteed by the else branch

                    if (chartRecords.isNotEmpty()) {
                        item {
                            PriceLineChart(
                                records = chartRecords,  // FIX #4: pre-sorted, stable reference
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Registros (${filteredHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp
                            )
                        )
                    }

                    items(filteredHistory, key = { it.id }) { record ->
                        // FIX #3: O(1) map lookup replaces O(n) indexOf
                        val previousPrice = previousPrices[record.id]
                        SwipeToDeleteHistoryItem(
                            record = record,
                            previousPrice = previousPrice,
                            onDelete = onDeleteRecord  // FIX #2: stable lambda reference
                        )
                    }
                }
            }
        }
    }
}

// ── Statistics summary card ────────────────────────────────────────────────

@Composable
fun StatsSummaryCard(history: List<RegistroPrecio>) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // FIX #6: all derived data memoized — avoids re-computation on unrelated recompositions
    val stats = remember(history) {
        val prices = history.map { it.price }
        PriceStats(
            min = prices.min(),
            max = prices.max(),
            avg = prices.average(),
            trendDiff = if (history.size >= 2) history.first().price - history.last().price else 0.0
        )
    }

    // FIX #6b: formatted strings memoized separately so they only reformat when values change
    val fmtMin = remember(stats.min) { currencyFormat.format(stats.min) }
    val fmtAvg = remember(stats.avg) { currencyFormat.format(stats.avg) }
    val fmtMax = remember(stats.max) { currencyFormat.format(stats.max) }
    val fmtTrend = remember(stats.trendDiff) { currencyFormat.format(abs(stats.trendDiff)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Resumen estadístico", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Mínimo", fmtMin, Color(0xFF2E7D32))
                StatItem("Promedio", fmtAvg, MaterialTheme.colorScheme.onSurfaceVariant)
                StatItem("Máximo", fmtMax, MaterialTheme.colorScheme.error)
            }
            if (history.size >= 2) {
                HorizontalDivider()
                val trendColor = when {
                    stats.trendDiff > 0 -> MaterialTheme.colorScheme.error
                    stats.trendDiff < 0 -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val trendIcon = when {
                    stats.trendDiff > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
                    stats.trendDiff < 0 -> Icons.AutoMirrored.Rounded.TrendingDown
                    else -> Icons.Rounded.Remove
                }
                val trendText = when {
                    stats.trendDiff > 0 -> "El precio subió $fmtTrend desde el primer registro"
                    stats.trendDiff < 0 -> "El precio bajó $fmtTrend desde el primer registro"
                    else -> "El precio se mantuvo estable"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(16.dp))
                    Text(trendText, style = MaterialTheme.typography.labelSmall, color = trendColor)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Price line chart (Canvas) ──────────────────────────────────────────────

@Composable
fun PriceLineChart(
    records: List<RegistroPrecio>, // must be sorted ASC by timestamp
    modifier: Modifier = Modifier
) {
    // Read theme colors in composition phase (stable references passed to Canvas)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // FIX #7: all chart metrics memoized — only recomputed when records change
    val metrics = remember(records) {
        val prices = records.map { it.price }
        val minPrice = prices.min()
        val maxPrice = prices.max()
        val priceRange = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice
        val pricePad = priceRange * 0.08
        ChartMetrics(
            minPrice = minPrice,
            maxPrice = maxPrice,
            dispMin = minPrice - pricePad,
            dispRange = priceRange + pricePad * 2,
            gridLevels = listOf(minPrice, (minPrice + maxPrice) / 2, maxPrice),
            labelIndices = when {
                records.size <= 1 -> listOf(0)
                records.size <= 3 -> listOf(0, records.size - 1)
                else -> listOf(0, records.size / 2, records.size - 1)
            }
        )
    }

    // FIX #8: text measurement done in composition phase, NOT inside the Canvas draw block.
    // textMeasurer.measure() is expensive (full text layout). Moving it here means it only
    // runs when records or labelColor changes, not on every draw frame.
    val labelTextStyle = remember(labelColor) { TextStyle(fontSize = 8.sp, color = labelColor) }

    val yAxisLabels: List<TextLayoutResult> = remember(metrics.gridLevels, labelColor) {
        metrics.gridLevels.map { level ->
            textMeasurer.measure(currencyFormat.format(level), labelTextStyle)
        }
    }
    val xAxisLabels: List<TextLayoutResult> = remember(metrics.labelIndices, records, labelColor) {
        metrics.labelIndices.map { i ->
            textMeasurer.measure(dateFormat.format(Date(records[i].timestamp)), labelTextStyle)
        }
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = surfaceVariantColor)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text("Evolución de precios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                val leftPad = 68f; val rightPad = 8f
                val topPad = 12f; val bottomPad = 32f
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad

                // Pure functions — no state reads here (no extra invalidations)
                fun priceToY(p: Double): Float =
                    topPad + chartH * (1f - ((p - metrics.dispMin) / metrics.dispRange).toFloat())

                fun indexToX(i: Int): Float =
                    if (records.size == 1) leftPad + chartW / 2f
                    else leftPad + (i.toFloat() / (records.size - 1)) * chartW

                // Grid lines + pre-measured Y-axis labels
                metrics.gridLevels.forEachIndexed { gi, level ->
                    val y = priceToY(level)
                    var x = leftPad
                    while (x < leftPad + chartW) {
                        drawLine(gridColor, Offset(x, y), Offset(minOf(x + 10f, leftPad + chartW), y), 1f)
                        x += 18f
                    }
                    val measured = yAxisLabels[gi]
                    drawText(measured, topLeft = Offset(0f, y - measured.size.height / 2f))
                }

                val points = records.mapIndexed { i, r -> Offset(indexToX(i), priceToY(r.price)) }

                if (points.size >= 2) {
                    // Gradient fill area
                    val fillPath = Path().apply {
                        moveTo(points.first().x, topPad + chartH)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                        lineTo(points.last().x, topPad + chartH)
                        close()
                    }
                    drawPath(
                        fillPath,
                        Brush.verticalGradient(
                            listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0f)),
                            startY = topPad, endY = topPad + chartH
                        )
                    )
                    // Line
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }
                    drawPath(linePath, primaryColor, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // Data point dots
                points.forEach { pt ->
                    drawCircle(primaryColor, 5f, pt)
                    drawCircle(Color.White, 2.5f, pt)
                }

                // Pre-measured X-axis labels
                metrics.labelIndices.forEachIndexed { gi, i ->
                    val measured = xAxisLabels[gi]
                    val rawX = indexToX(i) - measured.size.width / 2f
                    drawText(
                        measured,
                        topLeft = Offset(
                            rawX.coerceIn(leftPad, size.width - measured.size.width),
                            topPad + chartH + 6f
                        )
                    )
                }
            }
        }
    }
}

// ── Swipe-to-delete wrapper ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteHistoryItem(
    record: RegistroPrecio,
    previousPrice: Double?,
    onDelete: (RegistroPrecio) -> Unit
) {
    // FIX #9: rememberUpdatedState ensures confirmValueChange always calls the LATEST onDelete
    // even though rememberSwipeToDismissBoxState captures it only once on creation.
    val latestOnDelete = rememberUpdatedState(onDelete)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                latestOnDelete.value(record)
                true
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(bgColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        HistoryItem(price = record.price, timestamp = record.timestamp, previousPrice = previousPrice)
    }
}

// ── History item card ──────────────────────────────────────────────────────

@Composable
fun HistoryItem(price: Double, timestamp: Long, previousPrice: Double? = null) {
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale.getDefault()) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // FIX #10: memoize all derived values — avoids re-format/re-compute on each recomposition
    val formattedDate = remember(timestamp) { dateFormat.format(Date(timestamp)) }
    val formattedPrice = remember(price) { currencyFormat.format(price) }
    val variationPct = remember(price, previousPrice) {
        previousPrice?.let { if (it != 0.0) (price - it) / it * 100 else null }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formattedDate, style = MaterialTheme.typography.bodyMedium)
                if (variationPct != null && abs(variationPct) > 0.001) {
                    val isUp = variationPct > 0
                    val varColor = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${if (isUp) "▲" else "▼"} ${"%.1f".format(abs(variationPct))}% respecto al anterior",
                        style = MaterialTheme.typography.labelSmall,
                        color = varColor
                    )
                }
            }
            Text(
                text = formattedPrice,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}