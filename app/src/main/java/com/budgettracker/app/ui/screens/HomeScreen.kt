package com.budgettracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.PaymentType
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.ui.components.*
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.TransactionViewModel
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
    onDeleteTransaction: (Transaction) -> Unit = {}
) {
    val thisMonth      by viewModel.thisMonthDailyExpenses.collectAsState()
    val lastMonth      by viewModel.lastMonthDailyExpenses.collectAsState()
    val recent         by viewModel.recentTransactions.collectAsState()
    val topCats        by viewModel.topCategories.collectAsState()
    val expense        by viewModel.thisMonthExpense.collectAsState()
    val globalColoring by viewModel.globalColoring.collectAsState()
    val byPaymentType  by viewModel.thisMonthByPaymentType.collectAsState()

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
        // ── 1. Scrollable content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Month summary card ────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape  = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        "Total spent this month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$currencySymbol ${"%.2f".format(expense)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Payment type breakdown ────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaymentType.values().forEach { pt ->
                            val amount = byPaymentType[pt] ?: 0.0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPaymentTypeClick(pt) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                PaymentTypeIcon(type = pt, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                Text(
                                    "$currencySymbol ${"%.0f".format(amount)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    pt.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Monthly progress chart ────────────────────────────────────────
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape    = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Monthly Progress — $monthName", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    MonthlyProgressChart(
                        thisMonth = thisMonth,
                        lastMonth = lastMonth,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Recent Transactions ───────────────────────────────────────────
            RecentSpendingCard(
                transactions = recent,
                resolveColor = resolveColor,
                categoryFor  = { viewModel.categoryFor(it) },
                onLongPress  = { txn, bounds ->
                    overlayTransaction = txn
                    overlayBounds      = bounds
                },
                modifier     = Modifier.fillMaxWidth()
            )

            // ── Top Categories ────────────────────────────────────────────────
            TopCategoriesCard(
                categories      = topCats,
                resolveColor    = resolveColor,
                categoryFor     = { viewModel.categoryFor(it) },
                onCategoryClick = onCategoryClick,
                modifier        = Modifier.fillMaxWidth()
            )

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
