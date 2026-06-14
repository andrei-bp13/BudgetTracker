package com.pardonmyspending.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RainbowColorSwatch(
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    val borderColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    radius = this.size.minDimension / 2f
                )
            )
            if (selected) {
                drawCircle(
                    color  = borderColor,
                    style  = Stroke(width = 3.dp.toPx()),
                    radius = this.size.minDimension / 2f - 1.5.dp.toPx()
                )
            }
        }
        Icon(
            imageVector        = Icons.Rounded.Edit,
            contentDescription = "Custom color",
            tint               = Color.Black,
            modifier           = Modifier.size(size * 0.4f)
        )
    }
}

@Composable
fun RgbColorPickerDialog(
    initialHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initColor = runCatching {
        Color(0xFF000000L or initialHex.trimStart('#').toLong(16))
    }.getOrElse { Color(0xFF4361EE) }

    var r by remember { mutableFloatStateOf(initColor.red   * 255f) }
    var g by remember { mutableFloatStateOf(initColor.green * 255f) }
    var b by remember { mutableFloatStateOf(initColor.blue  * 255f) }

    val preview = Color(r / 255f, g / 255f, b / 255f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(preview)
                )

                listOf(
                    Triple("R", r, Color(1f, 0f, 0f)) to { v: Float -> r = v },
                    Triple("G", g, Color(0f, 0.7f, 0f)) to { v: Float -> g = v },
                    Triple("B", b, Color(0f, 0.4f, 1f)) to { v: Float -> b = v }
                ).forEach { (info, setter) ->
                    val (label, value, tint) = info
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            label,
                            style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color    = tint,
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
                val hex = "#%02X%02X%02X".format(r.toInt(), g.toInt(), b.toInt())
                onConfirm(hex)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
