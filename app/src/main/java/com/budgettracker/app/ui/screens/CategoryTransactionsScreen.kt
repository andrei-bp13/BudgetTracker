package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    categoryName: String,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val allTransactions  by viewModel.transactions.collectAsState()
    val currencySymbol   = LocalCurrencySymbol.current
    val cat      = viewModel.categoryFor(categoryName)
    val color    = cat?.color ?: MaterialTheme.colorScheme.primary
    val iconId   = cat?.iconId ?: "more_horiz"

    val filtered = remember(allTransactions, categoryName) {
        allTransactions
            .filter { it.category == categoryName }
            .sortedByDescending { it.date }
    }

    val total = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    // Group by date
    val grouped: Map<LocalDate, List<Transaction>> = remember(filtered) {
        filtered.groupBy { it.date }
    }
    val sortedDates = grouped.keys.sortedDescending()

    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMM d")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconById(iconId), contentDescription = null,
                                tint = color, modifier = Modifier.size(18.dp))
                        }
                        Text(categoryName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Summary chip
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total spent",
                                style = MaterialTheme.typography.labelMedium,
                                color = color.copy(alpha = 0.7f))
                            Text("$currencySymbol ${"%.2f".format(total)}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = color)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${filtered.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = color
                            )
                            Text(
                                "transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = color.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Grouped by date
            sortedDates.forEach { date ->
                val dayTxns = grouped[date] ?: return@forEach

                item(key = date.toString()) {
                    Text(
                        text = date.format(dateFmt),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }

                items(dayTxns, key = { it.id }) { txn ->
                    TransactionRow(txn, color, currencySymbol)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction, color: Color, currencySymbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            if (txn.note.isNotBlank()) {
                Text(txn.note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = "${if (txn.type == TransactionType.EXPENSE) "-" else "+"}$currencySymbol ${"%.2f".format(txn.amount)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (txn.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else color
        )
    }
}
