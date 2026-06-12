package com.budgettracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgettracker.app.ui.navigation.BudgetTrackerNavGraph
import com.budgettracker.app.ui.theme.BudgetTrackerTheme
import com.budgettracker.app.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BudgetTrackerApp
        val factory = TransactionViewModel.Factory(app.transactionRepository, app.preferencesRepository, app.categoryRepository)

        setContent {
            val vm: TransactionViewModel = viewModel(factory = factory)
            val themeMode   by vm.themeMode.collectAsState()
            val accentIndex by vm.accentIndex.collectAsState()

            BudgetTrackerTheme(themeMode = themeMode, accentIndex = accentIndex) {
                BudgetTrackerNavGraph(viewModel = vm)
            }
        }
    }
}
