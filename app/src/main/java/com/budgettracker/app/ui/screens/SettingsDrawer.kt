package com.pardonmyspending.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.pardonmyspending.app.viewmodel.AccountViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pardonmyspending.app.ui.components.RainbowColorSwatch
import com.pardonmyspending.app.ui.components.RgbColorPickerDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.data.model.AllCurrencies
import com.pardonmyspending.app.ui.theme.AccentColors
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate

@Composable
fun SettingsDrawer(
    viewModel: TransactionViewModel,
    accountViewModel: AccountViewModel,
    onClose: () -> Unit,
    onNavigateToCategories: () -> Unit = {}
) {
    val themeMode        by viewModel.themeMode.collectAsState()
    val accentIndex      by viewModel.accentIndex.collectAsState()
    val globalColoring   by viewModel.globalColoring.collectAsState()
    val granularColoring by viewModel.granularColoring.collectAsState()
    val currencyCode     by viewModel.currencyCode.collectAsState()
    val aiMode           by viewModel.aiMode.collectAsState()
    val customAccentHex  by viewModel.customAccentHex.collectAsState()
    val aiApiKey         by viewModel.aiApiKey.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showColorPicker    by remember { mutableStateOf(false) }
    var showApiKey         by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportCsv(it) } }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importCsv(it) } }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = accountViewModel.buildLocalBackupJson()
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)
                ?.use { input -> input.bufferedReader().readText() }
            if (json != null) accountViewModel.applyLocalBackup(json)
        }
    }

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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

        // â"€â"€ Theme section â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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

        // 10 color swatches in a 5Ã—2 grid (slot 9 = custom rainbow picker)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(0..4, 5..9).forEach { range ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    range.forEach { i ->
                        if (i == 9) {
                            RainbowColorSwatch(
                                selected = accentIndex == 9,
                                onClick  = { showColorPicker = true }
                            )
                        } else {
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
        }

        Spacer(Modifier.height(8.dp))

        // Coloring mode switches
        Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SettingsSwitch(
                label       = "Global theming",
                description = "All categories use the accent color",
                checked     = globalColoring,
                onChecked   = { on ->
                    viewModel.setGlobalColoring(on)
                    if (on) viewModel.setGranularColoring(false)
                    else    viewModel.setGranularColoring(true)
                }
            )
            SettingsSwitch(
                label       = "Granular coloring",
                description = "Each category uses its own color",
                checked     = granularColoring,
                onChecked   = { on ->
                    viewModel.setGranularColoring(on)
                    if (on) viewModel.setGlobalColoring(false)
                    else    viewModel.setGlobalColoring(true)
                }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // â"€â"€ Input mode section â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        SectionLabel("Input Mode")

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            SettingsSwitch(
                label       = "AI Mode",
                description = if (aiMode) "Expenses added via AI assistant" else "Expenses added manually",
                checked     = aiMode,
                onChecked   = { viewModel.setAiMode(it) }
            )
        }

        if (aiMode) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value         = aiApiKey,
                    onValueChange = { viewModel.setAiApiKey(it) },
                    label         = { Text("Mistral API Key") },
                    placeholder   = { Text("your key...") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = MaterialTheme.shapes.medium,
                    textStyle     = MaterialTheme.typography.bodySmall,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector        = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showApiKey) "Hide key" else "Show key",
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                )
                Text(
                    "Get your key at console.mistral.ai",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // â"€â"€ Currency section â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        SectionLabel("Currency")

        val selectedCurrency = remember(currencyCode) {
            AllCurrencies.firstOrNull { it.code == currencyCode }
        }
        NavigationDrawerItem(
            label = {
                Column {
                    Text("Currency")
                    Text(
                        "${selectedCurrency?.code ?: "USD"} - ${selectedCurrency?.name ?: "US Dollar"}",
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

        // â"€â"€ Categories section â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        SectionLabel("Categories")

        NavigationDrawerItem(
            label = { Text("Manage Categories") },
            icon  = { Icon(Icons.Rounded.Category, contentDescription = null) },
            selected = false,
            onClick = { onNavigateToCategories() },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // â"€â"€ Data section â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        SectionLabel("Data")

        NavigationDrawerItem(
            label = { Text("Export Transactions") },
            icon  = { Icon(Icons.Rounded.Upload, contentDescription = null) },
            selected = false,
            onClick = { exportCsvLauncher.launch("budget_${LocalDate.now()}.csv") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            label = { Text("Import Transactions") },
            icon  = { Icon(Icons.Rounded.Download, contentDescription = null) },
            selected = false,
            onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            label = { Text("Export Settings") },
            icon  = { Icon(Icons.Rounded.IosShare, contentDescription = null) },
            selected = false,
            onClick = { exportBackupLauncher.launch("pms_backup_${LocalDate.now()}.json") },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            label = { Text("Import Settings") },
            icon  = { Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null) },
            selected = false,
            onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

            Spacer(Modifier.height(16.dp))

            Text(
                "BudgetTracker v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } // end scrollable Column
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            selectedCode = currencyCode,
            onSelect     = { viewModel.setCurrencyCode(it.code) },
            onDismiss    = { showCurrencyPicker = false }
        )
    }

    if (showColorPicker) {
        RgbColorPickerDialog(
            initialHex = "#$customAccentHex",
            onConfirm  = { hex ->
                viewModel.setCustomAccentHex(hex.trimStart('#'))
                viewModel.setAccentIndex(9)
                showColorPicker = false
            },
            onDismiss  = { showColorPicker = false }
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
private fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked
        )
    }
}
