package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.CategoryItem
import com.pardonmyspending.app.data.model.CategoryType
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val categories  by viewModel.categories.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    var editTarget by remember { mutableStateOf<CategoryItem?>(null) }
    var showNew    by remember { mutableStateOf(false) }
    var showEdit   by remember { mutableStateOf(false) }

    val expenseCats = remember(categories) { categories.filter { it.type == CategoryType.EXPENSE } }
    val incomeCats  = remember(categories) { categories.filter { it.type == CategoryType.INCOME } }

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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Sticky "New Category" button ──────────────────────────────────
            Button(
                onClick  = { showNew = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Category", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            // ── Scrollable category list ──────────────────────────────────────
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        "Expense (${expenseCats.size})",
                        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(expenseCats) { cat ->
                    CategoryRow(cat, onClick = { editTarget = cat; showEdit = true })
                }

                if (incomeCats.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Income (${incomeCats.size})",
                            style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(incomeCats) { cat ->
                        CategoryRow(cat, onClick = { editTarget = cat; showEdit = true })
                    }
                }
            }
        }
    }

    if (showNew) {
        EditCategorySheet(
            category    = null,
            initialType = CategoryType.EXPENSE,
            accentColor = accentColor,
            onSave      = { viewModel.saveCategory(it); showNew = false },
            onDelete    = null,
            onDismiss   = { showNew = false }
        )
    }

    if (showEdit && editTarget != null) {
        EditCategorySheet(
            category    = editTarget,
            initialType = editTarget!!.type,
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
        shape  = MaterialTheme.shapes.large
    ) {
        Row(
            modifier             = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
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
                    imageVector        = iconById(category.iconId),
                    contentDescription = null,
                    tint               = category.color,
                    modifier           = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                if (!category.isDefault) {
                    Text(
                        "Custom · ${category.type.name.lowercase().replaceFirstChar { it.titlecase() }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(Icons.Outlined.ChevronRight, contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
