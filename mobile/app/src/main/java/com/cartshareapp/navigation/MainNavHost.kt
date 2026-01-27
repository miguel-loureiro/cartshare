package com.cartshareapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cartshareapp.features.auth.ui.SplashScreenPlaceholder
import com.cartshareapp.features.auth.ui.screen.AuthScreen
import com.cartshareapp.features.auth.ui.screen.PrivacyPolicyScreen
import com.cartshareapp.features.auth.ui.screen.ProfileScreen
import com.cartshareapp.features.auth.ui.screen.SignedInScreen
import com.cartshareapp.features.auth.ui.viewmodel.AuthViewModel

@Composable
fun MainNavHost(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val startRoute = viewModel.startDestination

    // Observe changes to startDestination and navigate accordingly
    LaunchedEffect(startRoute) {
        if (startRoute != Destinations.Splash.route) {
            navController.navigate(startRoute) {
                popUpTo(Destinations.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.Splash.route
    ) {
        composable(Destinations.Splash.route) {
            SplashScreenPlaceholder()
        }

        composable(Destinations.Auth.route) {
            AuthScreen(
                viewModel = viewModel,
                onAuthenticated = {
                    navController.navigate(Destinations.SignedIn.route) {
                        popUpTo(Destinations.Auth.route) { inclusive = true }
                    }
                },

                onNavigateToPrivacy = {
                    navController.navigate(Destinations.PrivacyPolicy.route)
                }
            )
        }

        composable(Destinations.SignedIn.route) {
            SignedInScreen(
                onNavigateToProfile = { navController.navigate(Destinations.Profile.route) },
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Destinations.Auth.route) { popUpTo(0) }
                }
            )
        }

        composable(Destinations.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(Destinations.PrivacyPolicy.route) },
                onAccountDeleted = {
                    navController.navigate(Destinations.Auth.route) { popUpTo(0) }
                }
            )
        }
    }
}