package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.ui.icons.AppIcon
import com.pardonmyspending.app.ui.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    currentIconId: String,
    accentColor: Color,
    onSelect: (AppIcon) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) AppIcons
        else AppIcons.filter { it.label.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier         = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Choose Icon",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search icons…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(360.dp)
            ) {
                items(filtered) { icon ->
                    val selected = icon.id == currentIconId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(icon) }
                            .background(
                                if (selected) accentColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .then(
                                if (selected) Modifier.border(1.5.dp, accentColor, MaterialTheme.shapes.medium)
                                else Modifier
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon.vector,
                            contentDescription = icon.label,
                            tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            icon.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
