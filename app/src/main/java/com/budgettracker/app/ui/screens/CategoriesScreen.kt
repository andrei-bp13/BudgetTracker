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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Restore
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

    var editTarget       by remember { mutableStateOf<CategoryItem?>(null) }
    var showNew          by remember { mutableStateOf(false) }
    var showEdit         by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

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
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(bottom = 8.dp)
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
                    CategoryRow(
                        category = cat,
                        onClick  = { editTarget = cat; showEdit = true },
                        onDelete = if (!cat.isDefault) ({ viewModel.deleteCategory(cat) }) else null
                    )
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
                        CategoryRow(
                            category = cat,
                            onClick  = { editTarget = cat; showEdit = true },
                            onDelete = if (!cat.isDefault) ({ viewModel.deleteCategory(cat) }) else null
                        )
                    }
                }
            }

            // ── Sticky "Restore Defaults" button ──────────────────────────────
            OutlinedButton(
                onClick  = { showResetConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(52.dp),
                shape  = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore Defaults", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    // ── Restore confirmation dialog ───────────────────────────────────────────
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon  = { Icon(Icons.Outlined.Restore, contentDescription = null) },
            title = { Text("Restore Default Categories?") },
            text  = {
                Text(
                    "This will remove all custom categories and restore any deleted default " +
                    "categories back to their original state. Your existing transactions will " +
                    "keep their category names, but those categories won't appear in the list " +
                    "unless you re-create them."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetCategories(); showResetConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
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
private fun CategoryRow(
    category: CategoryItem,
    onClick:  () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape  = MaterialTheme.shapes.large
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
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

            if (onDelete != null) {
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete category",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Edit",
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
