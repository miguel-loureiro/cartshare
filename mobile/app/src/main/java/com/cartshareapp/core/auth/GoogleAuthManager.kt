package com.cartshareapp.core.auth

import android.content.Context
import android.credentials.GetCredentialException
import android.os.Build
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun signIn(): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(
                "572462600114-f4goen6i393vbiaq1shghfvjtnhv33vj.apps.googleusercontent.com"
            )
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val googleIdToken =
                GoogleIdTokenCredential.createFrom(result.credential.data)

            val credential =
                GoogleAuthProvider.getCredential(googleIdToken.idToken, null)

            auth.signInWithCredential(credential).await()

            googleIdToken.idToken
        } catch (e: GetCredentialException) {
            // User canceled sign-in → NOT an error
            null
        }
    }

    /** ✅ Safe suspend sign-out */
    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(
                ClearCredentialStateRequest()
            )
        }
    }
}