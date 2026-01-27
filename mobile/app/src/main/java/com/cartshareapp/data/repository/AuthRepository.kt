package com.cartshareapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun firebaseSignIn(idToken: String): FirebaseUser {
        val credential =
            GoogleAuthProvider.getCredential(idToken, null)

        val result =
            firebaseAuth.signInWithCredential(credential).await()

        return result.user ?: error("User is null")
    }

    suspend fun createUserIfNotExists(user: FirebaseUser) {
        val ref = firestore.collection("users").document(user.uid)

        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(
                mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "name" to user.displayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun isLoggedIn(): Boolean =
        firebaseAuth.currentUser != null
}