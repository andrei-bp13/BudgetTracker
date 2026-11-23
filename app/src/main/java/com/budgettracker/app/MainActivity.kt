package com.pardonmyspending.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.pardonmyspending.app.ui.navigation.BudgetTrackerNavGraph
import com.pardonmyspending.app.ui.theme.BudgetTrackerTheme
import com.pardonmyspending.app.viewmodel.AccountViewModel
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import com.pardonmyspending.app.widget.EXTRA_CALENDAR_DATE_NAME
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

private data class AppThemeState(
    val themeMode: String,
    val accentIndex: Int,
    val customAccentHex: String
)

class MainActivity : ComponentActivity() {
    private var transactionVm: TransactionViewModel? = null
    private val pendingCalendarDate = mutableStateOf<LocalDate?>(null)

    private fun extractCalendarDate(intent: Intent?) {
        val raw = intent?.getStringExtra(EXTRA_CALENDAR_DATE_NAME) ?: return
        runCatching { LocalDate.parse(raw) }.onSuccess { date ->
            pendingCalendarDate.value = date
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractCalendarDate(intent)

        val app = application as BudgetTrackerApp
        val factory = TransactionViewModel.Factory(
            app.transactionRepository,
            app.preferencesRepository,
            app.categoryRepository,
            app.recurringRepository,
            app.budgetRepository,
            app.applicationContext
        )

        // Create VM before setContent so the splash condition can observe it.
        val vm = ViewModelProvider(this, factory)[TransactionViewModel::class.java]
        transactionVm = vm

        val accountFactory = AccountViewModel.Factory(
            app.googleAuthManager,
            app.driveBackupRepository,
            app.transactionRepository,
            app.recurringRepository,
            app.categoryRepository,
            app.budgetRepository,
            app.preferencesRepository,
            app.database,
            app.applicationContext
        )
        val accountVm = ViewModelProvider(this, accountFactory)[AccountViewModel::class.java]

        // Hold the splash until theme prefs + repo data are ready.
        // processRecurring runs in its own coroutine and does not block dataLoaded.
        installSplashScreen().setKeepOnScreenCondition { !vm.dataLoaded.value }

        setContent {
            val themeFlow = remember(vm) {
                combine(vm.themeMode, vm.accentIndex, vm.customAccentHex) { mode, accent, hex ->
                    AppThemeState(mode, accent, hex)
                }
            }
            val themeState by themeFlow.collectAsState(initial = AppThemeState("system", 0, "2196F3"))

            BudgetTrackerTheme(
                themeMode       = themeState.themeMode,
                accentIndex     = themeState.accentIndex,
                customAccentHex = themeState.customAccentHex
            ) {
                BudgetTrackerNavGraph(
                    viewModel             = vm,
                    accountViewModel      = accountVm,
                    pendingCalendarDate   = pendingCalendarDate.value,
                    onCalendarDateConsumed = { pendingCalendarDate.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractCalendarDate(intent)
    }

    override fun onResume() {
        super.onResume()
        transactionVm?.processRecurringIfNeeded()
    }
}
