package com.rodrip.precioslocales.comparador.data.local

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SyncPreferences(private val context: Context) {

    companion object {
        val LAST_SYNCED_AT_KEY = longPreferencesKey("last_synced_at")
        val CONTRIBUTOR_ID_KEY = stringPreferencesKey("contributor_id")
    }

    val lastSyncedAt: Flow<Long> = context.dataStore.data
        .map { it[LAST_SYNCED_AT_KEY] ?: 0L }

    val contributorId: Flow<String?> = context.dataStore.data
        .map { it[CONTRIBUTOR_ID_KEY] }

    suspend fun saveLastSyncedAt(timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNCED_AT_KEY] = timestamp }
    }

    suspend fun saveContributorId(uid: String) {
        context.dataStore.edit { it[CONTRIBUTOR_ID_KEY] = uid }
    }

    suspend fun getLastSyncedAtOnce(): Long =
        context.dataStore.data.map { it[LAST_SYNCED_AT_KEY] ?: 0L }.first()
}


