package com.cartshareapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val NEEDS_LOGIN_KEY = booleanPreferencesKey("needs_login")

    // A Flow that emits whenever the value in DataStore changes
    val needsLoginFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NEEDS_LOGIN_KEY] ?: true // Default to true
        }

    suspend fun setNeedsLogin(needsLogin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NEEDS_LOGIN_KEY] = needsLogin
        }
    }
}