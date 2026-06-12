package com.budgettracker.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onEdit: (Transaction) -> Unit = {},
    onDelete: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Track which item has its overlay open, and whether it's in delete-confirm state
    var activeId      by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun dismiss() {
        activeId      = null
        confirmDelete = false
    }

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
                    val color   = resolveColor(t.category)
                    val cat     = categoryFor(t.category)
                    val isActive = activeId == t.id

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // ── Row content ───────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick    = { if (isActive) dismiss() },
                                    onLongClick = {
                                        activeId      = t.id
                                        confirmDelete = false
                                    }
                                )
                        ) {
                            RecentRow(t, cat, color)
                        }

                        // ── Action overlay ────────────────────────────────────
                        OverlayVisibility(visible = isActive) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.88f))
                                    // tapping the overlay background dismisses
                                    .combinedClickable(onClick = { dismiss() }),
                                contentAlignment = Alignment.Center
                            ) {
                                if (confirmDelete) {
                                    // ── Confirm / cancel ──────────────────────
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OverlayIconButton(
                                            icon    = Icons.Outlined.Check,
                                            label   = "Confirm",
                                            tint    = Color.White
                                        ) {
                                            onDelete(t)
                                            dismiss()
                                        }
                                        OverlayIconButton(
                                            icon    = Icons.Outlined.Close,
                                            label   = "Cancel",
                                            tint    = Color.White.copy(alpha = 0.7f)
                                        ) {
                                            confirmDelete = false
                                        }
                                    }
                                } else {
                                    // ── Edit / delete ─────────────────────────
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OverlayIconButton(
                                            icon  = Icons.Outlined.Edit,
                                            label = "Edit",
                                            tint  = Color.White
                                        ) {
                                            onEdit(t)
                                            dismiss()
                                        }
                                        OverlayIconButton(
                                            icon  = Icons.Outlined.Delete,
                                            label = "Delete",
                                            tint  = Color.White
                                        ) {
                                            confirmDelete = true
                                        }
                                    }
                                }
                            }
                        }
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

// Standalone wrapper so AnimatedVisibility resolves without a ColumnScope receiver
@Composable
private fun OverlayVisibility(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + scaleIn(initialScale = 0.95f),
        exit    = fadeOut() + scaleOut(targetScale = 0.95f)
    ) { content() }
}

@Composable
private fun OverlayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(24.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
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
