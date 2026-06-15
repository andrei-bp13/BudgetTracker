package com.pardonmyspending.app.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.pardonmyspending.app.data.auth.GoogleAuthManager
import com.pardonmyspending.app.data.model.BackupData
import com.pardonmyspending.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

sealed class BackupUiState {
    object Idle    : BackupUiState()
    object Loading : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String)   : BackupUiState()
}

class AccountViewModel(
    private val authManager: GoogleAuthManager,
    private val driveRepo: DriveBackupRepository,
    private val transactionRepo: TransactionRepository,
    private val recurringRepo: RecurringTransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetRepo: BudgetRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter)
        .create()

    private val listOfStringType = object : TypeToken<List<String>>() {}.type
    private val mapOfStringBoolType = object : TypeToken<Map<String, Boolean>>() {}.type

    private val _account = MutableStateFlow(authManager.getSignedInAccount())
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    private val _status = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val status: StateFlow<BackupUiState> = _status.asStateFlow()

    private val _localBackupStatus = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val localBackupStatus: SharedFlow<String> = _localBackupStatus.asSharedFlow()

    fun getSignInIntent(): Intent = authManager.signInClient.signInIntent

    fun handleSignInResult(data: Intent?) {
        _account.value = authManager.handleSignInResult(data)
    }

    fun signOut() = viewModelScope.launch {
        authManager.signOut()
        _account.value = null
    }

    // â"€â"€ Helpers â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private suspend fun currentSettings(): BackupData {
        val cardOrderStr   = prefsRepo.cardOrderJson.first()
        val cardVisibleStr = prefsRepo.cardVisibleJson.first()
        val cardOrder: List<String> = if (cardOrderStr.isEmpty())
            TransactionViewModel.DEFAULT_CARD_ORDER
        else
            gson.fromJson(cardOrderStr, listOfStringType)
        val cardVisible: Map<String, Boolean> = if (cardVisibleStr.isEmpty())
            TransactionViewModel.DEFAULT_CARD_VISIBLE
        else
            gson.fromJson(cardVisibleStr, mapOfStringBoolType)

        return BackupData(
            hasSettings     = true,
            themeMode       = prefsRepo.themeMode.first(),
            accentIndex     = prefsRepo.accentColorIndex.first(),
            customAccentHex = prefsRepo.customAccentHex.first(),
            globalColoring  = prefsRepo.globalColoring.first(),
            granularColoring = prefsRepo.granularColoring.first(),
            currencyCode    = prefsRepo.currencyCode.first(),
            chartGranularity = prefsRepo.chartGranularity.first(),
            aiMode          = prefsRepo.aiMode.first(),
            cardOrder       = cardOrder,
            cardVisible     = cardVisible
        )
    }

    private suspend fun applySettings(backup: BackupData) {
        if (!backup.hasSettings) return
        prefsRepo.setThemeMode(backup.themeMode)
        prefsRepo.setAccentColorIndex(backup.accentIndex)
        prefsRepo.setCustomAccentHex(backup.customAccentHex)
        prefsRepo.setGlobalColoring(backup.globalColoring)
        prefsRepo.setGranularColoring(backup.granularColoring)
        prefsRepo.setCurrencyCode(backup.currencyCode)
        prefsRepo.setChartGranularity(backup.chartGranularity)
        prefsRepo.setAiMode(backup.aiMode)
        if (backup.cardOrder.isNotEmpty())  prefsRepo.setCardOrder(gson.toJson(backup.cardOrder))
        if (backup.cardVisible.isNotEmpty()) prefsRepo.setCardVisible(gson.toJson(backup.cardVisible))
    }

    // â"€â"€ Google Drive backup / restore â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    fun backupNow() = viewModelScope.launch {
        _status.value = BackupUiState.Loading
        runCatching {
            val token   = authManager.getAccessToken() ?: error("Not signed in")
            val settings = currentSettings()
            val backup  = BackupData(
                exportedAt            = Instant.now().toString(),
                transactions          = transactionRepo.transactions.first(),
                recurringTransactions = recurringRepo.recurring.first(),
                categories            = categoryRepo.categories.first(),
                budgets               = budgetRepo.budgets.first(),
                hasSettings           = settings.hasSettings,
                themeMode             = settings.themeMode,
                accentIndex           = settings.accentIndex,
                customAccentHex       = settings.customAccentHex,
                globalColoring        = settings.globalColoring,
                granularColoring      = settings.granularColoring,
                currencyCode          = settings.currencyCode,
                chartGranularity      = settings.chartGranularity,
                aiMode                = settings.aiMode,
                cardOrder             = settings.cardOrder,
                cardVisible           = settings.cardVisible
            )
            val ok = driveRepo.upload(gson.toJson(backup), token)
            if (!ok) error("Upload failed - check your connection and try again")
        }.onSuccess {
            _status.value = BackupUiState.Success("Backup complete")
        }.onFailure { e ->
            _status.value = BackupUiState.Error(e.message ?: "Backup failed")
        }
    }

    fun restoreLatest() = viewModelScope.launch {
        _status.value = BackupUiState.Loading
        runCatching {
            val token  = authManager.getAccessToken() ?: error("Not signed in")
            val json   = driveRepo.downloadLatest(token) ?: error("No backup found on Drive")
            val backup = gson.fromJson(json, BackupData::class.java)
            transactionRepo.replaceAll(backup.transactions)
            recurringRepo.replaceAll(backup.recurringTransactions)
            if (backup.categories.isNotEmpty()) categoryRepo.save(backup.categories)
            budgetRepo.replaceAll(backup.budgets)
            applySettings(backup)
        }.onSuccess {
            _status.value = BackupUiState.Success("Restored successfully")
        }.onFailure { e ->
            _status.value = BackupUiState.Error(e.message ?: "Restore failed")
        }
    }

    // â"€â"€ Local file backup / restore â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    /** Builds a full local backup JSON string (no transactions -" use CSV for those). */
    suspend fun buildLocalBackupJson(): String {
        val settings = currentSettings()
        val backup = BackupData(
            version               = 2,
            exportedAt            = Instant.now().toString(),
            recurringTransactions = recurringRepo.recurring.first(),
            categories            = categoryRepo.categories.first(),
            budgets               = budgetRepo.budgets.first(),
            hasSettings           = settings.hasSettings,
            themeMode             = settings.themeMode,
            accentIndex           = settings.accentIndex,
            customAccentHex       = settings.customAccentHex,
            globalColoring        = settings.globalColoring,
            granularColoring      = settings.granularColoring,
            currencyCode          = settings.currencyCode,
            chartGranularity      = settings.chartGranularity,
            aiMode                = settings.aiMode,
            cardOrder             = settings.cardOrder,
            cardVisible           = settings.cardVisible
        )
        return gson.toJson(backup)
    }

    fun applyLocalBackup(json: String) = viewModelScope.launch {
        runCatching {
            val backup = gson.fromJson(json, BackupData::class.java)
            if (backup.categories.isNotEmpty())            categoryRepo.save(backup.categories)
            if (backup.budgets.isNotEmpty())               budgetRepo.replaceAll(backup.budgets)
            if (backup.recurringTransactions.isNotEmpty()) recurringRepo.replaceAll(backup.recurringTransactions)
            applySettings(backup)
        }.onSuccess {
            _localBackupStatus.tryEmit("Backup restored")
        }.onFailure { e ->
            _localBackupStatus.tryEmit("Restore failed: ${e.message}")
        }
    }

    fun clearStatus() { _status.value = BackupUiState.Idle }

    class Factory(
        private val authManager: GoogleAuthManager,
        private val driveRepo: DriveBackupRepository,
        private val transactionRepo: TransactionRepository,
        private val recurringRepo: RecurringTransactionRepository,
        private val categoryRepo: CategoryRepository,
        private val budgetRepo: BudgetRepository,
        private val prefsRepo: PreferencesRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(
                authManager, driveRepo, transactionRepo, recurringRepo, categoryRepo, budgetRepo, prefsRepo
            ) as T
        }
    }

    private object LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        override fun serialize(src: LocalDate, t: Type, ctx: JsonSerializationContext) =
            JsonPrimitive(src.toEpochDay())
        override fun deserialize(json: JsonElement, t: Type, ctx: JsonDeserializationContext) =
            LocalDate.ofEpochDay(json.asLong)
    }

    private object LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        override fun serialize(src: LocalTime, t: Type, ctx: JsonSerializationContext) =
            JsonPrimitive(src.toSecondOfDay().toLong())
        override fun deserialize(json: JsonElement, t: Type, ctx: JsonDeserializationContext) =
            LocalTime.ofSecondOfDay(json.asLong)
    }
}
