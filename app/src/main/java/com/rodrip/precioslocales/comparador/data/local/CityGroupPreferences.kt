package com.rodrip.precioslocales.comparador.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persiste el conjunto de departamentos que el usuario quiere ver en su vista de locales.
 * Un conjunto vacío ( = cadena vacía ) significa "mostrar todos los departamentos".
 */
class CityGroupPreferences(private val context: Context) {

    companion object {
        private val ACTIVE_DEPARTMENTS_KEY = stringPreferencesKey("active_departments")
        private const val SEPARATOR = "|"
    }

    /** Flow con el Set<String> de departamentos activos; vacío → todos activos. */
    val activeDepartmentsFlow: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[ACTIVE_DEPARTMENTS_KEY] ?: ""
            if (raw.isBlank()) emptySet()
            else raw.split(SEPARATOR).filter { it.isNotEmpty() }.toSet()
        }

    /** Suspending variant para usarla desde coroutines sin Flow. */
    suspend fun getActiveDepartmentsOnce(): Set<String> = activeDepartmentsFlow.first()

    /** Reemplaza todo el conjunto de departamentos activos. */
    suspend fun setActiveDepartments(departments: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_DEPARTMENTS_KEY] = departments.joinToString(SEPARATOR)
        }
    }

    /** Activa o desactiva un departamento individualmente. */
    suspend fun toggleDepartment(dept: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[ACTIVE_DEPARTMENTS_KEY] ?: "")
                .split(SEPARATOR).filter { it.isNotEmpty() }.toMutableSet()
            if (dept in current) current.remove(dept) else current.add(dept)
            prefs[ACTIVE_DEPARTMENTS_KEY] = current.joinToString(SEPARATOR)
        }
    }

    /** True cuando el conjunto está vacío (= mostrar todo). */
    fun isShowAll(active: Set<String>): Boolean = active.isEmpty()
}

