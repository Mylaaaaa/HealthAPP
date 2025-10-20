package com.zihanwang.myhealth.presentation.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Single source of truth for theme mode across the app.
 * Loads from and saves to DataStore so the choice survives restarts.
 */
class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            ThemePreferences.getTheme(context).collectLatest { saved ->
                _themeMode.value = saved
            }
        }
    }

    /** Update both in-memory state and persistent storage. */
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            ThemePreferences.saveTheme(context, mode)
        }
    }
}
