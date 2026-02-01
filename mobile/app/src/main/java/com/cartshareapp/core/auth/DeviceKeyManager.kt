package com.cartshareapp.core.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import java.util.UUID

@Singleton
class DeviceKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "cartshare_device_prefs"
        private const val DEVICE_ID_KEY = "device_id"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the unique device ID, generating it if it doesn't exist yet.
     * This ID is persistent and tied to the app installation.
     */
    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(DEVICE_ID_KEY, null)
        if (!existing.isNullOrEmpty()) return existing

        val newId = generateDeviceId()
        prefs.edit { putString(DEVICE_ID_KEY, newId) }
        return newId
    }

    /**
     * Deletes the stored device ID.
     * Useful when signing out or deleting account.
     */
    fun clearDeviceId() {
        prefs.edit { remove(DEVICE_ID_KEY) }
    }

    /**
     * Generates a new random device ID.
     * Uses secure random UUID to prevent collisions and spoofing.
     */
    private fun generateDeviceId(): String =
        UUID.randomUUID().toString()
}