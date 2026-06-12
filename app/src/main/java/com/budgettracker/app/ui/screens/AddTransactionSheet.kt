package com.budgettracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.*
import com.budgettracker.app.ui.components.PaymentTypeIcon
import com.budgettracker.app.ui.components.displayName
import com.budgettracker.app.ui.icons.iconById
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    initialTransaction: Transaction? = null
) {
    val allCategories  by viewModel.categories.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current
    val isEditing      = initialTransaction != null

    var title       by remember { mutableStateOf(initialTransaction?.title ?: "") }
    var amount      by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var note        by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var paymentType by remember { mutableStateOf(initialTransaction?.paymentType ?: PaymentType.DEBIT) }
    var selectedCat by remember { mutableStateOf(initialTransaction?.category ?: "Food & Dining") }
    var sortByMostUsed by remember { mutableStateOf(false) }

    val incomeNames = setOf("Salary", "Freelance", "Investment", "Other Income")
    val baseCategories = allCategories.filter { cat -> cat.name !in incomeNames }

    val usageCount = remember(allTransactions) {
        allTransactions.groupingBy { it.category }.eachCount()
    }

    val categories = remember(baseCategories, sortByMostUsed, usageCount) {
        if (sortByMostUsed)
            baseCategories.sortedByDescending { usageCount[it.name] ?: 0 }
        else
            baseCategories.sortedBy { it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isEditing) "Edit Expense" else "Add Expense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // ── Compact text fields ───────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title", style = MaterialTheme.typography.labelSmall) },
                textStyle     = MaterialTheme.typography.bodyMedium,
                modifier      = Modifier.fillMaxWidth().height(52.dp),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value         = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("Amount", style = MaterialTheme.typography.labelSmall) },
                textStyle     = MaterialTheme.typography.bodyMedium,
                prefix        = { Text("$currencySymbol ", style = MaterialTheme.typography.bodyMedium) },
                modifier      = Modifier.fillMaxWidth().height(52.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            // ── Payment method ────────────────────────────────────────────────
            Text(
                "Payment Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PaymentType.values().forEach { pt ->
                    PaymentTypeButton(
                        type     = pt,
                        selected = paymentType == pt,
                        onClick  = { paymentType = pt }
                    )
                }
            }

            // ── Category ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { sortByMostUsed = !sortByMostUsed },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Rounded.SwapVert,
                        contentDescription = "Sort",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (sortByMostUsed) "Most Used" else "A–Z",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // 2-row horizontal scrolling grid
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val selected = selectedCat == cat.name
                    val scale by animateFloatAsState(
                        targetValue   = if (selected) 1.12f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "catScale"
                    )
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clickable { selectedCat = cat.name },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (selected) cat.color
                                    else cat.color.copy(alpha = 0.15f)
                                )
                                .then(
                                    if (selected) Modifier.border(2.dp, cat.color, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = iconById(cat.iconId),
                                contentDescription = null,
                                tint               = if (selected) Color.White else cat.color.copy(alpha = 0.8f),
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text      = cat.name,
                            style     = MaterialTheme.typography.labelSmall,
                            color     = if (selected) cat.color
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines  = 2,
                            overflow  = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Note ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Note (optional)", style = MaterialTheme.typography.labelSmall) },
                textStyle     = MaterialTheme.typography.bodyMedium,
                modifier      = Modifier.fillMaxWidth().height(52.dp),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (title.isBlank()) return@Button
                    if (isEditing && initialTransaction != null) {
                        viewModel.updateTransaction(
                            initialTransaction.copy(
                                title       = title.trim(),
                                amount      = amt,
                                category    = selectedCat,
                                note        = note.trim(),
                                paymentType = paymentType
                            )
                        )
                    } else {
                        viewModel.addTransaction(
                            Transaction(
                                id          = UUID.randomUUID().toString(),
                                title       = title.trim(),
                                amount      = amt,
                                type        = TransactionType.EXPENSE,
                                category    = selectedCat,
                                date        = LocalDate.now(),
                                note        = note.trim(),
                                paymentType = paymentType
                            )
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    if (isEditing) "Update Expense" else "Save Expense",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun PaymentTypeButton(type: PaymentType, selected: Boolean, onClick: () -> Unit) {
    val accent    = MaterialTheme.colorScheme.primary
    val onAccent  = MaterialTheme.colorScheme.onPrimary
    val surface   = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (selected) accent else surface)
                .then(
                    if (!selected) Modifier.border(1.5.dp, accent.copy(alpha = 0.3f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            PaymentTypeIcon(
                type = type,
                tint = if (selected) onAccent else onSurface,
                size = 20.dp
            )
        }
        Text(
            type.displayName(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) accent else onSurface
        )
    }
}
