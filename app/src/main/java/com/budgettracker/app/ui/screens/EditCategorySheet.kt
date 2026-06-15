package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.CategoryColorOptions
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.data.model.CategoryType
import com.pardonmyspending.app.data.model.colorToHex
import com.pardonmyspending.app.ui.components.RainbowColorSwatch
import com.pardonmyspending.app.ui.components.RgbColorPickerDialog
import com.pardonmyspending.app.ui.icons.AppIcons
import com.pardonmyspending.app.ui.icons.iconById

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategorySheet(
    category: CategoryItem?,    // null = new category
    initialType: CategoryType = CategoryType.EXPENSE,
    accentColor: Color,
    onSave: (CategoryItem) -> Unit,
    onDelete: ((CategoryItem) -> Unit)?,
    onDismiss: () -> Unit
) {
    var name          by remember { mutableStateOf(category?.name     ?: "") }
    var iconId        by remember { mutableStateOf(category?.iconId   ?: "more_horiz") }
    var colorHex      by remember { mutableStateOf(category?.colorHex ?: "#4361EE") }
    var catType       by remember { mutableStateOf(category?.type     ?: initialType) }
    var showIconPicker  by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val selectedColor = runCatching {
        Color(android.graphics.Color.parseColor(colorHex))
    }.getOrElse { Color.Gray }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier         = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (category == null) "New Category" else "Edit Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }

            // Preview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(selectedColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconById(iconId),
                        contentDescription = null,
                        tint = selectedColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    name.ifBlank { "Category name" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Type selector -" locked for default categories
            Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val typeEnabled = category == null || !category.isDefault
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(CategoryType.EXPENSE to "Expense", CategoryType.INCOME to "Income")
                    .forEachIndexed { idx, (type, label) ->
                        SegmentedButton(
                            selected = catType == type,
                            onClick  = { if (typeEnabled) catType = type },
                            shape    = SegmentedButtonDefaults.itemShape(idx, 2),
                            enabled  = typeEnabled
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
            }

            // Icon picker trigger
            Text("Icon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIconPicker = true },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(iconById(iconId), contentDescription = null, tint = selectedColor, modifier = Modifier.size(24.dp))
                    Text(
                        AppIcons.firstOrNull { it.id == iconId }?.label ?: iconId,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Change", style = MaterialTheme.typography.labelMedium, color = accentColor)
                }
            }

            // Color grid (19 preset swatches + 1 custom RGB picker)
            Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.height(80.dp)
            ) {
                items(CategoryColorOptions) { color ->
                    val hex = colorToHex(color)
                    val isSelected = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { colorHex = hex }
                            .then(
                                if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                    )
                }
                item {
                    val isCustomSelected = CategoryColorOptions.none { colorToHex(it).equals(colorHex, ignoreCase = true) }
                    RainbowColorSwatch(
                        selected = isCustomSelected,
                        onClick  = { showColorPicker = true },
                        size     = 28.dp
                    )
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val id = category?.id ?: "custom_${System.currentTimeMillis()}"
                    onSave(CategoryItem(id, name.trim(), iconId, colorHex, isDefault = category?.isDefault ?: false, type = catType))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Save Category", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            if (onDelete != null && category != null && !category.isDefault) {
                OutlinedButton(
                    onClick = { onDelete(category) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Category", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerSheet(
            currentIconId = iconId,
            accentColor   = accentColor,
            onSelect      = { icon -> iconId = icon.id; showIconPicker = false },
            onDismiss     = { showIconPicker = false }
        )
    }

    if (showColorPicker) {
        RgbColorPickerDialog(
            initialHex = colorHex,
            onConfirm  = { hex -> colorHex = hex; showColorPicker = false },
            onDismiss  = { showColorPicker = false }
        )
    }
}
