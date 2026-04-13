package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.data.repository.FirebaseHomeTaskRepository
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.UserProfile
import com.phuc.synctask.utils.AppSoundEffect
import com.phuc.synctask.utils.AchievementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Sealed class biểu diễn các trạng thái UI của màn hình Home.
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val tasks: List<FirebaseTask>) : HomeUiState()
    object Empty : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

/**
 * ViewModel quản lý logic nghiệp vụ cho màn hình Home (danh sách Task).
 * Cấu trúc Firebase: /tasks/{uid}/{taskId} → FirebaseTask object
 */
class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseHomeTaskRepository()

    private val _tasks = MutableStateFlow<List<FirebaseTask>>(emptyList())
    val tasks: StateFlow<List<FirebaseTask>> = _tasks.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Dialog mở khóa thành tựu — null = ẩn, non-null = hiện với achievementId
    private val _achievementUnlocked = MutableStateFlow<String?>(null)
    val achievementUnlocked: StateFlow<String?> = _achievementUnlocked.asStateFlow()

    private val _soundEvent = MutableSharedFlow<AppSoundEffect>(extraBufferCapacity = 8)
    val soundEvent: SharedFlow<AppSoundEffect> = _soundEvent.asSharedFlow()

    fun dismissAchievementDialog() { _achievementUnlocked.value = null }

    // Profile người dùng (chứa danh sách thành tựu đã mở)
    private var userProfile = UserProfile()

    val completedTasksCount: StateFlow<Int> = _tasks.map { taskList ->
        taskList.count { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTasksCount: StateFlow<Int> = _tasks.map { taskList ->
        val todayCalendar = Calendar.getInstance()
        val currentYear = todayCalendar.get(Calendar.YEAR)
        val currentDayOfYear = todayCalendar.get(Calendar.DAY_OF_YEAR)

        taskList.count { task ->
            task.dueDate?.let { dueDateMillis ->
                val taskCalendar = Calendar.getInstance()
                taskCalendar.timeInMillis = dueDateMillis
                taskCalendar.get(Calendar.YEAR) == currentYear &&
                        taskCalendar.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
            } ?: false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overdueTasksCount: StateFlow<Int> = _tasks.map { taskList ->
        val todayCalendar = Calendar.getInstance()
        todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        todayCalendar.set(Calendar.MINUTE, 0)
        todayCalendar.set(Calendar.SECOND, 0)
        todayCalendar.set(Calendar.MILLISECOND, 0)
        val startOfTodayMillis = todayCalendar.timeInMillis

        taskList.count { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate!! < startOfTodayMillis
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Hàm hủy đăng ký lắng nghe tasks */
    private var cancelTasksObservation: (() -> Unit)? = null

    init {
        listenToTasks()
        loadUserProfile()
    }

    /**
     * Lắng nghe real-time danh sách tasks từ Firebase Realtime Database
     * bằng addValueEventListener.
     */
    private fun listenToTasks() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _tasks.value = emptyList() // clear data khi log out
            _uiState.value = HomeUiState.Error("Chưa đăng nhập!")
            return
        }

        _uiState.value = HomeUiState.Loading
        cancelTasksObservation?.invoke()
        cancelTasksObservation = repository.observeTasks(
            uid = uid,
            onTasks = { taskList ->
                _tasks.value = taskList
                _uiState.value = if (taskList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(taskList)
                }
            },
            onError = { message ->
                _uiState.value = HomeUiState.Error(message)
            }
        )
    }

    /**
     * Thêm một task mới vào Firebase.
     * Dùng push() để Firebase tự sinh ID duy nhất.
     */
    fun addTask(
        title: String,
        description: String,
        isUrgent: Boolean,
        isImportant: Boolean,
        dueDate: Long?
    ) {
        val uid = auth.currentUser?.uid ?: return
        val task = FirebaseTask(
            id = "",
            title = title,
            description = description,
            isCompleted = false,
            isUrgent = isUrgent,
            isImportant = isImportant,
            creatorId = uid,
            timestamp = System.currentTimeMillis(),
            dueDate = dueDate
        )

        viewModelScope.launch {
            repository.addTask(uid, task)
                .onSuccess {
                    _soundEvent.tryEmit(AppSoundEffect.TASK_CREATED)
                    val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
                    notificationRepo.addNotification(uid, "Khởi tạo thành công", "Bạn vừa tạo mới tác vụ: ${task.title}")
                }
                .onFailure { e ->
                    _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Thêm task thất bại!")
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    /**
     * Xoá một task khỏi Firebase theo taskId.
     */
    fun deleteTask(taskId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteTask(uid, taskId)
                .onSuccess {
                    _soundEvent.tryEmit(AppSoundEffect.TASK_DELETED)
                }
                .onFailure { e ->
                    _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Xoá task thất bại!")
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    /**
     * Khôi phục một task đã xóa bằng cách nạp lại nguyên ID cũ.
     */
    fun restoreTask(task: FirebaseTask) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.restoreTask(uid, task)
                .onSuccess {
                    _soundEvent.tryEmit(AppSoundEffect.TASK_RESTORED)
                }
                .onFailure { e ->
                    _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Khôi phục task thất bại!")
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
        }
    }

    /**
     * Tải profile người dùng (bao gồm danh sách thành tựu đã mở khóa).
     * Đường dẫn: /users/{uid}/
     */
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.loadUserProfile(uid)?.let {
                userProfile = it
            }
        }
    }

    /**
     * Đảo trạng thái hoàn thành (isCompleted) của một task.
     * Nếu task được đánh dấu hoàn thành → kiểm tra thành tựu.
     */
    fun toggleTaskStatus(task: FirebaseTask) {
        val newStatus = !task.isCompleted
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.updateTaskCompleted(uid, task.id, newStatus)
                .onSuccess {
                    if (newStatus) {
                        val completedAt = System.currentTimeMillis()
                        val isOnTime = task.dueDate == null || completedAt <= task.dueDate!!
                        val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
                        if (isOnTime) {
                            _soundEvent.tryEmit(AppSoundEffect.TASK_COMPLETED_ON_TIME)
                            notificationRepo.addNotification(uid, "Tuyệt vời!", "Bạn đã hoàn thành tác vụ '${task.title}' đúng hạn!")
                        } else {
                            _soundEvent.tryEmit(AppSoundEffect.TASK_COMPLETED_LATE)
                            notificationRepo.addNotification(uid, "Chia buồn!", "Tác vụ '${task.title}' đã bị trễ hạn mất rồi! Rút kinh nghiệm lần sau nhé!")
                        }

                        // Đếm số task đã hoàn thành sau khi toggle
                        val completedCount = _tasks.value.count { it.isCompleted } +
                            if (!task.isCompleted) 1 else 0

                        AchievementManager.checkAndUnlock(
                            completedCount = completedCount,
                            dueDateMillis  = task.dueDate,
                            profile        = userProfile
                        ) { achievementId ->
                            unlockAchievement(achievementId)
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.value =
                        HomeUiState.Error(e.localizedMessage ?: "Cập nhật trạng thái thất bại!")
                    _soundEvent.tryEmit(AppSoundEffect.ERROR)
                }
            }
    }

    /**
     * Lưu thành tựu vào /users/{uid}/unlockedAchievements và phát sự kiện ra UI.
     */
    private fun unlockAchievement(achievementId: String) {
        val uid = auth.currentUser?.uid ?: return
        // Cập nhật local profile ngay để tránh unlock trùng trong cùng session
        userProfile = userProfile.copy(
            unlockedAchievements = userProfile.unlockedAchievements + achievementId
        )
        // Ghi lên Firebase
        viewModelScope.launch {
            repository.saveUnlockedAchievements(uid, userProfile.unlockedAchievements)
                .onFailure { e ->
                    _uiState.value = HomeUiState.Error(
                        e.localizedMessage ?: "Lưu thành tựu thất bại"
                    )
                }
            
            val notificationRepo = com.phuc.synctask.data.repository.FirebaseNotificationRepository()
            val achievementName = com.phuc.synctask.utils.AchievementManager.getAchievementName(achievementId)
            notificationRepo.addNotification(uid, "Thành tựu mới!", "Chà, bạn vừa đạt được danh hiệu: $achievementName!")
        }
        // Phát sự kiện ra UI — dùng StateFlow, set value trực tiếp
        _achievementUnlocked.value = achievementId
    }

    /**
     * Hủy listener khi ViewModel bị destroy để tránh memory leak.
     */
    override fun onCleared() {
        super.onCleared()
        cancelTasksObservation?.invoke()
        cancelTasksObservation = null
    }
}
