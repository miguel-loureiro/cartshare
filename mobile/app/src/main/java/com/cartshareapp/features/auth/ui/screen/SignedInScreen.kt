package com.cartshareapp.features.auth.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cartshareapp.features.auth.ui.theme.CartShareAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignedInScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit // Added the missing parameter
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CartShare") },
                actions = {
                    // Standard place for Settings/Profile
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "You are signed in ✅",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Welcome back! You can now start using CartShare.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignedInScreenPreview() {
    CartShareAppTheme {
        Surface {
            SignedInScreen(
                onSignOut = {},
                onNavigateToProfile = {} // Added dummy lambda for preview
            )
        }
    }
}