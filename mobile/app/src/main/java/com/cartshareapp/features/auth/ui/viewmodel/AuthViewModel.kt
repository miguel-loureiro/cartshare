package com.cartshareapp.features.auth.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cartshareapp.R
import com.cartshareapp.core.auth.GoogleAuthManager
import com.cartshareapp.data.local.AuthPreferences
import com.cartshareapp.features.auth.ui.AuthUiState
import com.cartshareapp.navigation.Destinations
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val prefs: AuthPreferences
) : ViewModel() {

    // 1. UI State for the Login Screen (Loading, Error, etc.)
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 2. Navigation State
    var startDestination by mutableStateOf(Destinations.Splash.route)
        private set

    var privacyPolicyText by mutableStateOf("")
        private set

    // 3. Combined Login Status Flow
    // This watches both Firebase Auth AND your local DataStore preferences
    private val isLoggedInFlow: Flow<Boolean?> = combine(
        prefs.needsLoginFlow,
        // We use a simple flow that emits the current user status
        flow { emit(FirebaseAuth.getInstance().currentUser) }
    ) { needsLogin, user ->
        user != null && !needsLogin
    }

    init {
        observeLoginStatus()
    }

    private fun observeLoginStatus() {
        viewModelScope.launch {
            isLoggedInFlow.collect { loggedIn ->
                // Delay for splash screen feel (optional)
                if (loggedIn == null) return@collect

                startDestination = if (loggedIn) {
                    Destinations.SignedIn.route
                } else {
                    Destinations.Auth.route
                }
            }
        }
    }

    /**
     * Triggers the Google Sign-In flow using Credential Manager.
     */
    fun signIn() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // This calls the logic in your GoogleAuthManager
                val result = authManager.signIn()

                if (result != null) {
                    // Success: Update local prefs
                    prefs.setNeedsLogin(false)
                    _uiState.value = AuthUiState.Authenticated
                } else {
                    _uiState.value = AuthUiState.Error("Sign in canceled")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Authentication failed")
            }
        }
    }

    /**
     * Signs out from Firebase and Credential Manager, then resets local prefs.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                // Clear Firebase session
                FirebaseAuth.getInstance().signOut()

                // Clear Google Credential Manager state
                authManager.signOut()

                // Update DataStore (this triggers the observeLoginStatus collector)
                prefs.setNeedsLogin(true)

                // Reset UI state for the next login attempt
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Sign out failed: ${e.localizedMessage}")
            }
        }
    }

    /**
     * GDPR Compliance: Implements the "Right to Erasure".
     * Deletes the user from Firebase and clears local preferences.
     */
    fun deleteUserAccount(onReauthRequired: () -> Unit, onAccountDeleted: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                user?.delete()?.await()
                // Success cleanup
                authManager.signOut()
                prefs.setNeedsLogin(true)
                _uiState.value = AuthUiState.Idle
                onAccountDeleted()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                // Trigger the UI to ask user to sign out and back in
                _uiState.value = AuthUiState.Error("Security: Please sign out and back in to verify your identity before deleting.")
                onReauthRequired()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Deletion failed")
            }
        }
    }

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }

    fun loadPrivacyPolicy(context: Context) {
        viewModelScope.launch {
            // OPTION 1: From Resources (Fastest/Reliable)
            privacyPolicyText = context.getString(R.string.gdpr_policy_text)

        }
    }
}