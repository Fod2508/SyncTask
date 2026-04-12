package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.UserProfile
import com.phuc.synctask.utils.AchievementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * Cấu trúc Firebase: /users/{uid}/tasks/{taskId} → FirebaseTask object
 */
class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    /** Hàm lấy reference tới danh sách task của user hiện tại */
    private fun getTasksRef() = auth.currentUser?.uid?.let { uid ->
        database.reference.child("tasks").child(uid)
    }

    private val _tasks = MutableStateFlow<List<FirebaseTask>>(emptyList())
    val tasks: StateFlow<List<FirebaseTask>> = _tasks.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Dialog mở khóa thành tựu — null = ẩn, non-null = hiện với achievementId
    private val _achievementUnlocked = MutableStateFlow<String?>(null)
    val achievementUnlocked: StateFlow<String?> = _achievementUnlocked.asStateFlow()

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
        val nowMillis = System.currentTimeMillis()
        taskList.count { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate!! < nowMillis
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Giữ reference để hủy listener trong onCleared() */
    private var valueEventListener: ValueEventListener? = null

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

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<FirebaseTask>()
                for (child in snapshot.children) {
                    val task = child.getValue(FirebaseTask::class.java)
                    if (task != null) {
                        task.id = child.key ?: ""
                        taskList.add(task)
                    }
                }

                // Sắp xếp theo timestamp mới nhất trước
                taskList.sortByDescending { it.timestamp }

                _tasks.value = taskList

                _uiState.value = if (taskList.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Success(taskList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = HomeUiState.Error(error.message)
            }
        }

        valueEventListener = listener
        getTasksRef()?.addValueEventListener(listener)
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

        val ref = getTasksRef() ?: return
        val taskId = ref.push().key ?: return
        val task = FirebaseTask(
            id = taskId,
            title = title,
            description = description,
            isCompleted = false,
            isUrgent = isUrgent,
            isImportant = isImportant,
            creatorId = uid,
            timestamp = System.currentTimeMillis(),
            dueDate = dueDate
        )

        ref.child(taskId).setValue(task)
            .addOnFailureListener { e ->
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Thêm task thất bại!")
            }
    }

    /**
     * Xoá một task khỏi Firebase theo taskId.
     */
    fun deleteTask(taskId: String) {
        getTasksRef()?.child(taskId)?.removeValue()
            ?.addOnFailureListener { e ->
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Xoá task thất bại!")
            }
    }

    /**
     * Tải profile người dùng (bao gồm danh sách thành tựu đã mở khóa).
     * Đường dẫn: /users/{uid}/
     */
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(UserProfile::class.java)?.let {
                        userProfile = it.copy(uid = uid)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /**
     * Đảo trạng thái hoàn thành (isCompleted) của một task.
     * Nếu task được đánh dấu hoàn thành → kiểm tra thành tựu.
     */
    fun toggleTaskStatus(task: FirebaseTask) {
        val newStatus = !task.isCompleted
        getTasksRef()?.child(task.id)?.child("isCompleted")?.setValue(newStatus)
            ?.addOnSuccessListener {
                if (newStatus) {
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
            ?.addOnFailureListener { e ->
                _uiState.value =
                    HomeUiState.Error(e.localizedMessage ?: "Cập nhật trạng thái thất bại!")
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
        database.reference.child("users").child(uid)
            .child("unlockedAchievements")
            .setValue(userProfile.unlockedAchievements)
        // Phát sự kiện ra UI — dùng StateFlow, set value trực tiếp
        _achievementUnlocked.value = achievementId
    }

    /**
     * Hủy listener khi ViewModel bị destroy để tránh memory leak.
     */
    override fun onCleared() {
        super.onCleared()
        valueEventListener?.let { listener ->
            getTasksRef()?.removeEventListener(listener)
        }
    }
}
