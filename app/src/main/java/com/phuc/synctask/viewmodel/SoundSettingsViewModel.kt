package com.phuc.synctask.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val android.content.Context.soundDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "sound_prefs")

private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
private val SOUND_VOLUME_KEY = intPreferencesKey("sound_volume")

data class SoundSettingsState(
    val isEnabled: Boolean = true,
    val volumePercent: Int = 80
)

class SoundSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.soundDataStore

    private val _state = MutableStateFlow(SoundSettingsState())
    val state: StateFlow<SoundSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dataStore.data.map { prefs -> prefs[SOUND_ENABLED_KEY] ?: true },
                dataStore.data.map { prefs -> (prefs[SOUND_VOLUME_KEY] ?: 80).coerceIn(0, 100) }
            ) { enabled, volume ->
                SoundSettingsState(isEnabled = enabled, volumePercent = volume)
            }.collect { settings ->
                _state.value = settings
            }
        }
    }

    fun setEnabled(value: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[SOUND_ENABLED_KEY] = value
            }
        }
    }

    fun setVolumePercent(value: Int) {
        val clamped = value.coerceIn(0, 100)
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[SOUND_VOLUME_KEY] = clamped
            }
        }
    }
}
