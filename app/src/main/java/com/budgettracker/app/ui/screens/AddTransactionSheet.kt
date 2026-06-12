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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val allCategories   by viewModel.categories.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol  = LocalCurrencySymbol.current
    val isEditing       = initialTransaction != null

    var title          by remember { mutableStateOf(initialTransaction?.title    ?: "") }
    var amount         by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var note           by remember { mutableStateOf(initialTransaction?.note     ?: "") }
    var paymentType    by remember { mutableStateOf(initialTransaction?.paymentType ?: PaymentType.DEBIT) }
    var selectedCat    by remember { mutableStateOf(initialTransaction?.category ?: "Food & Dining") }
    var sortByMostUsed by remember { mutableStateOf(false) }

    val incomeNames    = setOf("Salary", "Freelance", "Investment", "Other Income")
    val baseCategories = allCategories.filter { it.name !in incomeNames }

    val usageCount = remember(allTransactions) {
        allTransactions.groupingBy { it.category }.eachCount()
    }

    val categories = remember(baseCategories, sortByMostUsed, usageCount) {
        if (sortByMostUsed) baseCategories.sortedByDescending { usageCount[it.name] ?: 0 }
        else                baseCategories.sortedBy { it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    if (isEditing) "Edit Expense" else "Add Expense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // ── Title ────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title", style = MaterialTheme.typography.labelSmall) },
                textStyle     = MaterialTheme.typography.bodyMedium,
                modifier      = Modifier.fillMaxWidth().height(52.dp),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            // ── Amount ───────────────────────────────────────────────────────
            OutlinedTextField(
                value           = amount,
                onValueChange   = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label           = { Text("Amount", style = MaterialTheme.typography.labelSmall) },
                textStyle       = MaterialTheme.typography.bodyMedium,
                prefix          = { Text("$currencySymbol ", style = MaterialTheme.typography.bodyMedium) },
                modifier        = Modifier.fillMaxWidth().height(52.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine      = true,
                shape           = MaterialTheme.shapes.medium
            )

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

            // ── Payment type pill ─────────────────────────────────────────────
            val paymentTypes = PaymentType.values()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                paymentTypes.forEachIndexed { index, pt ->
                    SegmentedButton(
                        selected = paymentType == pt,
                        onClick  = { paymentType = pt },
                        shape    = SegmentedButtonDefaults.itemShape(index, paymentTypes.size),
                        icon     = {
                            PaymentTypeIcon(
                                type = pt,
                                tint = LocalContentColor.current,
                                size = 16.dp
                            )
                        }
                    ) {
                        Text(
                            pt.displayName(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // ── Category header ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick        = { sortByMostUsed = !sortByMostUsed },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Rounded.SwapVert, contentDescription = "Sort", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sortByMostUsed) "Most Used" else "A–Z", style = MaterialTheme.typography.labelSmall)
                }
            }

            // ── Category grid (2 rows, horizontally scrollable + scrollbar) ───
            val gridState = rememberLazyGridState()

            // Compute scrollbar thumb position and size from visible item indices
            val thumbStartFraction by remember {
                derivedStateOf {
                    val info       = gridState.layoutInfo
                    val totalItems = info.totalItemsCount
                    if (totalItems == 0) return@derivedStateOf 0f
                    val firstCol   = (info.visibleItemsInfo.firstOrNull()?.index ?: 0) / 2
                    val totalCols  = (totalItems + 1) / 2
                    val visCols    = ((info.visibleItemsInfo.lastOrNull()?.index ?: 0) / 2) - firstCol + 1
                    val maxScroll  = (totalCols - visCols).coerceAtLeast(1)
                    (firstCol.toFloat() / maxScroll) * (1f - visCols.toFloat() / totalCols)
                }
            }
            val thumbWidthFraction by remember {
                derivedStateOf {
                    val info       = gridState.layoutInfo
                    val totalItems = info.totalItemsCount
                    if (totalItems == 0) return@derivedStateOf 1f
                    val totalCols  = (totalItems + 1) / 2
                    val firstCol   = (info.visibleItemsInfo.firstOrNull()?.index ?: 0) / 2
                    val visCols    = ((info.visibleItemsInfo.lastOrNull()?.index ?: 0) / 2) - firstCol + 1
                    (visCols.toFloat() / totalCols).coerceIn(0f, 1f)
                }
            }

            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

            // Grid sized to an exact multiple of (item + gap) so no partial items appear
            val itemW = 80f  // dp
            val gap   = 8f   // dp
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols      = ((maxWidth.value + gap) / (itemW + gap)).toInt().coerceAtLeast(2)
                val gridWidth = (itemW * cols + gap * (cols - 1)).dp
                LazyHorizontalGrid(
                    rows                  = GridCells.Fixed(2),
                    state                 = gridState,
                    horizontalArrangement = Arrangement.spacedBy(gap.dp),
                    verticalArrangement   = Arrangement.spacedBy(gap.dp),
                    modifier              = Modifier.width(gridWidth).height(152.dp)
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
                            modifier            = Modifier.width(80.dp).clickable { selectedCat = cat.name },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(if (selected) cat.color else cat.color.copy(alpha = 0.15f))
                                    .then(if (selected) Modifier.border(2.dp, cat.color, CircleShape) else Modifier),
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
                                color     = if (selected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines  = 2,
                                overflow  = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } // BoxWithConstraints

            // Scrollbar — only shown when content overflows
            if (thumbWidthFraction < 1f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val trackW  = size.width
                    val trackH  = size.height
                    val radius  = CornerRadius(trackH / 2)
                    // Track
                    drawRoundRect(color = trackColor, cornerRadius = radius)
                    // Thumb
                    val thumbW = trackW * thumbWidthFraction
                    val thumbX = trackW * thumbStartFraction
                    drawRoundRect(
                        color        = thumbColor,
                        topLeft      = Offset(thumbX, 0f),
                        size         = Size(thumbW, trackH),
                        cornerRadius = radius
                    )
                }
            }

            // ── Save button ───────────────────────────────────────────────────
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = MaterialTheme.shapes.large
            ) {
                Text(
                    if (isEditing) "Update Expense" else "Save Expense",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
