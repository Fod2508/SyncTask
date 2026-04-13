package com.phuc.synctask.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.phuc.synctask.model.Group
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.model.UserProfile
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseGroupTaskRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    fun observeGroupInfo(
        groupId: String,
        onGroup: (Group?) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val ref = database.reference.child("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onGroup(snapshot.getValue<Group>())
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    fun observeGroupTasks(
        groupId: String,
        onTasks: (List<GroupTask>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val ref = database.reference.child("groupTasks").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<GroupTask>()
                for (child in snapshot.children) {
                    val task = child.getValue<GroupTask>()
                    if (task != null) {
                        task.id = child.key ?: ""
                        task.groupId = groupId
                        tasks.add(task)
                    }
                }
                tasks.sortByDescending { it.timestamp }
                onTasks(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    suspend fun loadUserProfile(uid: String): UserProfile? {
        val snapshot = database.reference.child("users").child(uid).get().await()
        return snapshot.getValue<UserProfile>()?.copy(uid = uid)
    }

    suspend fun fetchMemberNames(memberUids: List<String>): Map<String, String> {
        if (memberUids.isEmpty()) return emptyMap()
        val usersRef = database.reference.child("users")
        val map = mutableMapOf<String, String>()
        for (uid in memberUids.distinct()) {
            val snap = usersRef.child(uid).get().await()
            map[uid] = snap.child("displayName").getValue(String::class.java) ?: "User"
        }
        return map
    }

    suspend fun addGroupTask(
        groupId: String,
        uid: String,
        title: String,
        description: String,
        dueDate: Long?,
        assignedToId: String?
    ): Result<Unit> = runCatching {
        val ref = database.reference.child("groupTasks").child(groupId)
        val taskId = ref.push().key ?: error("Không tạo được taskId")
        val task = GroupTask(
            id = taskId,
            groupId = groupId,
            title = title,
            description = description,
            isCompleted = false,
            creatorId = uid,
            assignedToId = assignedToId,
            timestamp = System.currentTimeMillis(),
            dueDate = dueDate
        )
        ref.child(taskId).setValue(task).await()
    }

    suspend fun setTaskAssignee(groupId: String, taskId: String, assigneeId: String?): Result<Unit> = runCatching {
        database.reference.child("groupTasks").child(groupId).child(taskId)
            .child("assignedToId")
            .setValue(assigneeId)
            .await()
    }

    suspend fun toggleTaskStatus(groupId: String, taskId: String): Result<Int> = runCatching {
        val statusRef = database.reference.child("groupTasks").child(groupId).child(taskId).child("isCompleted")
        suspendCancellableCoroutine { cont ->
            statusRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentStatus = currentData.getValue(Boolean::class.java) ?: false
                    val nextStatus = !currentStatus
                    currentData.value = nextStatus
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        if (cont.isActive) cont.resumeWithException(error.toException())
                        return
                    }

                    if (!committed) {
                        if (cont.isActive) cont.resume(0)
                        return
                    }

                    val isCompleted = currentData?.getValue(Boolean::class.java) ?: false
                    val delta = if (isCompleted) 1 else -1
                    if (cont.isActive) cont.resume(delta)
                }
            })
        }
    }

    suspend fun applyGroupTaskCountDelta(uid: String, delta: Int): Result<Int> = runCatching {
        if (delta == 0) return@runCatching -1
        val ref = database.reference.child("users").child(uid).child("groupTaskCount")
        suspendCancellableCoroutine<Int> { cont ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = (current + delta).coerceAtLeast(0)
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null || !committed) {
                        if (cont.isActive) cont.resume(-1)
                        return
                    }

                    val updated = currentData?.getValue(Int::class.java) ?: -1
                    if (cont.isActive) cont.resume(updated)
                }
            })
        }
    }

    suspend fun saveUnlockedAchievements(uid: String, unlocked: List<String>): Result<Unit> = runCatching {
        database.reference.child("users").child(uid)
            .child("unlockedAchievements")
            .setValue(unlocked)
            .await()
    }

    suspend fun deleteGroupTask(groupId: String, taskId: String): Result<Unit> = runCatching {
        database.reference.child("groupTasks").child(groupId).child(taskId).removeValue().await()
    }

    suspend fun restoreGroupTask(groupId: String, task: GroupTask): Result<Unit> = runCatching {
        val taskId = task.id.ifBlank { error("Task id không hợp lệ để khôi phục") }
        val restoredTask = task.copy(groupId = groupId)
        database.reference.child("groupTasks").child(groupId).child(taskId).setValue(restoredTask).await()
    }

    suspend fun leaveGroup(groupId: String, uid: String): Result<Unit> = runCatching {
        val membersRef = database.reference.child("groups").child(groupId).child("members")
        suspendCancellableCoroutine { cont ->
            membersRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val members = (currentData.value as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.toMutableList()
                        ?: mutableListOf()
                    members.remove(uid)
                    currentData.value = members
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        if (cont.isActive) cont.resumeWithException(error.toException())
                        return
                    }

                    if (!committed) {
                        if (cont.isActive) cont.resumeWithException(IllegalStateException("Rời nhóm thất bại"))
                        return
                    }
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
    }

    suspend fun deleteGroupAndTasks(groupId: String): Result<Unit> = runCatching {
        database.reference.child("groupTasks").child(groupId).removeValue().await()
        database.reference.child("groups").child(groupId).removeValue().await()
    }

    suspend fun getUserFcmToken(uid: String): String? {
        val snapshot = database.reference.child("users").child(uid).child("fcmToken").get().await()
        return snapshot.getValue(String::class.java)
    }
}
