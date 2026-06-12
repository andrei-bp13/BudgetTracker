package com.budgettracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import com.budgettracker.app.ui.components.PaymentTypeIcon
import com.budgettracker.app.ui.components.displayName
import com.budgettracker.app.ui.icons.iconById
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol  = LocalCurrencySymbol.current
    val accent          = MaterialTheme.colorScheme.primary
    val today           = LocalDate.now()

    var selectedDate       by remember { mutableStateOf(today) }
    var displayedMonth     by remember { mutableStateOf(YearMonth.now()) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    val transactionsByDate = remember(allTransactions) {
        allTransactions.groupBy { it.date }
    }
    val daysWithTransactions = remember(transactionsByDate, displayedMonth) {
        transactionsByDate.keys
            .filter { YearMonth.from(it) == displayedMonth }
            .map { it.dayOfMonth }
            .toSet()
    }
    val selectedDayTransactions = remember(transactionsByDate, selectedDate) {
        (transactionsByDate[selectedDate] ?: emptyList())
            .sortedByDescending { it.amount }
    }

    val headerFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Calendar card ─────────────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape    = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {

                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                        }
                        TextButton(onClick = { showYearMonthPicker = true }) {
                            Text(
                                "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Day-of-week headers (Mon–Sun)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { d ->
                            Text(
                                d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Day grid
                    val firstDay    = displayedMonth.atDay(1)
                    val offset      = (firstDay.dayOfWeek.value - 1) // Mon=0 … Sun=6
                    val daysInMonth = displayedMonth.lengthOfMonth()
                    val rows        = ((offset + daysInMonth) + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(rows) { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                repeat(7) { col ->
                                    val cellIndex = row * 7 + col
                                    val dayNum    = cellIndex - offset + 1
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNum in 1..daysInMonth) {
                                            val date       = displayedMonth.atDay(dayNum)
                                            val isSelected = date == selectedDate
                                            val isToday    = date == today
                                            val hasDot     = dayNum in daysWithTransactions
                                            DayCell(
                                                day         = dayNum,
                                                isSelected  = isSelected,
                                                isToday     = isToday,
                                                hasDot      = hasDot,
                                                accentColor = accent,
                                                onClick     = { selectedDate = date }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Selected-day header ───────────────────────────────────────────────
        item {
            Text(
                selectedDate.format(headerFmt),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Transactions for selected day ─────────────────────────────────────
        if (selectedDayTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions on this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(selectedDayTransactions, key = { it.id }) { txn ->
                CalendarTransactionRow(
                    txn            = txn,
                    currencySymbol = currencySymbol,
                    categoryColor  = viewModel.categoryFor(txn.category)?.color
                        ?: MaterialTheme.colorScheme.primary,
                    categoryIconId = viewModel.categoryFor(txn.category)?.iconId ?: "more_horiz"
                )
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // ── Year / month picker dialog ────────────────────────────────────────────
    if (showYearMonthPicker) {
        YearMonthPickerDialog(
            current  = displayedMonth,
            onDismiss = { showYearMonthPicker = false },
            onSelect  = { ym ->
                displayedMonth = ym
                // if selected date is outside the new month, reset to 1st
                if (YearMonth.from(selectedDate) != ym) selectedDate = ym.atDay(1)
                showYearMonthPicker = false
            }
        )
    }
}

@Composable
private fun YearMonthPickerDialog(
    current: YearMonth,
    onDismiss: () -> Unit,
    onSelect: (YearMonth) -> Unit
) {
    var pickerYear  by remember { mutableStateOf(current.year) }
    var pickerMonth by remember { mutableStateOf(current.month) }
    val accent = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSelect(YearMonth.of(pickerYear, pickerMonth)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Select month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Year selector ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickerYear-- }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous year")
                    }
                    Text(
                        "$pickerYear",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { pickerYear++ }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next year")
                    }
                }

                // ── Month grid (4 × 3) ────────────────────────────────────────
                val months = Month.values()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    months.toList().chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { month ->
                                val isSelected = month == pickerMonth
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (isSelected) accent
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .clickable { pickerMonth = month }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) Color.White
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasDot: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSelected) accentColor else Color.Transparent)
                .then(
                    if (isToday && !isSelected)
                        Modifier.border(1.5.dp, accentColor, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelected -> Color.White
                    isToday    -> accentColor
                    else       -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        // Dot — always allocate the space so rows stay the same height
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasDot    -> Color.Transparent
                        isSelected -> Color.White
                        else       -> accentColor
                    }
                )
        )
    }
}

@Composable
private fun CalendarTransactionRow(
    txn: Transaction,
    currencySymbol: String,
    categoryColor: Color,
    categoryIconId: String
) {
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category icon circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconById(categoryIconId),
                contentDescription = null,
                tint     = categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title + meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    txn.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = secondaryColor)
                PaymentTypeIcon(type = txn.paymentType, tint = secondaryColor, size = 12.dp)
                Text(
                    txn.paymentType.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
            }
        }

        // Amount
        Text(
            "${if (txn.type == TransactionType.EXPENSE) "-" else "+"}$currencySymbol ${"%.2f".format(txn.amount)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (txn.type == TransactionType.EXPENSE)
                        MaterialTheme.colorScheme.error
                    else
                        categoryColor
        )
    }
}
