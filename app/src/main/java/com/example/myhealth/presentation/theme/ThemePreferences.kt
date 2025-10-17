package com.example.myhealth.presentation.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore for theme preferences (extension on Context)
val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    /** Persist the selected theme mode. */
    suspend fun saveTheme(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    /** Read theme mode; default to System if nothing stored. */
    fun getTheme(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            val raw = prefs[THEME_MODE_KEY]
            runCatching { ThemeMode.valueOf(raw ?: ThemeMode.System.name) }
                .getOrDefault(ThemeMode.System)
        }
}
