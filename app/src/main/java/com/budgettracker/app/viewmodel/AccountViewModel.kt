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
    private val budgetRepo: BudgetRepository
) : ViewModel() {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter)
        .create()

    private val _account = MutableStateFlow(authManager.getSignedInAccount())
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    private val _status = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val status: StateFlow<BackupUiState> = _status.asStateFlow()

    fun getSignInIntent(): Intent = authManager.signInClient.signInIntent

    fun handleSignInResult(data: Intent?) {
        _account.value = authManager.handleSignInResult(data)
    }

    fun signOut() = viewModelScope.launch {
        authManager.signOut()
        _account.value = null
    }

    fun backupNow() = viewModelScope.launch {
        _status.value = BackupUiState.Loading
        runCatching {
            val token = authManager.getAccessToken() ?: error("Not signed in")
            val backup = BackupData(
                exportedAt            = Instant.now().toString(),
                transactions          = transactionRepo.transactions.first(),
                recurringTransactions = recurringRepo.recurring.first(),
                categories            = categoryRepo.categories.first(),
                budgets               = budgetRepo.budgets.first()
            )
            val ok = driveRepo.upload(gson.toJson(backup), token)
            if (!ok) error("Upload failed — check your connection and try again")
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
            categoryRepo.save(backup.categories)
            budgetRepo.replaceAll(backup.budgets)
        }.onSuccess {
            _status.value = BackupUiState.Success("Restored successfully")
        }.onFailure { e ->
            _status.value = BackupUiState.Error(e.message ?: "Restore failed")
        }
    }

    fun clearStatus() { _status.value = BackupUiState.Idle }

    class Factory(
        private val authManager: GoogleAuthManager,
        private val driveRepo: DriveBackupRepository,
        private val transactionRepo: TransactionRepository,
        private val recurringRepo: RecurringTransactionRepository,
        private val categoryRepo: CategoryRepository,
        private val budgetRepo: BudgetRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(
                authManager, driveRepo, transactionRepo, recurringRepo, categoryRepo, budgetRepo
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
