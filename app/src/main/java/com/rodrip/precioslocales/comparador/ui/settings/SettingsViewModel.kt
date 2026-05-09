package com.rodrip.precioslocales.comparador.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.local.AppTheme
import com.rodrip.precioslocales.comparador.data.local.SyncPreferences
import com.rodrip.precioslocales.comparador.data.local.ThemePreferences
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import com.rodrip.precioslocales.comparador.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.rodrip.precioslocales.comparador.data.util.ConfigDiagnostic

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val result: SyncManager.SyncResult) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class DiagnosticState {
    object Idle : DiagnosticState()
    object Running : DiagnosticState()
    data class Done(val report: ConfigDiagnostic.DiagnosticReport) : DiagnosticState()
}

class SettingsViewModel(
    private val repository: MainRepository,
    private val themePreferences: ThemePreferences,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    val themeState: StateFlow<AppTheme> = themePreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    val lastSyncedAt: StateFlow<Long> = syncPreferences.lastSyncedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { themePreferences.saveTheme(theme) }
    }

    fun syncNow() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val result = syncManager.sync()
                _syncState.value = if (result.errors.isEmpty() || result.pushed > 0 || result.pulled > 0) {
                    SyncState.Success(result)
                } else if (result.errors.isNotEmpty()) {
                    SyncState.Error(result.errors.first())
                } else {
                    SyncState.Success(result)
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    // ── Diagnóstico de configuración ──────────────────────────────────────────

    private val _diagnosticState = MutableStateFlow<DiagnosticState>(DiagnosticState.Idle)
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState

    fun runDiagnostic() {
        if (_diagnosticState.value is DiagnosticState.Running) return
        viewModelScope.launch {
            _diagnosticState.value = DiagnosticState.Running
            val report = ConfigDiagnostic.run()
            _diagnosticState.value = DiagnosticState.Done(report)
        }
    }

    fun resetDiagnostic() {
        _diagnosticState.value = DiagnosticState.Idle
    }

    fun exportDataToCsv(context: Context, onResult: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = repository.exportDataToCsv(context)
            onResult(uri)
        }
    }
}
