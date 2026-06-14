package com.budgettracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.budgettracker.app.ui.navigation.BudgetTrackerNavGraph
import com.budgettracker.app.ui.theme.BudgetTrackerTheme
import com.budgettracker.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.combine

private data class AppThemeState(
    val themeMode: String,
    val accentIndex: Int,
    val customAccentHex: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BudgetTrackerApp
        val factory = TransactionViewModel.Factory(
            app.transactionRepository,
            app.preferencesRepository,
            app.categoryRepository,
            app.recurringRepository
        )

        // Create VM before setContent so the splash condition can observe it.
        val vm = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

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
                BudgetTrackerNavGraph(viewModel = vm)
            }
        }
    }
}
