package com.pardonmyspending.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pardonmyspending.app.viewmodel.AccountViewModel
import com.pardonmyspending.app.viewmodel.BackupUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    val account by viewModel.account.collectAsState()
    val status  by viewModel.status.collectAsState()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier         = Modifier.fillMaxHeight(0.85f)
    ) {
        if (account == null) {
            SignedOutContent(
                onSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) }
            )
        } else {
            SignedInContent(
                displayName = account?.displayName ?: "User",
                email       = account?.email ?: "",
                status      = status,
                onBackup    = { viewModel.backupNow() },
                onRestore   = { viewModel.restoreLatest() },
                onSignOut   = { viewModel.signOut(); onDismiss() }
            )
        }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.Backup,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint     = MaterialTheme.colorScheme.primary
        )
        Text(
            "Backup & Restore",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
        )
        Text(
            "Sign in with Google to back up your transactions, budgets, and categories to Google Drive - and restore them on any device.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick  = onSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Continue with Google",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun SignedInContent(
    displayName: String,
    email: String,
    status: BackupUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSignOut: () -> Unit
) {
    val initial = displayName.firstOrNull()?.uppercaseChar() ?: 'U'
    val isLoading = status is BackupUiState.Loading

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            displayName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Status message
        when (status) {
            is BackupUiState.Success -> Text(
                status.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            is BackupUiState.Error -> Text(
                status.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            else -> {}
        }

        Button(
            onClick  = onBackup,
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color     = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Back Up Now",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        OutlinedButton(
            onClick  = onRestore,
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Restore Latest Backup",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        TextButton(
            onClick  = onSignOut,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                "Sign out",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
