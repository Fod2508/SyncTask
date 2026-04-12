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

private val android.content.Context.onboardingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "onboarding_prefs")

private val FIRST_TIME_KEY = booleanPreferencesKey("is_first_time")

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.onboardingDataStore

    // null = chưa load xong, true = lần đầu, false = đã xem rồi
    private val _isFirstTime = MutableStateFlow<Boolean?>(null)
    val isFirstTime: StateFlow<Boolean?> = _isFirstTime.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data
                .map { prefs -> prefs[FIRST_TIME_KEY] ?: true }
                .collect { _isFirstTime.value = it }
        }
    }

    /** Gọi sau khi user hoàn thành onboarding */
    fun markOnboardingDone() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[FIRST_TIME_KEY] = false }
        }
    }

    /** Reset về trạng thái ban đầu — gọi khi Đăng xuất để tiện test nhiều tài khoản */
    fun resetOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[FIRST_TIME_KEY] = true }
        }
    }
}
