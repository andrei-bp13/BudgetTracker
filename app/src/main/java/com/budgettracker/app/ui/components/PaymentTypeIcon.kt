package com.budgettracker.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgettracker.app.data.model.PaymentType

@Composable
fun PaymentTypeIcon(
    type: PaymentType,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    when (type) {
        PaymentType.CASH -> Icon(
            imageVector    = Icons.Outlined.Payments,
            contentDescription = "Cash",
            tint           = tint,
            modifier       = modifier.size(size)
        )
        else -> Icon(
            imageVector        = Icons.Outlined.CreditCard,
            contentDescription = if (type == PaymentType.DEBIT) "Debit" else "Credit",
            tint               = tint,
            modifier           = modifier.size(size)
        )
    }
}

fun PaymentType.displayName(): String = when (this) {
    PaymentType.DEBIT  -> "Debit"
    PaymentType.CREDIT -> "Credit"
    PaymentType.CASH   -> "Cash"
}
