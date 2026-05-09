package com.rodrip.precioslocales.comparador.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rodrip.precioslocales.comparador.MainApplication
import com.rodrip.precioslocales.comparador.data.local.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            app.repository,
            app.themePreferences,
            app.syncManager,
            app.syncPreferences
        )
    )

    val currentTheme by settingsViewModel.themeState.collectAsState()
    val syncState by settingsViewModel.syncState.collectAsState()
    val lastSyncedAt by settingsViewModel.lastSyncedAt.collectAsState()
    val diagnosticState by settingsViewModel.diagnosticState.collectAsState()
    var isExporting by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Apariencia ────────────────────────────────────────────────────
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = currentTheme == AppTheme.LIGHT,
                    onClick = { settingsViewModel.setTheme(AppTheme.LIGHT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = { Icon(Icons.Rounded.LightMode, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) }
                ) { Text("Claro") }
                SegmentedButton(
                    selected = currentTheme == AppTheme.DARK,
                    onClick = { settingsViewModel.setTheme(AppTheme.DARK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) }
                ) { Text("Oscuro") }
                SegmentedButton(
                    selected = currentTheme == AppTheme.SYSTEM,
                    onClick = { settingsViewModel.setTheme(AppTheme.SYSTEM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = { Icon(Icons.Rounded.SettingsSuggest, contentDescription = null, modifier = Modifier.size(SegmentedButtonDefaults.IconSize)) }
                ) { Text("Sistema") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Sincronización ────────────────────────────────────────────────
            Text("Sincronización de catálogo", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sincroniza el catálogo de productos con la nube. Los precios y locales siempre son privados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Barra de progreso visible durante sync
                    if (syncState is SyncState.Syncing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // Resultado de la última operación
                    when (val state = syncState) {
                        is SyncState.Success -> {
                            val r = state.result
                            SyncResultChip(
                                text = buildString {
                                    if (r.pushed > 0) append("⬆️ ${r.pushed} subidos  ")
                                    if (r.pulled > 0) append("☁️ ${r.pulled} recibidos  ")
                                    if (r.skippedDuplicates > 0) append("⚠️ ${r.skippedDuplicates} duplicados")
                                    if (r.pushed == 0 && r.pulled == 0 && r.skippedDuplicates == 0)
                                        append("✅ Todo al día")
                                },
                                isError = false
                            )
                        }
                        is SyncState.Error -> {
                            SyncResultChip(text = "❌ ${state.message}", isError = true)
                        }
                        else -> {}
                    }

                    // Última sincronización
                    if (lastSyncedAt > 0L) {
                        Text(
                            text = "Última sincronización: ${dateFormat.format(Date(lastSyncedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            settingsViewModel.resetSyncState()
                            settingsViewModel.syncNow()
                        },
                        enabled = syncState !is SyncState.Syncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizando…")
                        } else {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizar ahora")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Exportar CSV ──────────────────────────────────────────────────
            Text("Exportar Datos", style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!isExporting) {
                        isExporting = true
                        settingsViewModel.exportDataToCsv(context) { uri ->
                            isExporting = false
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir CSV"))
                            } else {
                                Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Exportar a CSV", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Comparte tus datos de precios en formato CSV",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isExporting) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Diagnóstico de configuración ──────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Diagnóstico", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Verifica que Firebase y Cloudinary estén correctamente configurados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (diagnosticState is DiagnosticState.Running) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Verificando servicios…", style = MaterialTheme.typography.bodySmall)
                    }

                    if (diagnosticState is DiagnosticState.Done) {
                        val report = (diagnosticState as DiagnosticState.Done).report
                        report.checks.forEach { check ->
                            DiagnosticCheckRow(check)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            settingsViewModel.resetDiagnostic()
                            settingsViewModel.runDiagnostic()
                        },
                        enabled = diagnosticState !is DiagnosticState.Running,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (diagnosticState is DiagnosticState.Running) "Verificando…" else "Verificar configuración")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "ComparaPreciosRodriP v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SyncResultChip(text: String, isError: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DiagnosticCheckRow(check: com.rodrip.precioslocales.comparador.data.util.ConfigDiagnostic.CheckResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (check.ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = if (check.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Column {
            Text(
                text = check.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (check.ok) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error
            )
            Text(
                text = check.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
