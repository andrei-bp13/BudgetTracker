package com.budgettracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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

        setContent {
            val vm: TransactionViewModel = viewModel(factory = factory)
            val dataLoaded by vm.dataLoaded.collectAsState()

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
                if (dataLoaded) {
                    BudgetTrackerNavGraph(viewModel = vm)
                } else {
                    // Hold a solid surface while Room loads so the window never
                    // shows as empty/black between the splash exit and first data frame.
                    Surface(modifier = Modifier.fillMaxSize()) {}
                }
            }
        }
    }
}
