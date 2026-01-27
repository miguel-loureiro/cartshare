package com.cartshareapp.core.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.cartshareapp.core.auth.GoogleAuthManager
import com.cartshareapp.data.local.AuthPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthPreferences(@ApplicationContext context: Context): AuthPreferences {
        return AuthPreferences(context)
    }

    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context
    ): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideGoogleAuthManager(
        credentialManager: CredentialManager,
        @ApplicationContext context: Context
    ): GoogleAuthManager =
        GoogleAuthManager(context, credentialManager)
}