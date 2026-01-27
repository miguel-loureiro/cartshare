package com.cartshareapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cartshareapp.features.auth.ui.theme.CartShareAppTheme
import com.cartshareapp.navigation.MainNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            CartShareAppTheme {
                // Just call the NavHost; it uses hiltViewModel() internally
                MainNavHost()
            }
        }
    }
}