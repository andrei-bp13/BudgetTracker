package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.data.repository.ParsedTransaction
import com.pardonmyspending.app.data.repository.toTransaction
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInputSheet(
    viewModel: TransactionViewModel,
    onDismiss: () -> Unit,
    onParsed: (ParsedTransaction) -> Unit,
    onSwitchToManual: () -> Unit
) {
    val scope   = rememberCoroutineScope()
    var trustAi by remember { mutableStateOf(false) }
    var input   by remember { mutableStateOf("") }
    var parsing by remember { mutableStateOf(false) }
    var error   by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (input.isBlank() || parsing) return
        scope.launch {
            parsing = true
            error   = null
            val result = viewModel.parseTransactionWithAI(input)
            parsing = false
            result.fold(
                onSuccess = { parsed ->
                    if (trustAi) {
                        viewModel.addTransaction(parsed.toTransaction())
                        onDismiss()
                    } else {
                        onParsed(parsed)
                    }
                },
                onFailure = { err -> error = err.message ?: "Unknown error" }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.85f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "AI Assistant",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 18.sp)
            )

            // â"€â"€ Switches â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape  = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                "Trust AI",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                "Add transaction without confirmation (not recommended)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = trustAi, onCheckedChange = { trustAi = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                "Manual mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                "Switch to classic input form",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = false, onCheckedChange = { if (it) onSwitchToManual() })
                    }
                }
            }

            // â"€â"€ Input row â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value           = input,
                    onValueChange   = { input = it; error = null },
                    label           = { Text("Describe your transaction") },
                    placeholder     = { Text("e.g. lunch ${'$'}12 cash yesterday") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    shape           = MaterialTheme.shapes.medium,
                    enabled         = !parsing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                if (parsing) {
                    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else {
                    FilledIconButton(
                        onClick  = ::submit,
                        enabled  = input.isNotBlank(),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Send",
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
