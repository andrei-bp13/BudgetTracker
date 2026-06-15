package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.components.EditDeleteOverlay
import com.pardonmyspending.app.util.filterByTypeAndDateRange
import com.pardonmyspending.app.ui.components.PaymentTypeIcon
import com.pardonmyspending.app.ui.components.displayName
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DrillDownScreen(
    title: String,
    startDate: LocalDate,
    endDate: LocalDate,
    transactionType: TransactionType = TransactionType.EXPENSE,
    categoryFilter: String? = null,
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {}
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol  = LocalCurrencySymbol.current

    val filtered = remember(allTransactions, startDate, endDate, transactionType, categoryFilter) {
        filterByTypeAndDateRange(allTransactions, startDate, endDate, transactionType)
            .let { if (categoryFilter != null) it.filter { t -> t.category == categoryFilter } else it }
    }

    val totalAmount = remember(filtered) { filtered.sumOf { it.amount } }

    val byCategory = remember(filtered) {
        filtered
            .groupBy { it.category }
            .map { (cat, txns) -> cat to txns.sortedByDescending { it.date } }
            .sortedByDescending { (_, txns) -> txns.sumOf { it.amount } }
    }

    val expandedCategories = remember(startDate, endDate) { mutableStateMapOf<String, Boolean>() }
    val dateFmt     = DateTimeFormatter.ofPattern("MMM d")
    val dateFmtFull = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    val fallbackColor = MaterialTheme.colorScheme.primary

    val byDate = remember(filtered, categoryFilter) {
        if (categoryFilter == null) emptyList()
        else filtered
            .groupBy { it.date }
            .map { (date, txns) -> date to txns.sortedByDescending { it.amount } }
            .sortedByDescending { (date, _) -> date }
    }

    // Overlay state
    var overlayTransaction by remember { mutableStateOf<Transaction?>(null) }
    var overlayBounds      by remember { mutableStateOf(Rect.Zero) }
    var rootCoords         by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Root Box captures screen coordinates so the overlay can be positioned absolutely
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoords = it }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
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
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // â"€â"€ Summary card â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
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
                                Text(
                                    if (transactionType == TransactionType.INCOME) "Total income" else "Total expenses",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$currencySymbol ${"%.2f".format(totalAmount)}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (transactionType == TransactionType.INCOME) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${filtered.size}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (categoryFilter != null) {
                    // Date-grouped flat list when a specific category is selected
                    byDate.forEach { (date, txns) ->
                        item(key = "date_$date") {
                            Text(
                                date.format(dateFmtFull),
                                style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                        items(txns, key = { "flat_${it.id}" }) { txn ->
                            val catItem   = viewModel.categoryFor(txn.category)
                            val catColor  = catItem?.color ?: fallbackColor
                            val catIconId = catItem?.iconId ?: "more_horiz"
                            DrillFlatRow(
                                txn            = txn,
                                catColor       = catColor,
                                catIconId      = catIconId,
                                isIncome       = transactionType == TransactionType.INCOME,
                                currencySymbol = currencySymbol,
                                onLongPress    = { bounds ->
                                    overlayTransaction = txn
                                    overlayBounds      = bounds
                                }
                            )
                            HorizontalDivider(
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                } else {
                    byCategory.forEach { (cat, txns) ->
                        val catTotal   = txns.sumOf { it.amount }
                        val isExpanded = expandedCategories[cat] == true
                        val catItem    = viewModel.categoryFor(cat)
                        val catColor   = catItem?.color ?: fallbackColor
                        val catIconId  = catItem?.iconId ?: "more_horiz"

                        item(key = "header_$cat") {
                            CategoryDrillHeader(
                                name           = cat,
                                total          = catTotal,
                                count          = txns.size,
                                color          = catColor,
                                iconId         = catIconId,
                                isExpanded     = isExpanded,
                                currencySymbol = currencySymbol,
                                onClick        = { expandedCategories[cat] = !isExpanded }
                            )
                        }

                        if (isExpanded) {
                            items(txns, key = { it.id }) { txn ->
                                DrillTransactionRow(
                                    txn            = txn,
                                    color          = catColor,
                                    isIncome       = transactionType == TransactionType.INCOME,
                                    currencySymbol = currencySymbol,
                                    dateFmt        = dateFmt,
                                    onLongPress    = { bounds ->
                                        overlayTransaction = txn
                                        overlayBounds      = bounds
                                    }
                                )
                                HorizontalDivider(
                                    modifier  = Modifier.padding(start = 52.dp),
                                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        item(key = "div_$cat") {
                            HorizontalDivider(
                                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Overlay (scrim + edit/delete buttons) positioned over the long-pressed row
        val activeTxn = overlayTransaction
        val coords    = rootCoords
        if (activeTxn != null && coords != null) {
            EditDeleteOverlay(
                transaction = activeTxn,
                bounds      = overlayBounds,
                rootCoords  = coords,
                onEdit      = { t -> onEditTransaction(t) },
                onDelete    = { t -> onDeleteTransaction(t) },
                onDismiss   = { overlayTransaction = null }
            )
        }
    }
}

@Composable
private fun CategoryDrillHeader(
    name: String,
    total: Double,
    count: Int,
    color: Color,
    iconId: String,
    isExpanded: Boolean,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconById(iconId),
                contentDescription = null,
                tint     = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "$count ${if (count == 1) "transaction" else "transactions"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "$currencySymbol ${"%.2f".format(total)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )

        Icon(
            imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp
                          else Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrillTransactionRow(
    txn: Transaction,
    color: Color,
    isIncome: Boolean,
    currencySymbol: String,
    dateFmt: DateTimeFormatter,
    onLongPress: (Rect) -> Unit
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    var rowBounds by remember { mutableStateOf(Rect.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .combinedClickable(onClick = {}, onLongClick = { onLongPress(rowBounds) })
            .padding(start = 52.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    txn.date.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = secondary)
                PaymentTypeIcon(type = txn.paymentType, tint = secondary, size = 11.dp)
                Text(
                    txn.paymentType.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary
                )
            }
        }

        Text(
            "${if (isIncome) "+" else "-"}$currencySymbol ${"%.2f".format(txn.amount)}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrillFlatRow(
    txn: Transaction,
    catColor: Color,
    catIconId: String,
    isIncome: Boolean,
    currencySymbol: String,
    onLongPress: (Rect) -> Unit
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    var rowBounds by remember { mutableStateOf(Rect.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .combinedClickable(onClick = {}, onLongClick = { onLongPress(rowBounds) })
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconById(catIconId),
                contentDescription = null,
                tint     = catColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                txn.title,
                style   = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentTypeIcon(type = txn.paymentType, tint = secondary, size = 12.dp)
                Text(
                    txn.paymentType.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary
                )
            }
        }

        Text(
            "${if (isIncome) "+" else "-"}$currencySymbol ${"%.2f".format(txn.amount)}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )
    }
}
