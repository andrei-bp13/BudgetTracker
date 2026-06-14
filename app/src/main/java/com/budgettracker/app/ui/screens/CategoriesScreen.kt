package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    var editTarget  by remember { mutableStateOf<CategoryItem?>(null) }
    var showNew     by remember { mutableStateOf(false) }
    var showEdit    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon    = { Icon(Icons.Outlined.Add, contentDescription = "Add") },
                text    = { Text("New Category") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "Default (${categories.count { it.isDefault }})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(categories.filter { it.isDefault }) { cat ->
                CategoryRow(cat, onClick = { editTarget = cat; showEdit = true })
            }

            val custom = categories.filter { !it.isDefault }
            if (custom.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Custom (${custom.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(custom) { cat ->
                    CategoryRow(cat, onClick = { editTarget = cat; showEdit = true })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showNew) {
        EditCategorySheet(
            category    = null,
            accentColor = accentColor,
            onSave      = { viewModel.saveCategory(it); showNew = false },
            onDelete    = null,
            onDismiss   = { showNew = false }
        )
    }

    if (showEdit && editTarget != null) {
        EditCategorySheet(
            category    = editTarget,
            accentColor = accentColor,
            onSave      = { viewModel.saveCategory(it); showEdit = false },
            onDelete    = { viewModel.deleteCategory(it); showEdit = false },
            onDismiss   = { showEdit = false }
        )
    }
}

@Composable
private fun CategoryRow(category: CategoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconById(category.iconId),
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                if (!category.isDefault) {
                    Text("Custom", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Icon(Icons.Outlined.ChevronRight, contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
