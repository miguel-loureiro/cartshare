package com.cartshareapp.features.auth.ui.screen

import android.text.Spanned
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.cartshareapp.R

// PrivacyPolicyScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GDPR & Privacy Policy") },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Converts our XML string (with <b> tags) into a visual text
            Text(
                text = HtmlCompat.fromHtml(
                    stringResource(id = R.string.gdpr_policy_text),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toAnnotatedString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper to convert Spanned HTML to AnnotatedString
fun Spanned.toAnnotatedString(): AnnotatedString {
    return buildAnnotatedString {
        append(this@toAnnotatedString.toString())
        // (Simplified for example; you can add styling for bold tags here if needed)
    }
}