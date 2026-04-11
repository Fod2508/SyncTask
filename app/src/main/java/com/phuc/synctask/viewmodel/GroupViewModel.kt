package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.phuc.synctask.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

sealed class GroupUiState {
    object Loading : GroupUiState()
    data class Success(val groups: List<Group>) : GroupUiState()
    object Empty : GroupUiState()
    data class Error(val message: String) : GroupUiState()
}

class GroupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val groupsRef = database.reference.child("groups")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private var valueEventListener: ValueEventListener? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            fetchUserGroups()
        } else {
            // User logged out
            _groups.value = emptyList()
            _uiState.value = GroupUiState.Error("Chưa đăng nhập!")
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun fetchUserGroups() {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _groups.value = emptyList()
            _uiState.value = GroupUiState.Error("Chưa đăng nhập!")
            return
        }

        _uiState.value = GroupUiState.Loading

        // Remove old listener if exists
        valueEventListener?.let { groupsRef.removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupList = mutableListOf<Group>()
                for (child in snapshot.children) {
                    val group = child.getValue(Group::class.java)
                    if (group != null && group.members.contains(uid)) {
                        groupList.add(group)
                    }
                }

                _groups.value = groupList

                _uiState.value = if (groupList.isEmpty()) {
                    GroupUiState.Empty
                } else {
                    GroupUiState.Success(groupList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = GroupUiState.Error(error.message)
            }
        }

        valueEventListener = listener
        groupsRef.addValueEventListener(listener)
    }

    fun createGroup(name: String, onResult: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập lại.")
            return
        }

        val inviteCode = generateInviteCode()
        val newGroupRef = groupsRef.push()
        val groupId = newGroupRef.key ?: return

        val newGroup = Group(
            id = groupId,
            name = name,
            inviteCode = inviteCode,
            ownerId = uid,
            members = listOf(uid)
        )

        newGroupRef.setValue(newGroup)
            .addOnSuccessListener {
                onResult(true, "Tạo nhóm thành công! Mã mời: $inviteCode")
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage ?: "Tạo nhóm thất bại.")
            }
    }

    fun joinGroup(inviteCode: String, onResult: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập lại.")
            return
        }

        groupsRef.orderByChild("inviteCode").equalTo(inviteCode).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val group = child.getValue(Group::class.java)
                        if (group != null) {
                            if (group.members.contains(uid)) {
                                onResult(false, "Bạn đã ở trong nhóm này rồi.")
                                return
                            }
                            
                            // Cập nhật members list
                            val updatedMembers = group.members.toMutableList()
                            updatedMembers.add(uid)
                            
                            child.ref.child("members").setValue(updatedMembers)
                                .addOnSuccessListener {
                                    onResult(true, "Tham gia nhóm thành công!")
                                }
                                .addOnFailureListener { e ->
                                    onResult(false, e.localizedMessage ?: "Lỗi khi cập nhật danh sách thành viên.")
                                }
                            return
                        }
                    }
                } else {
                    onResult(false, "Mã mời không chính xác hoặc nhóm không tồn tại.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false, error.message)
            }
        })
    }

    private fun generateInviteCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        valueEventListener?.let { listener ->
            groupsRef.removeEventListener(listener)
        }
    }
}
