package com.cartshareapp.features.auth.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cartshareapp.R
import com.cartshareapp.features.auth.ui.AuthUiState
import com.cartshareapp.features.auth.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val annotatedString = buildAnnotatedString {
        append("By signing in, you agree to our ")
        pushStringAnnotation(tag = "policy", annotation = "policy")
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
        ) {
            append("Privacy Policy")
        }
        pop()
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "policy", start = offset, end = offset)
                .firstOrNull()?.let { onNavigateToPrivacy() }
        }
    )

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Section: Branding ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
            Image(
                painter = painterResource(id = R.drawable.cartshare2),
                contentDescription = "Logo",
                modifier = Modifier.size(160.dp)
            )
            Text(
                text = "CartShare",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Middle Section: Sign In ---
        Box(contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is AuthUiState.Loading -> CircularProgressIndicator()
                else -> {
                    Button(
                        onClick = { viewModel.signIn() },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Sign in with Google")
                    }
                }
            }
        }

        // --- Bottom Section: GDPR Notice ---
        Text(
            text = "GDPR Compliance: Signing out ensures no further information manipulation. " +
                    "We prioritize your privacy and data protection according to EU directives.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

fun onNavigateToPrivacy() {
    TODO("Not yet implemented")
}
