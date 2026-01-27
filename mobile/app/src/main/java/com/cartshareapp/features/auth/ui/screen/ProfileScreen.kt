package com.cartshareapp.features.auth.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cartshareapp.features.auth.ui.AuthUiState
import com.cartshareapp.features.auth.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Profile Header ---
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = FirebaseAuth.getInstance().currentUser?.email ?: "User",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Legal Section ---
            Text(
                text = "Legal & Privacy",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Privacy Policy Navigation Item
            Surface(
                onClick = onNavigateToPrivacy,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Privacy Policy (GDPR Compliance)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Danger Zone ---
            Text(
                text = "Danger Zone",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))

            // Deletion Error Message
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account (Right to Erasure)")
            }

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }

    // Deletion Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Permanently delete account?") },
            text = { Text("This will purge all your data from our servers. This action is final and satisfies GDPR requirements.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteUserAccount(
                            onReauthRequired = { /* Stay on screen to see error */ },
                            onAccountDeleted = { onAccountDeleted() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}