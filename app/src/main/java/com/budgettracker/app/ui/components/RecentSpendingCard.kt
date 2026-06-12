package com.budgettracker.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.CategoryItem
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.ui.icons.iconById
import com.budgettracker.app.ui.theme.LocalCurrencySymbol

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentSpendingCard(
    transactions: List<Transaction>,
    resolveColor: (String) -> Color,
    categoryFor: (String) -> CategoryItem?,
    onLongPress: (Transaction, Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Spending", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (transactions.isEmpty()) {
                Text(
                    "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                transactions.forEachIndexed { i, t ->
                    val color = resolveColor(t.category)
                    val cat   = categoryFor(t.category)

                    // Capture this row's bounds so the parent can position the overlay
                    var rowBounds by remember { mutableStateOf(Rect.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
                            .combinedClickable(
                                onClick    = { /* normal tap does nothing */ },
                                onLongClick = { onLongPress(t, rowBounds) }
                            )
                    ) {
                        RecentRow(t, cat, color)
                    }

                    if (i < transactions.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = 8.dp),
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRow(t: Transaction, cat: CategoryItem?, color: Color) {
    val iconId         = cat?.iconId ?: "more_horiz"
    val currencySymbol = LocalCurrencySymbol.current
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = iconById(iconId),
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                t.title,
                style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "-$currencySymbol ${"%.2f".format(t.amount)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = secondaryColor)
                PaymentTypeIcon(type = t.paymentType, tint = secondaryColor, size = 12.dp)
                Text(
                    t.paymentType.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor
                )
            }
        }
    }
}
