package com.pardonmyspending.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol

private val Green   = Color(0xFF4CAF50)
private val DarkRed = Color(0xFFB71C1C)

@Composable
fun BudgetOverviewCard(
    budgets: List<Budget>,
    budgetSpent: Map<String, Double>,
    categoryFor: (String) -> CategoryItem?,
    modifier: Modifier = Modifier,
    globalColoring: Boolean = false
) {
    val currencySymbol = LocalCurrencySymbol.current
    val accent         = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Budgets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            budgets.forEachIndexed { i, budget ->
                val cat        = categoryFor(budget.categoryName)
                val catColor   = if (globalColoring) accent else (cat?.color ?: accent)
                val iconId     = cat?.iconId ?: "savings"
                val spent      = budgetSpent[budget.id] ?: 0.0
                val exceeded   = spent >= budget.amount
                val progress   = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                val spentColor = if (exceeded) DarkRed else Green
                val barColor   = if (exceeded) DarkRed else catColor

                BudgetOverviewRow(
                    budget         = budget,
                    spent          = spent,
                    progress       = progress,
                    spentColor     = spentColor,
                    barColor       = barColor,
                    catColor       = catColor,
                    iconId         = iconId,
                    currencySymbol = currencySymbol
                )
                if (i < budgets.lastIndex) {
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

@Composable
private fun BudgetOverviewRow(
    budget: Budget,
    spent: Double,
    progress: Float,
    spentColor: Color,
    barColor: Color,
    catColor: Color,
    iconId: String,
    currencySymbol: String
) {
    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label         = "budgetProgress_${budget.id}"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconById(iconId),
                        contentDescription = null,
                        tint     = catColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    budget.title,
                    style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "$currencySymbol ${"%.0f".format(spent)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = spentColor
                )
                Text(
                    " / ${"%.0f".format(budget.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(MaterialTheme.shapes.small)
                .background(barColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(barColor)
            )
        }
    }
}
