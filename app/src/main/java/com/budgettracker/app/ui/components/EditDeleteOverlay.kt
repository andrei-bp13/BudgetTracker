package com.pardonmyspending.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.data.model.Transaction
import kotlin.math.roundToInt

/**
 * Drop-in overlay: a full-screen scrim + an accent-colored box positioned exactly
 * over [bounds], showing Edit / Delete buttons (Delete requires confirmation).
 *
 * Must live inside the same root Box whose coordinates are captured in [rootCoords].
 */
@Composable
fun EditDeleteOverlay(
    transaction : Transaction,
    bounds      : Rect,
    rootCoords  : LayoutCoordinates,
    onEdit      : (Transaction) -> Unit,
    onDelete    : (Transaction) -> Unit,
    onDismiss   : () -> Unit
) {
    var confirmDelete by remember(transaction.id) { mutableStateOf(false) }
    val accent  = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    // Transparent scrim -" tapping outside the overlay dismisses it
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    )

    val rootOrigin = rootCoords.boundsInRoot()
    val relX = (bounds.left - rootOrigin.left).roundToInt()
    val relY = (bounds.top  - rootOrigin.top ).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(relX, relY) }
            .size(
                width  = with(density) { bounds.width.roundToInt().toDp() },
                height = with(density) { bounds.height.roundToInt().toDp() }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.93f)),
        contentAlignment = Alignment.Center
    ) {
        if (confirmDelete) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OverlayIconButton(Icons.Rounded.Check, "Confirm") {
                    onDelete(transaction); onDismiss()
                }
                OverlayIconButton(Icons.Rounded.Close, "Cancel") {
                    confirmDelete = false
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OverlayIconButton(Icons.Rounded.Edit, "Edit") {
                    onEdit(transaction); onDismiss()
                }
                OverlayIconButton(Icons.Rounded.Delete, "Delete") {
                    confirmDelete = true
                }
            }
        }
    }
}

@Composable
fun OverlayIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick  = onClick,
        modifier = Modifier.size(37.dp),
        colors   = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor   = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp))
    }
}
