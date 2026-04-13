package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.data.repository.FirebaseGroupRepository
import com.phuc.synctask.data.repository.JoinGroupStatus
import com.phuc.synctask.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GroupUiState {
    object Loading : GroupUiState()
    data class Success(val groups: List<Group>) : GroupUiState()
    object Empty : GroupUiState()
    data class Error(val message: String) : GroupUiState()
}

class GroupViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseGroupRepository()
    private val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private var cancelGroupObservation: (() -> Unit)? = null

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
        cancelGroupObservation?.invoke()
        cancelGroupObservation = repository.observeUserGroups(
            uid = uid,
            onGroups = { groupList ->
                _groups.value = groupList
                _uiState.value = if (groupList.isEmpty()) {
                    GroupUiState.Empty
                } else {
                    GroupUiState.Success(groupList)
                }
            },
            onError = { message ->
                _uiState.value = GroupUiState.Error(message)
            }
        )
    }

    fun createGroup(name: String, onResult: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập lại.")
            return
        }

        viewModelScope.launch {
            repository.createGroup(uid, name)
                .onSuccess { inviteCode ->
                    notificationRepo.addNotification(uid, "Tạo nhóm thành công", "Bạn đã tạo nhóm $name. Mã mời: $inviteCode")
                    onResult(true, "Tạo nhóm thành công! Mã mời: $inviteCode")
                }
                .onFailure { e ->
                    onResult(false, e.localizedMessage ?: "Tạo nhóm thất bại.")
                }
        }
    }

    fun joinGroup(inviteCode: String, onResult: (Boolean, String) -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            onResult(false, "Vui lòng đăng nhập lại.")
            return
        }

        viewModelScope.launch {
            repository.joinGroup(uid, inviteCode.uppercase())
                .onSuccess { status ->
                    when (status) {
                        is JoinGroupStatus.Joined -> {
                            notificationRepo.addNotification(
                                status.ownerId, 
                                "Thành viên mới", 
                                "Một thành viên vừa tham gia nhóm ${status.groupName} của bạn qua mã mời"
                            )
                            onResult(true, "Tham gia nhóm thành công!")
                        }
                        is JoinGroupStatus.AlreadyMember -> onResult(false, "Bạn đã ở trong nhóm này rồi.")
                        is JoinGroupStatus.NotFound -> onResult(false, "Mã mời không chính xác hoặc nhóm không tồn tại.")
                    }
                }
                .onFailure { e ->
                    onResult(false, e.localizedMessage ?: "Tham gia nhóm thất bại.")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        cancelGroupObservation?.invoke()
        cancelGroupObservation = null
    }
}
