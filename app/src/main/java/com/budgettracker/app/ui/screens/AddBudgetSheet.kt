package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBudgetSheet(
    viewModel: TransactionViewModel,
    initial: Budget? = null,
    onDismiss: () -> Unit
) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val currencySymbol    = LocalCurrencySymbol.current
    val isEditing         = initial != null

    var title        by remember { mutableStateOf(initial?.title ?: "") }
    var amount       by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var period       by remember { mutableStateOf(initial?.period ?: BudgetPeriod.MONTHLY) }
    var selectedCat  by remember { mutableStateOf(initial?.categoryName ?: expenseCategories.firstOrNull()?.name ?: "") }

    val onSave = save@{
        val titleVal  = title.trim()
        val amountVal = amount.toDoubleOrNull() ?: return@save
        if (titleVal.isBlank() || selectedCat.isBlank()) return@save
        val budget = Budget(
            id              = initial?.id ?: UUID.randomUUID().toString(),
            title           = titleVal,
            categoryName    = selectedCat,
            amount          = amountVal,
            period          = period,
            createdEpochDay = initial?.createdEpochDay ?: LocalDate.now().toEpochDay(),
            sortOrder       = initial?.sortOrder ?: Int.MAX_VALUE
        )
        if (isEditing) viewModel.updateBudget(budget) else viewModel.addBudget(budget)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
        modifier         = Modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEditing) "Edit Budget" else "New Budget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                // ── Title ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Budget name") },
                    textStyle     = MaterialTheme.typography.bodyMedium,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.medium
                )

                // ── Amount ────────────────────────────────────────────────────
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label           = { Text("Budget limit") },
                    textStyle       = MaterialTheme.typography.bodyMedium,
                    prefix          = { Text("$currencySymbol ", style = MaterialTheme.typography.bodyMedium) },
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    shape           = MaterialTheme.shapes.medium
                )

                // ── Period ────────────────────────────────────────────────────
                Text("Resets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val periods = BudgetPeriod.values()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    periods.forEachIndexed { index, p ->
                        SegmentedButton(
                            selected = period == p,
                            onClick  = { period = p },
                            shape    = SegmentedButtonDefaults.itemShape(index, periods.size)
                        ) {
                            Text(
                                when (p) {
                                    BudgetPeriod.ONE_TIME -> "One-time"
                                    BudgetPeriod.WEEKLY   -> "Weekly"
                                    BudgetPeriod.MONTHLY  -> "Monthly"
                                    BudgetPeriod.YEARLY   -> "Yearly"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // ── Category ──────────────────────────────────────────────────
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    expenseCategories.sortedBy { it.name.lowercase() }.forEach { cat ->
                        key(cat.id) {
                            val selected = selectedCat == cat.name
                            Column(
                                modifier            = Modifier.width(72.dp).clickable { selectedCat = cat.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) cat.color else cat.color.copy(alpha = 0.15f))
                                        .then(if (selected) Modifier.border(2.dp, cat.color, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        iconById(cat.iconId), contentDescription = null,
                                        tint     = if (selected) Color.White else cat.color.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    cat.name,
                                    style     = MaterialTheme.typography.labelSmall,
                                    color     = if (selected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines  = 2,
                                    overflow  = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = { onSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    if (isEditing) "Update Budget" else "Save Budget",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
