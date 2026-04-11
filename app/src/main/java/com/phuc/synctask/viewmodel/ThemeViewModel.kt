package com.phuc.synctask.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.content.Context.themeDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "theme_prefs")

private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.themeDataStore

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data
                .map { prefs -> prefs[DARK_MODE_KEY] ?: false }
                .collect { _isDarkTheme.value = it }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DARK_MODE_KEY] = !(_isDarkTheme.value)
            }
        }
    }
}
