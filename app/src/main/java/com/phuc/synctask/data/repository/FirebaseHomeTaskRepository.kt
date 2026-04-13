package com.phuc.synctask.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.UserProfile
import kotlinx.coroutines.tasks.await

class FirebaseHomeTaskRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    fun observeTasks(
        uid: String,
        onTasks: (List<FirebaseTask>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val ref = database.reference.child("tasks").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<FirebaseTask>()
                for (child in snapshot.children) {
                    val task = child.getValue<FirebaseTask>()
                    if (task != null) {
                        task.id = child.key ?: ""
                        taskList.add(task)
                    }
                }
                taskList.sortByDescending { it.timestamp }
                onTasks(taskList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    suspend fun addTask(uid: String, task: FirebaseTask): Result<Unit> = runCatching {
        val ref = database.reference.child("tasks").child(uid)
        val taskId = ref.push().key ?: error("Không tạo được taskId")
        ref.child(taskId).setValue(task.copy(id = taskId)).await()
    }

    suspend fun deleteTask(uid: String, taskId: String): Result<Unit> = runCatching {
        database.reference.child("tasks").child(uid).child(taskId).removeValue().await()
    }

    suspend fun restoreTask(uid: String, task: FirebaseTask): Result<Unit> = runCatching {
        val taskId = task.id.ifBlank { error("Task id không hợp lệ để khôi phục") }
        database.reference.child("tasks").child(uid).child(taskId).setValue(task).await()
    }

    suspend fun updateTaskCompleted(uid: String, taskId: String, isCompleted: Boolean): Result<Unit> = runCatching {
        database.reference
            .child("tasks")
            .child(uid)
            .child(taskId)
            .child("isCompleted")
            .setValue(isCompleted)
            .await()
    }

    suspend fun loadUserProfile(uid: String): UserProfile? {
        val snapshot = database.reference.child("users").child(uid).get().await()
        return snapshot.getValue<UserProfile>()?.copy(uid = uid)
    }

    suspend fun saveUnlockedAchievements(uid: String, unlocked: List<String>): Result<Unit> = runCatching {
        database.reference
            .child("users")
            .child(uid)
            .child("unlockedAchievements")
            .setValue(unlocked)
            .await()
    }
}
