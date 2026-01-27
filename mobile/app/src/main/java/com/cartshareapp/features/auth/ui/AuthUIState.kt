package com.cartshareapp.features.auth.ui

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Authenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}