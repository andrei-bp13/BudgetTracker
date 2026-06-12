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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val aiMode           by viewModel.aiMode.collectAsState()
    val customAccentHex  by viewModel.customAccentHex.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showColorPicker    by remember { mutableStateOf(false) }

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

        // 10 color swatches in a 5×2 grid (slot 9 = custom rainbow picker)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0..4, 5..9).forEach { range ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onChecked   = { viewModel.setGlobalColoring(it) }
            )
            SettingsSwitch(
                label       = "Granular coloring",
                description = "Each category uses its own color",
                checked     = granularColoring,
                onChecked   = { viewModel.setGranularColoring(it) }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // ── Input mode section ────────────────────────────────────────────────
        SectionLabel("Input Mode")

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            SettingsSwitch(
                label       = "AI Mode",
                description = if (aiMode) "Expenses added via AI assistant" else "Expenses added manually",
                checked     = aiMode,
                onChecked   = { viewModel.setAiMode(it) }
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

    if (showColorPicker) {
        RgbColorPickerDialog(
            initialHex = customAccentHex,
            onConfirm  = { hex ->
                viewModel.setCustomAccentHex(hex)
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
private fun RainbowColorSwatch(selected: Boolean, onClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Hue sweep + white radial center
        Canvas(modifier = Modifier.matchParentSize()) {
            val sweepBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
                )
            )
            drawCircle(brush = sweepBrush)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    radius = size.minDimension / 2f
                )
            )
            if (selected) {
                drawCircle(
                    color  = borderColor,
                    style  = Stroke(width = 3.dp.toPx()),
                    radius = size.minDimension / 2f - 1.5.dp.toPx()
                )
            }
        }
        // Pencil icon
        Icon(
            imageVector        = Icons.Rounded.Edit,
            contentDescription = "Custom color",
            tint               = Color.Black,
            modifier           = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun RgbColorPickerDialog(
    initialHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initColor = runCatching {
        Color(0xFF000000L or initialHex.toLong(16))
    }.getOrElse { Color(0xFF2196F3) }

    var r by remember { mutableFloatStateOf(initColor.red   * 255f) }
    var g by remember { mutableFloatStateOf(initColor.green * 255f) }
    var b by remember { mutableFloatStateOf(initColor.blue  * 255f) }

    val preview = Color(r / 255f, g / 255f, b / 255f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Accent Color", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(preview)
                )

                // RGB sliders
                listOf(
                    Triple("R", r, Color(1f, 0f, 0f)) to { v: Float -> r = v },
                    Triple("G", g, Color(0f, 0.7f, 0f)) to { v: Float -> g = v },
                    Triple("B", b, Color(0f, 0.4f, 1f)) to { v: Float -> b = v }
                ).forEach { (info, setter) ->
                    val (label, value, tint) = info
                    Row(
                        verticalAlignment  = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            label,
                            style  = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color  = tint,
                            modifier = Modifier.width(14.dp)
                        )
                        Slider(
                            value         = value,
                            onValueChange = setter,
                            valueRange    = 0f..255f,
                            modifier      = Modifier.weight(1f),
                            colors        = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint)
                        )
                        Text(
                            value.toInt().toString(),
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }

                // Hex preview
                val hexStr = "%02X%02X%02X".format(r.toInt(), g.toInt(), b.toInt())
                Text(
                    "#$hexStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                val hex = "%02X%02X%02X".format(r.toInt(), g.toInt(), b.toInt())
                onConfirm(hex)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
