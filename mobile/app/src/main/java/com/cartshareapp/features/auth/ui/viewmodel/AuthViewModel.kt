package com.cartshareapp.features.auth.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cartshareapp.core.auth.AppLockManager
import com.cartshareapp.core.auth.DeviceKeyManager
import com.cartshareapp.core.auth.GoogleAuthManager
import com.cartshareapp.features.auth.ui.AuthUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val deviceKeyManager: DeviceKeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun isSignedIn(): Boolean =
        FirebaseAuth.getInstance().currentUser != null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun signIn() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val token = authManager.signIn()
                if (token != null) {
                    deviceKeyManager.getOrCreateDeviceId()
                    _uiState.value = AuthUiState.Authenticated
                } else {
                    _uiState.value = AuthUiState.Error("Sign-in cancelled")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.localizedMessage ?: "Sign-in failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            authManager.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun deleteAccount(
        onReauthRequired: () -> Unit,
        onDeleted: () -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                user.delete().await()
                signOut()
                onDeleted()
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                _uiState.value = AuthUiState.Error(
                    "Please sign in again to delete your account."
                )
                onReauthRequired()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.localizedMessage ?: "Account deletion failed"
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = AuthUiState.Idle
    }
}