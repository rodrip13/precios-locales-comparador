package com.rodrip.precioslocales.comparador.ui.stores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodrip.precioslocales.comparador.data.local.entity.LocalComercial
import com.rodrip.precioslocales.comparador.data.model.UruguayGeo
import com.rodrip.precioslocales.comparador.data.remote.RemoteStoreDto
import com.rodrip.precioslocales.comparador.data.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ─── Estado de la verificación anti-duplicados ────────────────────────────────

sealed class StoreCheckState {
    object Idle : StoreCheckState()
    object Checking : StoreCheckState()
    data class Clear(val pendingStore: LocalComercial) : StoreCheckState()
    data class LocalDuplicate(
        val existing: LocalComercial,
        val pendingStore: LocalComercial,
        val distanceMeters: Double
    ) : StoreCheckState()
    data class RemoteDuplicate(
        val dto: RemoteStoreDto,
        val pendingStore: LocalComercial,
        val distanceMeters: Double
    ) : StoreCheckState()
    object Saved : StoreCheckState()
}

class StoreViewModel(private val repository: MainRepository) : ViewModel() {

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _sortByDistance = MutableStateFlow(false)
    val sortByDistance: StateFlow<Boolean> = _sortByDistance.asStateFlow()

    val activeDepartments: StateFlow<Set<String>> = repository.getActiveDepartmentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val stores: StateFlow<List<LocalComercial>> = combine(
        repository.getAllStores(),
        activeDepartments,
        _userLocation,
        _sortByDistance
    ) { allStores, activeDepts, location, sort ->
        val filtered = if (activeDepts.isEmpty()) allStores
        else allStores.filter { it.department in activeDepts }
        if (sort && location != null)
            filtered.sortedBy { UruguayGeo.haversineMeters(location.first, location.second, it.latitude, it.longitude) }
        else filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _checkState = MutableStateFlow<StoreCheckState>(StoreCheckState.Idle)
    val checkState: StateFlow<StoreCheckState> = _checkState.asStateFlow()

    fun checkBeforeSave(
        id: Long = 0L,
        name: String, address: String, hours: String,
        department: String, localidad: String,
        latitude: Double, longitude: Double
    ) {
        viewModelScope.launch {
            _checkState.value = StoreCheckState.Checking
            val pendingStore = LocalComercial(
                id = id, name = name.trim(), address = address.trim(),
                hours = hours.trim(), department = department, localidad = localidad.trim(),
                latitude = latitude, longitude = longitude,
                updatedAt = System.currentTimeMillis()
            )
            // Al editar, omitir la validación duplicados
            if (id != 0L) {
                _checkState.value = StoreCheckState.Clear(pendingStore); return@launch
            }
            val delta = 0.001
            if (latitude != 0.0 || longitude != 0.0) {
                val locals = repository.findNearbyStoresByName(
                    name, latitude - delta, latitude + delta, longitude - delta, longitude + delta, excludeId = id
                )
                if (locals.isNotEmpty()) {
                    val dup = locals.first()
                    _checkState.value = StoreCheckState.LocalDuplicate(
                        dup, pendingStore,
                        UruguayGeo.haversineMeters(latitude, longitude, dup.latitude, dup.longitude)
                    ); return@launch
                }
                try {
                    val remoteDto = repository.checkRemoteStoreDuplicate(name, latitude, longitude)
                    if (remoteDto != null) {
                        _checkState.value = StoreCheckState.RemoteDuplicate(
                            remoteDto, pendingStore,
                            UruguayGeo.haversineMeters(latitude, longitude, remoteDto.latitude, remoteDto.longitude)
                        ); return@launch
                    }
                } catch (_: Exception) { /* sin internet → continuar */ }
            }
            _checkState.value = StoreCheckState.Clear(pendingStore)
        }
    }

    fun confirmSave(store: LocalComercial, linkedRemoteId: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val toSave = store.copy(remoteId = linkedRemoteId ?: store.remoteId, updatedAt = System.currentTimeMillis())
            if (toSave.id == 0L) repository.insertStore(toSave) else repository.updateStore(toSave)
            _checkState.value = StoreCheckState.Saved
            onSuccess()
        }
    }

    fun resetCheckState() { _checkState.value = StoreCheckState.Idle }

    fun setUserLocation(lat: Double, lon: Double) {
        _userLocation.value = Pair(lat, lon); _sortByDistance.value = true
    }

    fun clearDistanceSort() {
        _userLocation.value = null; _sortByDistance.value = false
    }

    fun toggleDepartment(dept: String) {
        viewModelScope.launch { repository.toggleDepartment(dept) }
    }

    suspend fun getStoreById(id: Long): LocalComercial? = repository.getStoreById(id)
    fun searchStores(query: String): Flow<List<LocalComercial>> = repository.searchStores(query)
    fun deleteStore(store: LocalComercial) { viewModelScope.launch { repository.deleteStore(store) } }
}