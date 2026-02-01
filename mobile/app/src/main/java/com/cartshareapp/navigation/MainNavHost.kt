package com.cartshareapp.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cartshareapp.features.auth.ui.screen.AppLogoScreen
import com.cartshareapp.features.auth.ui.screen.AuthScreen
import com.cartshareapp.features.auth.ui.screen.HomeScreen
import com.cartshareapp.features.auth.ui.screen.PrivacyPolicyScreen
import com.cartshareapp.features.auth.ui.screen.ProfileScreen
import com.cartshareapp.features.auth.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun MainNavHost(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Destinations.AppLogo.route,
            modifier = Modifier.padding(padding)
        ) {

            // 1️⃣ App Logo Screen (Compose splash)
            composable(Destinations.AppLogo.route) {
                AppLogoScreen {
                    if (viewModel.isSignedIn()) {
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.AppLogo.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Destinations.Auth.route) {
                            popUpTo(Destinations.AppLogo.route) { inclusive = true }
                        }
                    }
                }
            }

            // 2️⃣ Auth Screen
            composable(Destinations.Auth.route) {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthenticated = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Welcome to CartShare 👋",
                                duration = SnackbarDuration.Short
                            )
                        }
                        navController.navigate(Destinations.Home.route) {
                            popUpTo(Destinations.Auth.route) { inclusive = true }
                        }
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(Destinations.PrivacyPolicy.route)
                    }
                )
            }

            // 3️⃣ Home Screen
            composable(Destinations.Home.route) {
                HomeScreen(
                    onNavigateToProfile = {
                        navController.navigate(Destinations.Profile.route)
                    },
                    onCreateCart = {
                        // TODO: navigate to CreateCartScreen later
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            // 4️⃣ Profile Screen
            composable(Destinations.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPrivacy = {
                        navController.navigate(Destinations.PrivacyPolicy.route)
                    },
                    onAccountDeleted = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Account deleted successfully",
                                duration = SnackbarDuration.Short
                            )
                        }
                        navController.navigate(Destinations.Auth.route) {
                            popUpTo(0)
                        }
                    }
                )
            }

            // 5️⃣ Privacy Policy
            composable(Destinations.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}