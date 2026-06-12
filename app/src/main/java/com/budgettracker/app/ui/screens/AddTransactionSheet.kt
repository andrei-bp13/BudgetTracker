package com.budgettracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onDismiss: () -> Unit
) {
    val allCategories  by viewModel.categories.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current

    var title       by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var note        by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentType.DEBIT) }
    var selectedCat by remember { mutableStateOf("Food & Dining") }

    val incomeNames = setOf("Salary", "Freelance", "Investment", "Other Income")
    val categories  = allCategories.filter { cat -> cat.name !in incomeNames }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add Expense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                prefix = { Text("$currencySymbol ") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
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

            // ── Category matrix ───────────────────────────────────────────────
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            val selected = selectedCat == cat.name
                            val scale by animateFloatAsState(
                                targetValue   = if (selected) 1.15f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness    = Spring.StiffnessMedium
                                ),
                                label = "catScale"
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCat = cat.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
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
                                        imageVector = iconById(cat.iconId),
                                        contentDescription = null,
                                        tint = if (selected) Color.White else cat.color.copy(alpha = 0.8f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    cat.name.take(9),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) cat.color
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (title.isBlank()) return@Button
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
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    "Save Expense",
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
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
                size = 28.dp
            )
        }
        Text(
            type.displayName(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) accent else onSurface
        )
    }
}
