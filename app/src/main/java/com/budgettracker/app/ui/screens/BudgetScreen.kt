package com.pardonmyspending.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val Green   = Color(0xFF4CAF50)
private val DarkRed = Color(0xFFB71C1C)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: TransactionViewModel, editMode: Boolean = false) {
    val budgets      by viewModel.budgets.collectAsState()
    val budgetSpent  by viewModel.budgetSpent.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current

    var showAddSheet      by remember { mutableStateOf(false) }
    var editTarget        by remember { mutableStateOf<Budget?>(null) }
    var showPastBudgets   by remember { mutableStateOf(false) }

    // Local ordered list for drag-to-reorder; syncs from Room when not dragging
    val localList = remember { mutableStateListOf<Budget>() }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(budgets) {
        if (!isDragging) {
            localList.clear()
            localList.addAll(budgets)
        }
    }

    // Drag state
    var draggedId   by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val cardHeights = remember { mutableStateMapOf<String, Int>() }

    fun swap(a: String, b: String) {
        val i = localList.indexOfFirst { it.id == a }
        val j = localList.indexOfFirst { it.id == b }
        if (i >= 0 && j >= 0) { val tmp = localList[i]; localList[i] = localList[j]; localList[j] = tmp }
    }

    val handleDrag: (Float) -> Unit = { delta ->
        dragOffsetY += delta
        val dragged = draggedId
        if (dragged != null) {
            val idx = localList.indexOfFirst { it.id == dragged }
            if (dragOffsetY > 0f && idx < localList.lastIndex) {
                val nextId    = localList[idx + 1].id
                val threshold = (cardHeights[nextId] ?: 0) / 2f
                if (dragOffsetY > threshold) { swap(dragged, nextId); dragOffsetY -= (cardHeights[nextId] ?: 0).toFloat() }
            } else if (dragOffsetY < 0f && idx > 0) {
                val prevId    = localList[idx - 1].id
                val threshold = (cardHeights[prevId] ?: 0) / 2f
                if (-dragOffsetY > threshold) { swap(dragged, prevId); dragOffsetY += (cardHeights[prevId] ?: 0).toFloat() }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Sticky "Add Budget" button ────────────────────────────────────────
        Button(
            onClick  = { showAddSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Budget", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }

        if (localList.isEmpty()) {
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    Text("No budgets yet", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("Tap \"Add Budget\" to get started", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                }
            }
        } else {
            // ── Draggable budget cards ────────────────────────────────────────
            LazyColumn(
                modifier            = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding      = PaddingValues(bottom = 8.dp)
            ) {
                if (editMode && localList.size > 1) {
                    item {
                        Text(
                            "Hold and drag to reorder",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                        )
                    }
                }
                items(localList, key = { it.id }) { budget ->
                    val cat     = viewModel.categoryFor(budget.categoryName)
                    val spent   = budgetSpent[budget.id] ?: 0.0
                    val isDragged = draggedId == budget.id

                    BudgetCardWrapper(
                        editMode        = editMode,
                        isDragged       = isDragged,
                        dragOffsetY     = if (isDragged) dragOffsetY else 0f,
                        onDragStart     = { draggedId = budget.id; dragOffsetY = 0f; isDragging = true },
                        onDrag          = handleDrag,
                        onDragEnd       = {
                            draggedId   = null
                            dragOffsetY = 0f
                            isDragging  = false
                            viewModel.updateBudgetOrder(localList.toList())
                        },
                        onHeightChanged = { cardHeights[budget.id] = it }
                    ) {
                        BudgetCard(
                            budget         = budget,
                            spent          = spent,
                            editMode       = editMode,
                            categoryColor  = cat?.color ?: MaterialTheme.colorScheme.primary,
                            categoryIconId = cat?.iconId ?: "savings",
                            currencySymbol = currencySymbol,
                            onClick        = { if (!editMode) editTarget = budget },
                            onDelete       = { viewModel.deleteBudget(budget.id) }
                        )
                    }
                }
            }
        }

        // ── Sticky "Show Past Budgets" button ─────────────────────────────────
        OutlinedButton(
            onClick  = { showPastBudgets = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Show Past Budgets", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }

    if (showAddSheet) {
        AddBudgetSheet(viewModel = viewModel, initial = null, onDismiss = { showAddSheet = false })
    }
    if (editTarget != null) {
        AddBudgetSheet(viewModel = viewModel, initial = editTarget, onDismiss = { editTarget = null })
    }
    if (showPastBudgets) {
        PastBudgetsSheet(
            budgets      = budgets,
            transactions = transactions,
            currencySymbol = currencySymbol,
            categoryFor  = { viewModel.categoryFor(it) },
            onDismiss    = { showPastBudgets = false }
        )
    }
}

// ── Drag wrapper ──────────────────────────────────────────────────────────────

@Composable
private fun BudgetCardWrapper(
    editMode: Boolean,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val latestStart by rememberUpdatedState(onDragStart)
    val latestDrag  by rememberUpdatedState(onDrag)
    val latestEnd   by rememberUpdatedState(onDragEnd)

    val scale = remember { Animatable(1f) }
    LaunchedEffect(isDragged) {
        if (isDragged) scale.animateTo(1.03f, spring(dampingRatio = 0.4f, stiffness = 500f))
        else           scale.animateTo(1f,    spring(dampingRatio = 0.6f, stiffness = 500f))
    }

    val dragMod = if (editMode) {
        Modifier.pointerInput(editMode) {
            detectDragGesturesAfterLongPress(
                onDragStart  = { latestStart() },
                onDrag       = { change, delta -> change.consume(); latestDrag(delta.y) },
                onDragEnd    = { latestEnd() },
                onDragCancel = { latestEnd() }
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .zIndex(if (isDragged) 1f else 0f)
            .onGloballyPositioned { onHeightChanged(it.size.height) }
            .offset { IntOffset(0, if (isDragged) dragOffsetY.roundToInt() else 0) }
            .then(dragMod)
    ) {
        content()
    }
}

// ── Budget card ───────────────────────────────────────────────────────────────

@Composable
private fun BudgetCard(
    budget: Budget,
    spent: Double,
    editMode: Boolean,
    categoryColor: Color,
    categoryIconId: String,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val exceeded  = spent >= budget.amount
    val progress  = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val spentColor = if (exceeded) DarkRed else Green
    val barColor   = if (exceeded) DarkRed else categoryColor

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // Title row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (editMode) {
                        Icon(
                            Icons.Outlined.DragHandle,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        budget.title,
                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Period chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            when (budget.period) {
                                BudgetPeriod.ONE_TIME -> "One-time"
                                BudgetPeriod.WEEKLY   -> "Weekly"
                                BudgetPeriod.MONTHLY  -> "Monthly"
                                BudgetPeriod.YEARLY   -> "Yearly"
                            },
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                        tint = DarkRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category icon + amount row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconById(categoryIconId), contentDescription = null,
                        tint = categoryColor, modifier = Modifier.size(20.dp))
                }

                // spent / budget
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "$currencySymbol ${"%.0f".format(spent)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = spentColor
                    )
                    Text(
                        " / $currencySymbol ${"%.0f".format(budget.amount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = if (exceeded) DarkRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress         = { progress },
                modifier         = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                color            = barColor,
                trackColor       = barColor.copy(alpha = 0.15f),
                strokeCap        = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete Budget?") },
            text             = { Text("\"${budget.title}\" will be permanently removed. Past transactions are not affected.") },
            confirmButton    = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Past Budgets sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastBudgetsSheet(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    currencySymbol: String,
    categoryFor: (String) -> com.pardonmyspending.app.data.model.CategoryItem?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier         = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Past Budgets",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            val periodicBudgets = budgets.filter { it.period != BudgetPeriod.ONE_TIME }

            if (periodicBudgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No past periods available.\nAdd a weekly, monthly, or yearly budget first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                Column(
                    modifier            = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    periodicBudgets.forEach { budget ->
                        val cat = categoryFor(budget.categoryName)
                        val catColor = cat?.color ?: Color.Gray
                        val iconId   = cat?.iconId ?: "savings"
                        val pastPeriods = buildPastPeriods(budget, transactions)

                        if (pastPeriods.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(budget.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                pastPeriods.forEach { (label, spent) ->
                                    PastPeriodRow(
                                        label          = label,
                                        spent          = spent,
                                        budgetAmount   = budget.amount,
                                        categoryColor  = catColor,
                                        categoryIconId = iconId,
                                        currencySymbol = currencySymbol
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun PastPeriodRow(
    label: String,
    spent: Double,
    budgetAmount: Double,
    categoryColor: Color,
    categoryIconId: String,
    currencySymbol: String
) {
    val exceeded   = spent >= budgetAmount
    val progress   = if (budgetAmount > 0) (spent / budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val spentColor = if (exceeded) DarkRed else Green
    val barColor   = if (exceeded) DarkRed else categoryColor

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier         = Modifier.size(28.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconById(categoryIconId), contentDescription = null,
                            tint = categoryColor, modifier = Modifier.size(16.dp))
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$currencySymbol ${"%.0f".format(spent)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = spentColor)
                    Text(" / $currencySymbol ${"%.0f".format(budgetAmount)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small),
                color      = barColor,
                trackColor = barColor.copy(alpha = 0.15f),
                strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

// ── Past period computation ───────────────────────────────────────────────────

private fun buildPastPeriods(budget: Budget, transactions: List<Transaction>): List<Pair<String, Double>> {
    val today       = LocalDate.now()
    val createdDate = LocalDate.ofEpochDay(budget.createdEpochDay)
    val result      = mutableListOf<Pair<String, Double>>()

    when (budget.period) {
        BudgetPeriod.ONE_TIME -> Unit // no past periods

        BudgetPeriod.WEEKLY -> {
            val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var weekStart = currentWeekStart.minusWeeks(1)
            while (!weekStart.isBefore(createdDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) && result.size < 12) {
                val weekEnd = weekStart.plusDays(6)
                val spent   = transactions.filter {
                    it.type == TransactionType.EXPENSE && it.category == budget.categoryName &&
                    !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd)
                }.sumOf { it.amount }
                val label = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                result += label to spent
                weekStart = weekStart.minusWeeks(1)
            }
        }

        BudgetPeriod.MONTHLY -> {
            var ym = YearMonth.now().minusMonths(1)
            val createdYM = YearMonth.from(createdDate)
            while (!ym.isBefore(createdYM) && result.size < 24) {
                val spent = transactions.filter {
                    it.type == TransactionType.EXPENSE && it.category == budget.categoryName &&
                    YearMonth.from(it.date) == ym
                }.sumOf { it.amount }
                result += ym.format(monthFmt) to spent
                ym = ym.minusMonths(1)
            }
        }

        BudgetPeriod.YEARLY -> {
            var year = today.year - 1
            while (year >= createdDate.year && result.size < 10) {
                val spent = transactions.filter {
                    it.type == TransactionType.EXPENSE && it.category == budget.categoryName &&
                    it.date.year == year
                }.sumOf { it.amount }
                result += "$year" to spent
                year--
            }
        }
    }

    return result
}
