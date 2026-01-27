package com.cartshareapp.features.auth.ui


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CartShare") })
        }
    ) {
        Text(
            text = "User Authorized ✔",
            modifier = Modifier.padding(it)
        )
    }
}