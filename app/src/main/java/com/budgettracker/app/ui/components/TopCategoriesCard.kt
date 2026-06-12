package com.budgettracker.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.CategoryItem
import com.budgettracker.app.ui.icons.iconById
import com.budgettracker.app.ui.theme.LocalCurrencySymbol

@Composable
fun TopCategoriesCard(
    categories: List<Pair<String, Double>>,
    resolveColor: (String) -> Color,
    categoryFor: (String) -> CategoryItem?,
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Top Categories", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (categories.isEmpty()) {
                Text("No data this month", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxAmount = categories.maxOf { it.second }.coerceAtLeast(0.01)
                categories.forEach { (name, amount) ->
                    CategoryBarRow(
                        name            = name,
                        amount          = amount,
                        maxAmount       = maxAmount,
                        color           = resolveColor(name),
                        iconId          = categoryFor(name)?.iconId ?: "more_horiz",
                        onCategoryClick = { onCategoryClick(name) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryBarRow(name: String, amount: Double, maxAmount: Double, color: Color, iconId: String, onCategoryClick: () -> Unit) {
    val fraction = (amount / maxAmount).toFloat().coerceIn(0.05f, 1f)
    val currencySymbol = LocalCurrencySymbol.current

    val animFraction by animateFloatAsState(
        targetValue   = fraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "barAnim"
    )

    Column(
        modifier = Modifier.clickable(onClick = onCategoryClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = iconById(iconId),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Text(
                "$currencySymbol ${"%.0f".format(amount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
