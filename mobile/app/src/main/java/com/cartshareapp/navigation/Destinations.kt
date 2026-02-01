package com.cartshareapp.navigation

sealed class Destinations(val route: String) {
    object CompanySplash : Destinations("company_splash")
    object AppLogo : Destinations("app_logo")
    object Auth : Destinations("auth")
    object Home : Destinations("home")
    object Profile : Destinations("profile")
    object PrivacyPolicy : Destinations("privacy_policy")
}
