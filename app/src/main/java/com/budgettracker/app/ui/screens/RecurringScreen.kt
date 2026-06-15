package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.*
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.format.DateTimeFormatter

private val shortDate = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun RecurringScreen(
    viewModel: TransactionViewModel,
    onEditRecurring: (RecurringTransaction) -> Unit = {}
) {
    val recurring      by viewModel.recurringTransactions.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current

    val active   = remember(recurring) { recurring.filter { it.status == RecurringStatus.ACTIVE } }
    val inactive = remember(recurring) { recurring.filter { it.status != RecurringStatus.ACTIVE } }

    if (recurring.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text("No recurring payments yet", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to add one", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        return
    }

    LazyColumn(
        modifier        = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding  = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("Upcoming & Active")
        }

        if (active.isEmpty()) {
            item {
                Text("No active recurring payments",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            }
        } else {
            items(active, key = { it.id }) { rec ->
                val categoryColor = viewModel.categoryFor(rec.category)?.color
                    ?: MaterialTheme.colorScheme.primary
                val categoryIconId = viewModel.categoryFor(rec.category)?.iconId ?: "more_horiz"
                RecurringCard(
                    rec            = rec,
                    categoryColor  = categoryColor,
                    categoryIconId = categoryIconId,
                    currencySymbol = currencySymbol,
                    onEdit         = { onEditRecurring(rec) },
                    onPauseResume  = {
                        viewModel.updateRecurring(rec.copy(status = RecurringStatus.PAUSED))
                    },
                    onDelete       = { viewModel.deleteRecurring(rec.id) }
                )
            }
        }

        if (inactive.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Paused & Finished")
            }
            items(inactive, key = { it.id }) { rec ->
                val categoryColor  = viewModel.categoryFor(rec.category)?.color
                    ?: MaterialTheme.colorScheme.primary
                val categoryIconId = viewModel.categoryFor(rec.category)?.iconId ?: "more_horiz"
                RecurringCard(
                    rec            = rec,
                    categoryColor  = categoryColor,
                    categoryIconId = categoryIconId,
                    currencySymbol = currencySymbol,
                    faded          = true,
                    onEdit         = { onEditRecurring(rec) },
                    onPauseResume  = if (rec.status == RecurringStatus.PAUSED) ({
                        viewModel.updateRecurring(rec.copy(status = RecurringStatus.ACTIVE))
                    }) else null,
                    onDelete       = { viewModel.deleteRecurring(rec.id) }
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun RecurringCard(
    rec: RecurringTransaction,
    categoryColor: Color,
    categoryIconId: String,
    currencySymbol: String,
    faded: Boolean = false,
    onEdit: () -> Unit,
    onPauseResume: (() -> Unit)?,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (faded) 0.5f else 1f)
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconById(categoryIconId), contentDescription = null, tint = categoryColor, modifier = Modifier.size(22.dp))
            }

            // Title + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(rec.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                Text(rec.frequencyLabel(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rec.status == RecurringStatus.ACTIVE) {
                    Text("Next: ${rec.nextDueDate.format(shortDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(rec.status.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Amount
            Text(
                "${if (rec.type == TransactionType.EXPENSE) "-" else "+"}$currencySymbol ${"%.2f".format(rec.amount)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (rec.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
            )

            // Actions
            if (onPauseResume != null) {
                IconButton(onClick = onPauseResume, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (rec.status == RecurringStatus.ACTIVE) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (rec.status == RecurringStatus.ACTIVE) "Pause" else "Resume",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete",
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete recurring payment?") },
            text             = { Text("This will stop future generations. Already-generated transactions are kept.") },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}
