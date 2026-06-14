package com.pardonmyspending.app.ui.screens

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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.data.model.*
import com.pardonmyspending.app.ui.components.PaymentTypeIcon
import com.pardonmyspending.app.ui.components.displayName
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringTransactionSheet(
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    initialRecurring: RecurringTransaction? = null
) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories  by viewModel.incomeCategories.collectAsState()
    val allTransactions   by viewModel.transactions.collectAsState()
    val currencySymbol    = LocalCurrencySymbol.current
    val isEditing         = initialRecurring != null

    var transactionType  by remember { mutableStateOf(initialRecurring?.type        ?: TransactionType.EXPENSE) }
    var title            by remember { mutableStateOf(initialRecurring?.title        ?: "") }
    var amount           by remember { mutableStateOf(initialRecurring?.amount?.toString() ?: "") }
    var note             by remember { mutableStateOf(initialRecurring?.note         ?: "") }
    var paymentType      by remember { mutableStateOf(initialRecurring?.paymentType  ?: PaymentType.DEBIT) }
    var selectedCat      by remember { mutableStateOf(initialRecurring?.category     ?: "Food & Dining") }
    var sortByMostUsed   by remember { mutableStateOf(false) }
    var startDate        by remember { mutableStateOf(initialRecurring?.startDate    ?: LocalDate.now()) }
    var frequency        by remember { mutableStateOf(initialRecurring?.frequency    ?: RecurringFrequency.MONTHLY) }
    var interval         by remember { mutableStateOf(initialRecurring?.interval     ?: 1) }
    var intervalUnit     by remember { mutableStateOf(initialRecurring?.intervalUnit ?: IntervalUnit.MONTHS) }
    var endCondition     by remember { mutableStateOf(initialRecurring?.endCondition ?: EndCondition.NEVER) }
    var endDate          by remember { mutableStateOf(initialRecurring?.endDate      ?: LocalDate.now().plusMonths(12)) }
    var endOccurrences   by remember { mutableStateOf(initialRecurring?.endAfterOccurrences?.toString() ?: "12") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showUnitMenu        by remember { mutableStateOf(false) }

    val baseCategories = if (transactionType == TransactionType.EXPENSE) expenseCategories else incomeCategories
    val usageCount     = remember(allTransactions) { allTransactions.groupingBy { it.category }.eachCount() }
    val categories     = remember(baseCategories, sortByMostUsed, usageCount) {
        if (sortByMostUsed) baseCategories.sortedByDescending { usageCount[it.name] ?: 0 }
        else                baseCategories.sortedBy { it.name.lowercase() }
    }

    val onSave: () -> Unit = save@{
        val titleVal  = title.trim()
        val amountVal = amount.toDoubleOrNull() ?: return@save
        if (titleVal.isBlank()) return@save

        val endOccurrencesVal = endOccurrences.toIntOrNull()?.takeIf { it > 0 }

        if (isEditing && initialRecurring != null) {
            viewModel.updateRecurring(initialRecurring.copy(
                title               = titleVal,
                amount              = amountVal,
                type                = transactionType,
                category            = selectedCat,
                paymentType         = paymentType,
                note                = note.trim(),
                frequency           = frequency,
                interval            = interval,
                intervalUnit        = intervalUnit,
                endCondition        = endCondition,
                endDate             = if (endCondition == EndCondition.DATE) endDate else null,
                endAfterOccurrences = if (endCondition == EndCondition.OCCURRENCES) endOccurrencesVal else null
            ))
        } else {
            viewModel.addRecurring(RecurringTransaction(
                id                  = UUID.randomUUID().toString(),
                title               = titleVal,
                amount              = amountVal,
                type                = transactionType,
                category            = selectedCat,
                paymentType         = paymentType,
                note                = note.trim(),
                frequency           = frequency,
                interval            = interval,
                intervalUnit        = intervalUnit,
                startDate           = startDate,
                nextDueDate         = startDate,
                endCondition        = endCondition,
                endDate             = if (endCondition == EndCondition.DATE) endDate else null,
                endAfterOccurrences = if (endCondition == EndCondition.OCCURRENCES) endOccurrencesVal else null
            ))
        }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val titleStyle  = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 15.sp)
                    val activeColor = MaterialTheme.colorScheme.onSurface
                    val dimColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Recurring Expense", style = titleStyle,
                            color = if (transactionType == TransactionType.EXPENSE) activeColor else dimColor,
                            modifier = Modifier.clickable(enabled = transactionType != TransactionType.EXPENSE) {
                                transactionType = TransactionType.EXPENSE
                                selectedCat = expenseCategories.firstOrNull()?.name ?: selectedCat
                            })
                        Box(
                            modifier = Modifier.clickable {
                                transactionType = if (transactionType == TransactionType.EXPENSE) TransactionType.INCOME else TransactionType.EXPENSE
                                selectedCat = (if (transactionType == TransactionType.INCOME) incomeCategories else expenseCategories).firstOrNull()?.name ?: selectedCat
                            }.padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = "Switch type", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Recurring Income", style = titleStyle,
                            color = if (transactionType == TransactionType.INCOME) activeColor else dimColor,
                            modifier = Modifier.clickable(enabled = transactionType != TransactionType.INCOME) {
                                transactionType = TransactionType.INCOME
                                selectedCat = incomeCategories.firstOrNull()?.name ?: selectedCat
                            })
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // ── Title ─────────────────────────────────────────────────────
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") },
                    textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium)

                // ── Amount ────────────────────────────────────────────────────
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount") }, textStyle = MaterialTheme.typography.bodyMedium,
                    prefix = { Text("$currencySymbol ", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, shape = MaterialTheme.shapes.medium)

                // ── Note ──────────────────────────────────────────────────────
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") },
                    textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, shape = MaterialTheme.shapes.medium)

                // ── Payment type ──────────────────────────────────────────────
                val paymentTypes = PaymentType.values()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    paymentTypes.forEachIndexed { index, pt ->
                        SegmentedButton(
                            selected = paymentType == pt,
                            onClick  = { paymentType = pt },
                            shape    = SegmentedButtonDefaults.itemShape(index, paymentTypes.size),
                            icon     = { PaymentTypeIcon(type = pt, tint = LocalContentColor.current, size = 16.dp) }
                        ) { Text(pt.displayName(), style = MaterialTheme.typography.labelMedium) }
                    }
                }

                // ── Start date ────────────────────────────────────────────────
                Text("Schedule", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startDate.format(dateFmt), onValueChange = {}, readOnly = true,
                        label = { Text("Starts on") },
                        trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp)) },
                        textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = MaterialTheme.shapes.medium
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showStartDatePicker = true })
                }

                // ── Frequency ─────────────────────────────────────────────────
                Text("Repeats", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val frequencies = RecurringFrequency.values()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    frequencies.forEachIndexed { index, freq ->
                        SegmentedButton(
                            selected = frequency == freq,
                            onClick  = { frequency = freq },
                            shape    = SegmentedButtonDefaults.itemShape(index, frequencies.size),
                            icon     = {}
                        ) {
                            Text(
                                when (freq) {
                                    RecurringFrequency.DAILY   -> "Daily"
                                    RecurringFrequency.WEEKLY  -> "Weekly"
                                    RecurringFrequency.MONTHLY -> "Monthly"
                                    RecurringFrequency.YEARLY  -> "Yearly"
                                    RecurringFrequency.CUSTOM  -> "Custom"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // ── Custom interval ───────────────────────────────────────────
                if (frequency == RecurringFrequency.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Every", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                        IconButton(onClick = { if (interval > 1) interval-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Less", modifier = Modifier.size(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$interval", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        IconButton(onClick = { interval++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }

                        ExposedDropdownMenuBox(
                            expanded = showUnitMenu,
                            onExpandedChange = { showUnitMenu = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = when (intervalUnit) {
                                    IntervalUnit.DAYS   -> if (interval == 1) "day"   else "days"
                                    IntervalUnit.WEEKS  -> if (interval == 1) "week"  else "weeks"
                                    IntervalUnit.MONTHS -> if (interval == 1) "month" else "months"
                                    IntervalUnit.YEARS  -> if (interval == 1) "year"  else "years"
                                },
                                onValueChange = {},
                                readOnly  = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showUnitMenu) },
                                modifier  = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            ExposedDropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                                IntervalUnit.values().forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                        onClick = { intervalUnit = unit; showUnitMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── End condition ─────────────────────────────────────────────
                Text("Ends", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val endOptions = EndCondition.values()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    endOptions.forEachIndexed { index, cond ->
                        SegmentedButton(
                            selected = endCondition == cond,
                            onClick  = { endCondition = cond },
                            shape    = SegmentedButtonDefaults.itemShape(index, endOptions.size)
                        ) {
                            Text(
                                when (cond) {
                                    EndCondition.NEVER       -> "Never"
                                    EndCondition.DATE        -> "On date"
                                    EndCondition.OCCURRENCES -> "Occurrences"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (endCondition == EndCondition.DATE) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = endDate.format(dateFmt), onValueChange = {}, readOnly = true,
                            label = { Text("End date") },
                            trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp)) },
                            textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, shape = MaterialTheme.shapes.medium
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showEndDatePicker = true })
                    }
                }

                if (endCondition == EndCondition.OCCURRENCES) {
                    OutlinedTextField(
                        value = endOccurrences, onValueChange = { endOccurrences = it.filter { c -> c.isDigit() } },
                        label = { Text("Number of occurrences") },
                        textStyle = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, shape = MaterialTheme.shapes.medium
                    )
                }

                // ── Category ──────────────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { sortByMostUsed = !sortByMostUsed }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = "Sort", modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (sortByMostUsed) "Most Used" else "A–Z", style = MaterialTheme.typography.labelSmall)
                    }
                }

                val gridRows   = if (transactionType == TransactionType.INCOME) 1 else 4
                val gridHeight = if (gridRows == 1) 76.dp else 304.dp
                val itemW = 72f; val gap = 8f

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cols      = ((maxWidth.value + gap) / (itemW + gap)).toInt().coerceAtLeast(1)
                    val gridWidth = (itemW * cols + gap * (cols - 1)).dp

                    LazyHorizontalGrid(
                        rows                  = GridCells.Fixed(gridRows),
                        horizontalArrangement = Arrangement.spacedBy(gap.dp),
                        verticalArrangement   = Arrangement.spacedBy(gap.dp),
                        modifier              = Modifier.width(gridWidth).height(gridHeight)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            val selected = selectedCat == cat.name
                            Column(
                                modifier            = Modifier.width(72.dp).clickable { selectedCat = cat.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape)
                                        .background(if (selected) cat.color else cat.color.copy(alpha = 0.15f))
                                        .then(if (selected) Modifier.border(2.dp, cat.color, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconById(cat.iconId), contentDescription = null,
                                        tint = if (selected) Color.White else cat.color.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp))
                                }
                                Text(cat.name, style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

            } // scrollable Column

            // ── Save button ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                val typeLabel = if (transactionType == TransactionType.EXPENSE) "Expense" else "Income"
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                    Text(if (isEditing) "Update Recurring $typeLabel" else "Save Recurring $typeLabel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // ── Start date picker ─────────────────────────────────────────────────
        if (showStartDatePicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = startDate.toEpochDay() * 86_400_000L)
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { startDate = LocalDate.ofEpochDay(it / 86_400_000L) }
                        showStartDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
            ) { DatePicker(state = state) }
        }

        // ── End date picker ───────────────────────────────────────────────────
        if (showEndDatePicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = endDate.toEpochDay() * 86_400_000L)
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { endDate = LocalDate.ofEpochDay(it / 86_400_000L) }
                        showEndDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
            ) { DatePicker(state = state) }
        }
    }
}
