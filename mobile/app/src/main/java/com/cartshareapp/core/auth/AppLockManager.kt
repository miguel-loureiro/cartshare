package com.cartshareapp.core.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    companion object {
        private const val LAST_ACTIVE = "last_active"
        private const val TIMEOUT_MS = 5 * 60 * 1000L
    }

    fun updateLastActive() {
        prefs.edit().putLong(LAST_ACTIVE, System.currentTimeMillis()).apply()
    }

    fun isLocked(): Boolean {
        val last = prefs.getLong(LAST_ACTIVE, 0L)
        return System.currentTimeMillis() - last > TIMEOUT_MS
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}