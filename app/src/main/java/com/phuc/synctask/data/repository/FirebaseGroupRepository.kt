package com.phuc.synctask.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.phuc.synctask.model.Group
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class FirebaseGroupRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val groupsRef = database.reference.child("groups")

    fun observeUserGroups(
        uid: String,
        onGroups: (List<Group>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children
                    .mapNotNull { it.getValue<Group>() }
                    .filter { uid in it.members }
                onGroups(groups)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        groupsRef.addValueEventListener(listener)
        return { groupsRef.removeEventListener(listener) }
    }

    suspend fun createGroup(uid: String, name: String): Result<String> = runCatching {
        val inviteCode = generateInviteCode()
        val newGroupRef = groupsRef.push()
        val groupId = newGroupRef.key ?: error("Không tạo được groupId")

        val group = Group(
            id = groupId,
            name = name,
            inviteCode = inviteCode,
            ownerId = uid,
            members = listOf(uid)
        )

        newGroupRef.setValue(group).await()
        inviteCode
    }

    suspend fun joinGroup(uid: String, inviteCode: String): Result<JoinGroupStatus> = runCatching {
        val snapshot = groupsRef.orderByChild("inviteCode").equalTo(inviteCode).get().await()
        val groupNode = snapshot.children.firstOrNull() ?: return@runCatching JoinGroupStatus.NotFound
        val group = groupNode.getValue<Group>() ?: return@runCatching JoinGroupStatus.NotFound

        val committed = suspendCancellableCoroutine<Boolean> { cont ->
            groupNode.ref.child("members").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val members = (currentData.value as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.toMutableList()
                        ?: mutableListOf()

                    if (uid in members) {
                        return Transaction.abort()
                    }

                    members.add(uid)
                    currentData.value = members
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        if (cont.isActive) cont.resume(false)
                        return
                    }
                    if (cont.isActive) cont.resume(committed)
                }
            })
        }

        if (committed) JoinGroupStatus.Joined(group.name, group.ownerId) else JoinGroupStatus.AlreadyMember
    }

    private fun generateInviteCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

sealed class JoinGroupStatus {
    data class Joined(val groupName: String, val ownerId: String) : JoinGroupStatus()
    object AlreadyMember : JoinGroupStatus()
    object NotFound : JoinGroupStatus()
}
