package com.phuc.synctask.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.phuc.synctask.model.AppNotification
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository {
    private val database = FirebaseDatabase.getInstance().reference

    fun observeNotifications(
        userId: String,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val ref = database.child("notifications").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<AppNotification>()
                for (child in snapshot.children) {
                    val notif = child.getValue(AppNotification::class.java)
                    if (notif != null) {
                        list.add(notif.copy(id = child.key ?: ""))
                    }
                }
                list.sortByDescending { it.timestamp }
                onUpdate(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    suspend fun addNotification(
        userId: String,
        title: String,
        message: String
    ): Result<Unit> {
        return try {
            val ref = database.child("notifications").child(userId).push()
            val notif = AppNotification(
                id = ref.key ?: "",
                userId = userId,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
            ref.setValue(notif).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> {
        return try {
            database.child("notifications").child(userId)
                .child(notificationId).child("isRead").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
