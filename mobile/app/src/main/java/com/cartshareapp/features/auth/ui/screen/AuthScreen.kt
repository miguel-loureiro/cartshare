package com.cartshareapp.features.auth.ui.screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cartshareapp.R
import com.cartshareapp.core.designsystem.AppImages
import com.cartshareapp.features.auth.ui.AuthUiState
import com.cartshareapp.features.auth.ui.viewmodel.AuthViewModel

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthenticated: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Texto com link para a política de privacidade
    val annotatedString = buildAnnotatedString {
        append("By signing in, you agree to our ")
        pushStringAnnotation("policy", "policy")
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            )
        ) {
            append("Privacy Policy")
        }
        pop()
    }

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

        // --- Topo: Branding ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Nota: Certifica-te que AppImages.Logo existe no teu projeto
            Icon(
                painter = painterResource(id = AppImages.SplashBackground), // Exemplo genérico
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "CartShare",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Centro: Ações de Login ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (val state = uiState) {
                is AuthUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthUiState.Error -> {
                    // Mostra o erro e permite tentar novamente
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    SignInButton { viewModel.signIn() }
                }
                else -> {
                    SignInButton { viewModel.signIn() }
                }
            }
        }

        // --- Base: Links Legais ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { offset ->
                    annotatedString
                        .getStringAnnotations("policy", offset, offset)
                        .firstOrNull()?.let { onNavigateToPrivacy() }
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "GDPR compliant. Your data stays yours.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.titleMedium
        )
    }
}