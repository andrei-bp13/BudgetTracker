package com.budgettracker.app.viewmodel

import com.budgettracker.app.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Encapsulates all UI-preference state separately from business data.
 * Borrows [scope] from its owner (TransactionViewModel.viewModelScope).
 *
 * Theme flows (themeMode, accentIndex, customAccentHex) use Eagerly because
 * MainActivity reads them synchronously via collectAsState before the first
 * frame. All other pref flows use WhileSubscribed(5s) so they release their
 * DataStore collector when no UI is subscribed.
 */
internal class PreferencesState(
    private val prefs: PreferencesRepository,
    private val scope: CoroutineScope
) {
    val themeMode: StateFlow<String> = prefs.themeMode
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "system")

    val accentIndex: StateFlow<Int> = prefs.accentColorIndex
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val customAccentHex: StateFlow<String> = prefs.customAccentHex
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "2196F3")

    val globalColoring: StateFlow<Boolean> = prefs.globalColoring
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), false)

    val granularColoring: StateFlow<Boolean> = prefs.granularColoring
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), true)

    val currencyCode: StateFlow<String> = prefs.currencyCode
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), "USD")

    val chartGranularity: StateFlow<String> = prefs.chartGranularity
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), "monthly")

    val aiMode: StateFlow<Boolean> = prefs.aiMode
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), false)

    fun setThemeMode(mode: String)         = scope.launch { prefs.setThemeMode(mode) }
    fun setAccentIndex(i: Int)             = scope.launch { prefs.setAccentColorIndex(i) }
    fun setCustomAccentHex(hex: String)    = scope.launch { prefs.setCustomAccentHex(hex) }
    fun setGlobalColoring(v: Boolean)      = scope.launch { prefs.setGlobalColoring(v) }
    fun setGranularColoring(v: Boolean)    = scope.launch { prefs.setGranularColoring(v) }
    fun setCurrencyCode(code: String)      = scope.launch { prefs.setCurrencyCode(code) }
    fun setChartGranularity(value: String) = scope.launch { prefs.setChartGranularity(value) }
    fun setAiMode(enabled: Boolean)        = scope.launch { prefs.setAiMode(enabled) }
}
