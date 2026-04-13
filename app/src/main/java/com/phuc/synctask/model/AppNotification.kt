package com.phuc.synctask.model

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @field:JvmField
    val isRead: Boolean = false // Firebase often needs @field:JvmField for isRead parsing properly
)
