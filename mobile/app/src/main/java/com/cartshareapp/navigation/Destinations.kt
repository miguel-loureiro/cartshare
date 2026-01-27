package com.cartshareapp.navigation

sealed class Destinations(val route: String) {
    object Splash : Destinations("splash")
    object Auth : Destinations("auth")
    object SignedIn : Destinations("signed_in")
    object Profile : Destinations("profile") // Add this
    object PrivacyPolicy : Destinations("privacy_policy")
}