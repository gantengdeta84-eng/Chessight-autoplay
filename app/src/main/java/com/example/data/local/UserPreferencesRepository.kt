package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chesssight_prefs")

data class EnginePreferences(
    val engineDepth: Int = 14,
    val variantsCount: Int = 2,
    val offlineMode: Boolean = false,
    val defaultWhiteBottom: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ENGINE_DEPTH = intPreferencesKey("engine_depth")
        val VARIANTS_COUNT = intPreferencesKey("variants_count")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val DEFAULT_WHITE_BOTTOM = booleanPreferencesKey("default_white_bottom")
    }

    val preferences: Flow<EnginePreferences> = context.dataStore.data.map { prefs ->
        EnginePreferences(
            engineDepth = (prefs[Keys.ENGINE_DEPTH] ?: 14).coerceIn(10, 18),
            variantsCount = (prefs[Keys.VARIANTS_COUNT] ?: 2).coerceIn(1, 3),
            offlineMode = prefs[Keys.OFFLINE_MODE] ?: false,
            defaultWhiteBottom = prefs[Keys.DEFAULT_WHITE_BOTTOM] ?: true
        )
    }

    suspend fun updateEngineDepth(depth: Int) {
        context.dataStore.edit { it[Keys.ENGINE_DEPTH] = depth.coerceIn(10, 18) }
    }

    suspend fun updateVariantsCount(count: Int) {
        context.dataStore.edit { it[Keys.VARIANTS_COUNT] = count.coerceIn(1, 3) }
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun updateDefaultWhiteBottom(whiteBottom: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_WHITE_BOTTOM] = whiteBottom }
    }
}
