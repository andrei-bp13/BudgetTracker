package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.ui.components.*
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onPaymentTypeClick: (PaymentType) -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    editMode: Boolean = false
) {
    val thisMonth      by viewModel.thisMonthDailyExpenses.collectAsState()
    val lastMonth      by viewModel.lastMonthDailyExpenses.collectAsState()
    val recent         by viewModel.recentTransactions.collectAsState()
    val topCats        by viewModel.topCategories.collectAsState()
    val expense             by viewModel.thisMonthExpense.collectAsState()
    val income              by viewModel.thisMonthIncome.collectAsState()
    val globalColoring      by viewModel.globalColoring.collectAsState()
    val byPaymentType       by viewModel.thisMonthByPaymentType.collectAsState()
    val byPaymentTypeIncome by viewModel.thisMonthIncomeByPaymentType.collectAsState()

    val accent = MaterialTheme.colorScheme.primary
    val resolveColor: (String) -> androidx.compose.ui.graphics.Color = remember(globalColoring, accent) {
        { name -> if (globalColoring) accent else viewModel.categoryFor(name)?.color ?: accent }
    }

    val currencySymbol = LocalCurrencySymbol.current
    val monthName = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    // ── Overlay state (lifted to this level for global dismiss & precise positioning) ─────
    var overlayTransaction by remember { mutableStateOf<Transaction?>(null) }
    var overlayBounds      by remember { mutableStateOf(Rect.Zero) }
    var homeBoxCoords      by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // ── Root box: content + scrim + overlay (Z order: content → scrim → overlay) ──────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { homeBoxCoords = it }
    ) {
        // ── Card order & visibility ───────────────────────────────────────────
        val cardOrder   = remember { mutableStateListOf("month_summary", "monthly_progress", "recent_transactions", "top_categories") }
        val cardVisible = remember { mutableStateMapOf(
            "month_summary"        to true,
            "monthly_progress"     to true,
            "recent_transactions"  to true,
            "top_categories"       to true
        ) }
        fun swapCards(a: String, b: String) {
            val i = cardOrder.indexOf(a); val j = cardOrder.indexOf(b)
            if (i >= 0 && j >= 0) { cardOrder[i] = b; cardOrder[j] = a }
        }
        val visibleCards = cardOrder.filter { cardVisible[it] == true }
        val hiddenCards  = cardOrder.filter { cardVisible[it] != true }

        // ── Drag-to-reorder state ─────────────────────────────────────────────
        var draggedCardId by remember { mutableStateOf<String?>(null) }
        var dragOffsetY   by remember { mutableStateOf(0f) }
        val cardHeights   = remember { mutableStateMapOf<String, Int>() }

        val handleDrag: (Float) -> Unit = handleDrag@{ delta ->
            dragOffsetY += delta
            val dragged = draggedCardId ?: return@handleDrag
            val currentVisible = cardOrder.filter { cardVisible[it] == true }
            val idx = currentVisible.indexOf(dragged)
            if (dragOffsetY > 0f && idx < currentVisible.lastIndex) {
                val nextId    = currentVisible[idx + 1]
                val threshold = (cardHeights[nextId] ?: 0) / 2f
                if (dragOffsetY > threshold) {
                    swapCards(dragged, nextId)
                    dragOffsetY -= (cardHeights[nextId] ?: 0).toFloat()
                }
            } else if (dragOffsetY < 0f && idx > 0) {
                val prevId    = currentVisible[idx - 1]
                val threshold = (cardHeights[prevId] ?: 0) / 2f
                if (-dragOffsetY > threshold) {
                    swapCards(dragged, prevId)
                    dragOffsetY += (cardHeights[prevId] ?: 0).toFloat()
                }
            }
        }

        // ── Card render dispatch ──────────────────────────────────────────────
        val renderCard: @Composable (String) -> Unit = { cardId ->
            when (cardId) {
                "month_summary" -> Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape    = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text("Total spent this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol ${"%.2f".format(expense)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total income this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol ${"%.2f".format(income)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                            PaymentType.values().forEach { pt ->
                                val spent  = byPaymentType[pt] ?: 0.0
                                val earned = byPaymentTypeIncome[pt] ?: 0.0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPaymentTypeClick(pt) }.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    PaymentTypeIcon(type = pt, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("$currencySymbol ${"%.0f".format(spent)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                                        Text("/", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${"%.0f".format(earned)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF4CAF50))
                                    }
                                    Text(pt.displayName(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                "monthly_progress" -> Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape    = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Monthly Progress — $monthName", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        MonthlyProgressChart(thisMonth = thisMonth, lastMonth = lastMonth, modifier = Modifier.fillMaxWidth())
                    }
                }
                "recent_transactions" -> RecentSpendingCard(
                    transactions = recent,
                    resolveColor = resolveColor,
                    categoryFor  = { viewModel.categoryFor(it) },
                    onLongPress  = if (editMode) { _, _ -> } else { txn, bounds -> overlayTransaction = txn; overlayBounds = bounds },
                    modifier     = Modifier.fillMaxWidth()
                )
                "top_categories" -> TopCategoriesCard(
                    categories      = topCats,
                    resolveColor    = resolveColor,
                    categoryFor     = { viewModel.categoryFor(it) },
                    onCategoryClick = onCategoryClick,
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        }

        // ── 1. Scrollable content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (editMode) {
                Text(
                    "Hold and drag to reorder",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            visibleCards.forEach { cardId ->
                key(cardId) {
                    HomeEditableCardWrapper(
                        editMode        = editMode,
                        visible         = true,
                        isDragged       = draggedCardId == cardId,
                        dragOffsetY     = if (draggedCardId == cardId) dragOffsetY else 0f,
                        onDragStart     = { draggedCardId = cardId; dragOffsetY = 0f },
                        onDrag          = handleDrag,
                        onDragEnd       = { draggedCardId = null; dragOffsetY = 0f },
                        onToggleVisible = { cardVisible[cardId] = false },
                        onHeightChanged = { cardHeights[cardId] = it }
                    ) { renderCard(cardId) }
                }
            }

            if (editMode && hiddenCards.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text("Hidden", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                hiddenCards.forEach { cardId ->
                    key(cardId) {
                        HomeEditableCardWrapper(
                            editMode        = true,
                            visible         = false,
                            isDragged       = false,
                            dragOffsetY     = 0f,
                            onDragStart     = {},
                            onDrag          = {},
                            onDragEnd       = {},
                            onToggleVisible = { cardVisible[cardId] = true },
                            onHeightChanged = {}
                        ) { renderCard(cardId) }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // ── 2. Overlay (scrim + edit/delete buttons) positioned over the long-pressed row ──
        val activeTxn = overlayTransaction
        val coords    = homeBoxCoords
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

// ── Editable card wrapper (home screen) ──────────────────────────────────────

@Composable
private fun HomeEditableCardWrapper(
    editMode: Boolean,
    visible: Boolean,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggleVisible: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag      by rememberUpdatedState(onDrag)
    val latestOnDragEnd   by rememberUpdatedState(onDragEnd)

    val scale = remember { Animatable(1f) }
    LaunchedEffect(isDragged) {
        if (isDragged) scale.animateTo(1.04f, spring(dampingRatio = 0.4f, stiffness = 500f))
        else           scale.animateTo(1f,    spring(dampingRatio = 0.6f, stiffness = 500f))
    }

    val dragMod = if (editMode && visible) {
        Modifier.pointerInput(editMode) {
            detectDragGesturesAfterLongPress(
                onDragStart  = { latestOnDragStart() },
                onDrag       = { change, delta -> change.consume(); latestOnDrag(delta.y) },
                onDragEnd    = { latestOnDragEnd() },
                onDragCancel = { latestOnDragEnd() }
            )
        }
    } else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .zIndex(if (isDragged) 1f else 0f)
            .onGloballyPositioned { onHeightChanged(it.size.height) }
            .then(dragMod)
            .offset { IntOffset(0, if (isDragged) dragOffsetY.roundToInt() else 0) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = if (editMode && !visible) Modifier.fillMaxWidth().alpha(0.35f) else Modifier.fillMaxWidth()) {
                content()
            }
            if (editMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(editMode) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val up = waitForUpOrCancellation()
                                up?.consume()
                            }
                        }
                )
            }
        }
        if (editMode) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(end = 28.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                        .clickable(onClick = onToggleVisible)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (visible) "Hide card" else "Show card",
                        tint               = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(13.dp)
                    )
                    Text(
                        text  = if (visible) "visible" else "hidden",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
