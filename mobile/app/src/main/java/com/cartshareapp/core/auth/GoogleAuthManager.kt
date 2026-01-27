package com.cartshareapp.core.auth

import android.content.Context
import android.credentials.GetCredentialException
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialManager: CredentialManager
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun signIn(): String? {
        // 1. Configure Google ID Option
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("572462600114-f4goen6i393vbiaq1shghfvjtnhv33vj.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()

        // 2. Create the Credential Request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            // 3. Launch the Bottom Sheet UI
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            // 4. Extract the ID Token
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdToken.idToken

            // 5. Authenticate with Firebase
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()

            idToken // Return success
        } catch (e: GetCredentialException) {
            // User canceled or failed
            null
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}