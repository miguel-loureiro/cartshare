package com.cartshareapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cartshareapp.auth.GoogleAuthManager
import com.cartshareapp.data.remote.AuthPreferences
import com.cartshareapp.presentation.MainApp
import com.cartshareapp.presentation.SplashAndLogin
import com.cartshareapp.ui.theme.CartShareAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Initialize the system splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            CartShareAppTheme {
                MainActivityContent()
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    val context = LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val prefs = remember { AuthPreferences(context) }

    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        isLoggedIn = auth.currentUser != null && !prefs.needsLogin()
    }

    when (isLoggedIn) {
        null -> SplashScreenPlaceholder()

        false -> SplashAndLogin(
            authManager = authManager,
            onAuthenticated = { token ->
                val credential =
                    GoogleAuthProvider.getCredential(token, null)

                FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .await()

                prefs.saveAuth(token)
                isLoggedIn = true
            }
        )

        true -> MainApp()
    }
}

@Composable
fun SplashScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show your logo or a progress bar while checking login status
        CircularProgressIndicator()
    }
}