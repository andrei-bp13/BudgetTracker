package com.budgettracker.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        val THEME_MODE        = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR      = intPreferencesKey("accent_color")
        val GLOBAL_COLORING   = booleanPreferencesKey("global_coloring")
        val GRANULAR_COLORING = booleanPreferencesKey("granular_coloring")
        val CURRENCY_CODE     = stringPreferencesKey("currency_code")
        const val DEFAULT_ACCENT = 0
    }

    val themeMode: Flow<String>      = context.dataStore.data.map { it[THEME_MODE]      ?: "system" }
    val accentColorIndex: Flow<Int>  = context.dataStore.data.map { it[ACCENT_COLOR]    ?: DEFAULT_ACCENT }
    val currencyCode: Flow<String>   = context.dataStore.data.map { it[CURRENCY_CODE]   ?: "USD" }

    // false = off, true = on. Only one should be active at a time.
    val globalColoring: Flow<Boolean>   = context.dataStore.data.map { it[GLOBAL_COLORING]   ?: false }
    val granularColoring: Flow<Boolean> = context.dataStore.data.map { it[GRANULAR_COLORING] ?: true }

    suspend fun setThemeMode(mode: String)      { context.dataStore.edit { it[THEME_MODE]   = mode  } }
    suspend fun setAccentColorIndex(index: Int) { context.dataStore.edit { it[ACCENT_COLOR] = index } }
    suspend fun setCurrencyCode(code: String)   { context.dataStore.edit { it[CURRENCY_CODE] = code } }

    suspend fun setGlobalColoring(enabled: Boolean) {
        context.dataStore.edit {
            it[GLOBAL_COLORING]  = enabled
            if (enabled) it[GRANULAR_COLORING] = false
        }
    }

    suspend fun setGranularColoring(enabled: Boolean) {
        context.dataStore.edit {
            it[GRANULAR_COLORING] = enabled
            if (enabled) it[GLOBAL_COLORING] = false
        }
    }
}
