package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.data.repository.FirebaseGroupTaskRepository
import com.phuc.synctask.model.Group
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.model.UserProfile
import com.phuc.synctask.utils.AppSoundEffect
import com.phuc.synctask.utils.AchievementManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý dữ liệu cho một nhóm cụ thể (group tasks, members, group info).
 */
class GroupTaskViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseGroupTaskRepository()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Group Info
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    // Members display names: uid -> displayName
    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames: StateFlow<Map<String, String>> = _memberNames.asStateFlow()

    // Group Tasks
    private val _tasks = MutableStateFlow<List<GroupTask>>(emptyList())
    val tasks: StateFlow<List<GroupTask>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Achievement dialog — null = ẩn, non-null = hiện với achievementId
    private val _achievementUnlocked = MutableStateFlow<String?>(null)
    val achievementUnlocked: StateFlow<String?> = _achievementUnlocked.asStateFlow()

    private val _soundEvent = MutableSharedFlow<AppSoundEffect>(extraBufferCapacity = 8)
    val soundEvent: SharedFlow<AppSoundEffect> = _soundEvent.asSharedFlow()

    fun dismissAchievementDialog() { _achievementUnlocked.value = null }

    // Profile người dùng (chứa danh sách thành tựu + groupTaskCount)
    private var userProfile = UserProfile()

    // Đảm bảo branded loading hiển thị tối thiểu 1500ms
    private var loadStartTime = 0L

    private var cancelGroupObservation: (() -> Unit)? = null
    private var cancelTaskObservation: (() -> Unit)? = null
    private var currentGroupId: String? = null

    fun loadGroup(groupId: String) {
        if (groupId == currentGroupId) return
        currentGroupId = groupId
        loadStartTime = System.currentTimeMillis()

        cleanup()
        loadUserProfile()
        listenToGroupInfo(groupId)
        listenToGroupTasks(groupId)
    }

    private fun loadUserProfile() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.loadUserProfile(uid)?.let {
                userProfile = it
            }
        }
    }

    private fun listenToGroupInfo(groupId: String) {
        cancelGroupObservation?.invoke()
        cancelGroupObservation = repository.observeGroupInfo(
            groupId = groupId,
            onGroup = { g ->
                _group.value = g
                viewModelScope.launch {
                    val memberNames = repository.fetchMemberNames(g?.members ?: emptyList())
                    _memberNames.value = memberNames
                }
            },
            onError = {}
        )
    }

    private fun listenToGroupTasks(groupId: String) {
        cancelTaskObservation?.invoke()
        cancelTaskObservation = repository.observeGroupTasks(
            groupId = groupId,
            onTasks = { taskList ->
                _tasks.value = taskList
                // Branded loading: đảm bảo hiển thị tối thiểu 1500ms
                viewModelScope.launch {
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    val remaining = 1500L - elapsed
                    if (remaining > 0) delay(remaining)
                    _isLoading.value = false
                }
            },
            onError = {
                _isLoading.value = false
            }
        )
    }

    fun addGroupTask(groupId: String, title: String, description: String, dueDate: Long?, assignedToId: String? = null) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.addGroupTask(groupId, uid, title, description, dueDate, assignedToId)
                .onSuccess {
                    _soundEvent.tryEmit(AppSoundEffect.TASK_CREATED)
                    val groupName = _group.value?.name ?: "nhóm"
                    val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
                    notificationRepo.addNotification(uid, "Task mới tạo xong!", "Tác vụ '$title' trong nhóm '$groupName' đã được khởi tạo thành công.")
                    if (assignedToId != null && assignedToId != uid) {
                        sendNotificationToUser(assignedToId, title)
                    }
                }
                .onFailure {
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    fun claimTask(groupId: String, taskId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.setTaskAssignee(groupId, taskId, uid)
                .onSuccess { _soundEvent.tryEmit(AppSoundEffect.TASK_ASSIGNED) }
                .onFailure { _soundEvent.tryEmit(AppSoundEffect.ERROR) }
        }
    }

    fun assignTask(groupId: String, taskId: String, taskTitle: String, assignedToId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.setTaskAssignee(groupId, taskId, assignedToId)
                .onSuccess {
                    _soundEvent.tryEmit(AppSoundEffect.TASK_ASSIGNED)
                    if (assignedToId != uid) {
                        sendNotificationToUser(assignedToId, taskTitle)
                    }
                }
                .onFailure {
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    private fun sendNotificationToUser(assignedToId: String, taskTitle: String) {
        val currentUid = currentUserId ?: return
        val currentUserName = _memberNames.value[currentUid] ?: "Ai đó"
        val groupName = _group.value?.name ?: "nhóm"

        viewModelScope.launch {
            val title = "Có công việc mới từ nhóm $groupName"
            val body = "$currentUserName vừa phân công cho bạn task: $taskTitle"
            
            val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
            notificationRepo.addNotification(assignedToId, title, body)

            val fcmToken = repository.getUserFcmToken(assignedToId)
            if (!fcmToken.isNullOrEmpty()) {
                com.phuc.synctask.util.NotificationHelper.sendPushNotification(fcmToken, title, body)
            }
        }
    }

    fun toggleTaskStatus(groupId: String, task: GroupTask) {
        val uid = currentUserId ?: return
        val isOwner = uid == _group.value?.ownerId
        viewModelScope.launch {
            repository.toggleTaskStatus(groupId, task.id)
                .onSuccess { delta ->
                    if (delta == 0) return@onSuccess

                    if (delta > 0) {
                        val completedAt = System.currentTimeMillis()
                        val isOnTime = task.dueDate == null || completedAt <= task.dueDate!!
                        val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
                        if (isOnTime) {
                            _soundEvent.tryEmit(AppSoundEffect.TASK_COMPLETED_ON_TIME)
                            notificationRepo.addNotification(uid, "Tuyệt vời!", "Bạn đã hoàn thành task nhóm '${task.title}' đúng hạn! Keep it up!")
                        } else {
                            _soundEvent.tryEmit(AppSoundEffect.TASK_COMPLETED_LATE)
                            notificationRepo.addNotification(uid, "Chia buồn!", "Task '${task.title}' đã bị trễ hạn mất rồi! Rút kinh nghiệm lần sau nhé!")
                        }
                    }

                    updateGroupTaskCount(uid, delta)

                    if (delta > 0) {
                        AchievementManager.checkAndUnlock(
                            completedCount = 0,
                            dueDateMillis = task.dueDate,
                            profile = userProfile,
                            isGroupTask = true,
                            isOwner = isOwner
                        ) { achievementId ->
                            unlockAchievement(uid, achievementId)
                        }
                    }
                }
                .onFailure {
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    private fun updateGroupTaskCount(uid: String, delta: Int) {
        viewModelScope.launch {
            repository.applyGroupTaskCountDelta(uid, delta)
                .onSuccess { updated ->
                    if (updated >= 0) {
                        userProfile = userProfile.copy(groupTaskCount = updated)
                    }
                }
        }
    }

    private fun unlockAchievement(uid: String, achievementId: String) {
        // Tránh unlock trùng trong cùng session
        if (achievementId in userProfile.unlockedAchievements) return
        userProfile = userProfile.copy(
            unlockedAchievements = userProfile.unlockedAchievements + achievementId
        )
        viewModelScope.launch {
            repository.saveUnlockedAchievements(uid, userProfile.unlockedAchievements)
            val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
            val achievementName = com.phuc.synctask.utils.AchievementManager.getAchievementName(achievementId)
            notificationRepo.addNotification(uid, "Thành tựu mới!", "Chà, bạn vừa đạt được danh hiệu: $achievementName!")
        }
        _achievementUnlocked.value = achievementId
    }

    fun deleteGroupTask(groupId: String, taskId: String) {
        viewModelScope.launch {
            repository.deleteGroupTask(groupId, taskId)
                .onSuccess { _soundEvent.tryEmit(AppSoundEffect.TASK_DELETED) }
                .onFailure { _soundEvent.tryEmit(AppSoundEffect.ERROR) }
        }
    }

    fun restoreGroupTask(groupId: String, task: GroupTask) {
        viewModelScope.launch {
            repository.restoreGroupTask(groupId, task)
                .onSuccess { _soundEvent.tryEmit(AppSoundEffect.TASK_RESTORED) }
                .onFailure { _soundEvent.tryEmit(AppSoundEffect.ERROR) }
        }
    }

    /**
     * Owner → xóa toàn bộ group + groupTasks.
     * Member → chỉ xóa uid khỏi mảng members.
     * [onComplete] được gọi khi xong để UI điều hướng về.
     */
    fun leaveOrDeleteGroup(groupId: String, onComplete: () -> Unit) {
        val uid = currentUserId ?: return
        val isOwner = uid == _group.value?.ownerId

        viewModelScope.launch {
            val result = if (isOwner) {
                repository.deleteGroupAndTasks(groupId)
            } else {
                repository.leaveGroup(groupId, uid)
            }

            if (result.isSuccess) {
                onComplete()
            }
        }
    }

    private fun cleanup() {
        cancelGroupObservation?.invoke()
        cancelTaskObservation?.invoke()
        cancelGroupObservation = null
        cancelTaskObservation = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
