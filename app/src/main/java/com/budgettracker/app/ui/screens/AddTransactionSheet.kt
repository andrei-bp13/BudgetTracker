package com.pardonmyspending.app.ui.screens

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.data.model.*
import com.pardonmyspending.app.ui.components.PaymentTypeIcon
import com.pardonmyspending.app.ui.components.displayName
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    initialTransaction: Transaction? = null,
    isAiPrefill: Boolean = false
) {
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories  by viewModel.incomeCategories.collectAsState()
    val allTransactions   by viewModel.transactions.collectAsState()
    val currencySymbol    = LocalCurrencySymbol.current
    val isEditing         = initialTransaction != null && !isAiPrefill

    var transactionType by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var title          by remember { mutableStateOf(initialTransaction?.title    ?: "") }
    var amount         by remember { mutableStateOf(initialTransaction?.amount?.toString() ?: "") }
    var note           by remember { mutableStateOf(initialTransaction?.note     ?: "") }
    var paymentType    by remember { mutableStateOf(initialTransaction?.paymentType ?: PaymentType.DEBIT) }
    var selectedCat    by remember { mutableStateOf(initialTransaction?.category ?: "Food & Dining") }
    var sortByMostUsed by remember { mutableStateOf(false) }
    var selectedDate   by remember { mutableStateOf(initialTransaction?.date ?: LocalDate.now()) }
    var selectedTime   by remember {
        mutableStateOf(initialTransaction?.time ?: LocalTime.now().withSecond(0).withNano(0))
    }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var showNewCatSheet  by remember { mutableStateOf(false) }

    val baseCategories = if (transactionType == TransactionType.EXPENSE) expenseCategories else incomeCategories

    val usageCount = remember(allTransactions) {
        allTransactions.groupingBy { it.category }.eachCount()
    }

    val categories = remember(baseCategories, sortByMostUsed, usageCount) {
        if (sortByMostUsed) baseCategories.sortedByDescending { usageCount[it.name] ?: 0 }
        else                baseCategories.sortedBy { it.name.lowercase() }
    }

    val onSave: () -> Unit = save@{
        val titleVal  = title.trim()
        val amountVal = amount.toDoubleOrNull() ?: return@save
        if (titleVal.isBlank()) return@save
        if (initialTransaction != null) {
            viewModel.updateTransaction(
                initialTransaction.copy(
                    title       = titleVal,
                    amount      = amountVal,
                    category    = selectedCat,
                    note        = note.trim(),
                    paymentType = paymentType,
                    date        = selectedDate,
                    time        = selectedTime
                )
            )
        } else {
            viewModel.addTransaction(
                Transaction(
                    id          = UUID.randomUUID().toString(),
                    title       = titleVal,
                    amount      = amountVal,
                    type        = transactionType,
                    category    = selectedCat,
                    date        = selectedDate,
                    note        = note.trim(),
                    paymentType = paymentType,
                    time        = selectedTime
                )
            )
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        Column(modifier = Modifier.heightIn(max = screenHeight * 0.85f)) {

            // â"€â"€ Scrollable form â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // â"€â"€ Header â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val verb       = if (isEditing) "Edit" else "Add"
                    val titleStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize   = 18.sp
                    )
                    val activeColor = MaterialTheme.colorScheme.onSurface
                    val dimColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                    Row(
                        modifier              = Modifier.weight(1f),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "$verb Expense",
                            style    = titleStyle,
                            color    = if (transactionType == TransactionType.EXPENSE) activeColor else dimColor,
                            modifier = Modifier.clickable(enabled = transactionType != TransactionType.EXPENSE) {
                                transactionType = TransactionType.EXPENSE
                                selectedCat = expenseCategories.firstOrNull()?.name ?: selectedCat
                            }
                        )
                        Box(
                            modifier         = Modifier
                                .clickable {
                                    val newType = if (transactionType == TransactionType.EXPENSE)
                                        TransactionType.INCOME else TransactionType.EXPENSE
                                    transactionType = newType
                                    selectedCat = (if (newType == TransactionType.INCOME) incomeCategories else expenseCategories)
                                        .firstOrNull()?.name ?: selectedCat
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.SwapHoriz,
                                contentDescription = "Switch type",
                                modifier = Modifier.size(16.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "$verb Income",
                            style    = titleStyle,
                            color    = if (transactionType == TransactionType.INCOME) activeColor else dimColor,
                            modifier = Modifier.clickable(enabled = transactionType != TransactionType.INCOME) {
                                transactionType = TransactionType.INCOME
                                selectedCat = incomeCategories.firstOrNull()?.name ?: selectedCat
                            }
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // â"€â"€ Title â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Title") },
                    textStyle     = MaterialTheme.typography.bodyMedium,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.medium
                )

                // â"€â"€ Amount â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                OutlinedTextField(
                    value           = amount,
                    onValueChange   = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label           = { Text("Amount") },
                    textStyle       = MaterialTheme.typography.bodyMedium,
                    prefix          = { Text("$currencySymbol ", style = MaterialTheme.typography.bodyMedium) },
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    shape           = MaterialTheme.shapes.medium
                )

                // â"€â"€ Note â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    textStyle     = MaterialTheme.typography.bodyMedium,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = MaterialTheme.shapes.medium
                )

                // â"€â"€ Payment type â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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
                            Text(pt.displayName(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // â"€â"€ Date + Time â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value         = selectedDate.format(dateFormatter),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Date") },
                            trailingIcon  = { Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp)) },
                            textStyle     = MaterialTheme.typography.bodyMedium,
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            shape         = MaterialTheme.shapes.medium
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value         = selectedTime.format(timeFormatter),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Time") },
                            trailingIcon  = { Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(18.dp)) },
                            textStyle     = MaterialTheme.typography.bodyMedium,
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            shape         = MaterialTheme.shapes.medium
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                    }
                }

                // â"€â"€ Category header â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick        = { sortByMostUsed = !sortByMostUsed },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.SwapVert, contentDescription = "Sort", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (sortByMostUsed) "Most Used" else "A-Z", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick        = { showNewCatSheet = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add category", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // â"€â"€ Category grid -" centered FlowRow, scrolls with the outer column â"€â"€
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        key(cat.id) {
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
                                modifier            = Modifier.width(72.dp).clickable { selectedCat = cat.name },
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
                }

            } // scrollable Column

            // â"€â"€ Fixed save button â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                val typeLabel = if (transactionType == TransactionType.EXPENSE) "Expense" else "Income"
                Button(
                    onClick  = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = MaterialTheme.shapes.large
                ) {
                    Text(
                        if (isEditing) "Update $typeLabel" else "Save $typeLabel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

        } // outer Column

        // â"€â"€ Date picker â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton    = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // â"€â"€ Time picker â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour   = selectedTime.hour,
                initialMinute = selectedTime.minute,
                is24Hour      = true
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title            = { Text("Select time") },
                text             = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }

        // â"€â"€ Add category shortcut â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        if (showNewCatSheet) {
            val newCatType = if (transactionType == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME
            EditCategorySheet(
                category    = null,
                initialType = newCatType,
                accentColor = MaterialTheme.colorScheme.primary,
                onSave      = { item ->
                    viewModel.saveCategory(item)
                    selectedCat = item.name
                    showNewCatSheet = false
                },
                onDelete    = null,
                onDismiss   = { showNewCatSheet = false }
            )
        }

    } // ModalBottomSheet
}
