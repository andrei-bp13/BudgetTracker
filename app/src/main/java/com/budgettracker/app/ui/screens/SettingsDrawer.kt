package com.budgettracker.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.AllCurrencies
import com.budgettracker.app.ui.theme.AccentColors
import com.budgettracker.app.viewmodel.TransactionViewModel
import java.time.LocalDate

@Composable
fun SettingsDrawer(
    viewModel: TransactionViewModel,
    onClose: () -> Unit,
    onNavigateToCategories: () -> Unit = {}
) {
    val themeMode        by viewModel.themeMode.collectAsState()
    val accentIndex      by viewModel.accentIndex.collectAsState()
    val globalColoring   by viewModel.globalColoring.collectAsState()
    val granularColoring by viewModel.granularColoring.collectAsState()
    val currencyCode     by viewModel.currencyCode.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // ── Theme section ─────────────────────────────────────────────────────
        SectionLabel("Appearance")

        Text(
            "Theme",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (mode, label) ->
                ThemeChip(
                    label    = label,
                    selected = themeMode == mode,
                    onClick  = { viewModel.setThemeMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Accent Color",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 10 color swatches in a 5×2 grid
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0..4, 5..9).forEach { range ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    range.forEach { i ->
                        val accent = AccentColors[i]
                        ColorSwatch(
                            color    = accent.seed,
                            name     = accent.name,
                            selected = accentIndex == i,
                            onClick  = { viewModel.setAccentIndex(i) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Coloring mode checkboxes
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ColoringCheckbox(
                label       = "Use global theming",
                description = "All categories use the accent color",
                checked     = globalColoring,
                onChecked   = { viewModel.setGlobalColoring(it) }
            )
            ColoringCheckbox(
                label       = "Use granular coloring",
                description = "Each category uses its own color",
                checked     = granularColoring,
                onChecked   = { viewModel.setGranularColoring(it) }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // ── Currency section ──────────────────────────────────────────────────
        SectionLabel("Currency")

        val selectedCurrency = remember(currencyCode) {
            AllCurrencies.firstOrNull { it.code == currencyCode }
        }
        NavigationDrawerItem(
            label = {
                Column {
                    Text("Currency")
                    Text(
                        "${selectedCurrency?.code ?: "USD"} — ${selectedCurrency?.name ?: "US Dollar"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            badge = {
                Text(
                    selectedCurrency?.symbol ?: "$",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            icon  = { Icon(Icons.Rounded.AttachMoney, contentDescription = null) },
            selected = false,
            onClick = { showCurrencyPicker = true },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // ── Categories section ────────────────────────────────────────────────
        SectionLabel("Categories")

        NavigationDrawerItem(
            label = { Text("Manage Categories") },
            icon  = { Icon(Icons.Rounded.Category, contentDescription = null) },
            selected = false,
            onClick = { onNavigateToCategories() },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // ── Data section ──────────────────────────────────────────────────────
        SectionLabel("Data")

        NavigationDrawerItem(
            label = { Text("Export CSV") },
            icon  = { Icon(Icons.Rounded.Upload, contentDescription = null) },
            selected = false,
            onClick = {
                val fname = "budget_${LocalDate.now()}.csv"
                exportLauncher.launch(fname)
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            label = { Text("Import CSV") },
            icon  = { Icon(Icons.Rounded.Download, contentDescription = null) },
            selected = false,
            onClick = { importLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.weight(1f))

        Text(
            "BudgetTracker v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            selectedCode = currencyCode,
            onSelect     = { viewModel.setCurrencyCode(it.code) },
            onDismiss    = { showCurrencyPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().wrapContentWidth(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ColoringCheckbox(
    label: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked
        )
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
