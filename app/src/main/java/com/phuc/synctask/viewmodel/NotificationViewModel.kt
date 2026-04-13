package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.data.repository.FirebaseNotificationRepository
import com.phuc.synctask.model.AppNotification
import com.phuc.synctask.utils.AppSoundEffect
import com.phuc.synctask.utils.AppSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseNotificationRepository()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadCount: StateFlow<Int> = _notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var cancelObservation: (() -> Unit)? = null
    
    private val appStartTime = System.currentTimeMillis()

    init {
        listenToNotifications()
    }

    private fun listenToNotifications() {
        val uid = auth.currentUser?.uid ?: return
        cancelObservation?.invoke()
        cancelObservation = repository.observeNotifications(
            userId = uid,
            onUpdate = { list ->
                val previousList = _notifications.value
                val previousSize = previousList.size
                
                _notifications.value = list
                
                if (list.isNotEmpty() && list.size > previousSize && previousSize > 0) {
                    val newest = list.first()
                    if (newest.timestamp > appStartTime && !newest.isRead) {
                        AppSoundPlayer.play(AppSoundEffect.NOTIFICATION)
                    }
                } else if (previousSize == 0 && list.any { it.timestamp > appStartTime && !it.isRead }) {
                     AppSoundPlayer.play(AppSoundEffect.NOTIFICATION)
                }
            },
            onError = {
                // Ignore error
            }
        )
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAsRead(uid, notificationId)
        }
    }

    fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _notifications.value.filter { !it.isRead }.forEach {
                repository.markAsRead(uid, it.id)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelObservation?.invoke()
    }
}
