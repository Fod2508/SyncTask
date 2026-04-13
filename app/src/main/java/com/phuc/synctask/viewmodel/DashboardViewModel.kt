package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Group
import com.phuc.synctask.model.GroupTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

enum class DashboardFilter {
    WEEK, MONTH
}

data class DailyWorkload(
    val label: String,
    val personalCount: Int,
    val groupCount: Int,
    val overdueCount: Int = 0
)

data class GroupProgressData(
    val groupName: String,
    val progress: Float,
    val userContributions: Int
)

data class FocusTask(
    val id: String,
    val title: String,
    val origin: String,
    val deadlineStatus: String,
    val isOverdue: Boolean,
    val dueDateMillis: Long?
)

data class EisenhowerData(
    val totalCount: Int,
    val completedCount: Int
)

class DashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val analyticsUseCase = DashboardAnalyticsUseCase()

    private val _personalCompleted = MutableStateFlow(0)
    val personalCompleted: StateFlow<Int> = _personalCompleted.asStateFlow()

    private val _groupCompleted = MutableStateFlow(0)
    val groupCompleted: StateFlow<Int> = _groupCompleted.asStateFlow()

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount.asStateFlow()

    private val _eisenhowerStats = MutableStateFlow(List(4) { EisenhowerData(0, 0) })
    val eisenhowerStats: StateFlow<List<EisenhowerData>> = _eisenhowerStats.asStateFlow()

    private val _weeklyWorkload = MutableStateFlow<List<DailyWorkload>>(emptyList())
    val weeklyWorkload: StateFlow<List<DailyWorkload>> = _weeklyWorkload.asStateFlow()

    private val _groupProgress = MutableStateFlow<List<GroupProgressData>>(emptyList())
    val groupProgress: StateFlow<List<GroupProgressData>> = _groupProgress.asStateFlow()

    private val _pendingFocusTasks = MutableStateFlow<List<FocusTask>>(emptyList())
    val pendingFocusTasks: StateFlow<List<FocusTask>> = _pendingFocusTasks.asStateFlow()

    private val _filterType = MutableStateFlow(DashboardFilter.WEEK)
    val filterType: StateFlow<DashboardFilter> = _filterType.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _rawPersonalTasks = MutableStateFlow<List<FirebaseTask>>(emptyList())
    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    private val _rawGroupTasks = MutableStateFlow<Map<String, List<GroupTask>>>(emptyMap())

    private var personalListener: ValueEventListener? = null
    private var groupsListener: ValueEventListener? = null
    private val groupTasksListeners = mutableMapOf<String, ValueEventListener>()

    init {
        startListening()

        combine(_rawPersonalTasks, _userGroups, _rawGroupTasks, _filterType) { pTasks, groups, gTasksMap, filter ->
            recalculateDashboardData(pTasks, groups, gTasksMap, filter)
        }.launchIn(viewModelScope)
    }

    fun setFilter(filter: DashboardFilter) {
        _filterType.value = filter
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        val pRef = database.reference.child("tasks").child(uid)
        personalListener = pRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _rawPersonalTasks.value = snapshot.children.mapNotNull { it.getValue(FirebaseTask::class.java) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val gRef = database.reference.child("groups")
        groupsListener = gRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { it.getValue(Group::class.java) }
                    .filter { it.members.contains(uid) }
                _userGroups.value = groups

                val currentGroupIds = groups.map { it.id }.toSet()
                val existingListeners = groupTasksListeners.keys.toSet()

                (existingListeners - currentGroupIds).forEach { id ->
                    groupTasksListeners.remove(id)?.let {
                        database.reference.child("groupTasks").child(id).removeEventListener(it)
                    }
                }

                (currentGroupIds - existingListeners).forEach { id ->
                    val listener = object : ValueEventListener {
                        override fun onDataChange(snap: DataSnapshot) {
                            val gTasks = snap.children.mapNotNull { it.getValue(GroupTask::class.java) }
                            val currentMap = _rawGroupTasks.value.toMutableMap()
                            currentMap[id] = gTasks
                            _rawGroupTasks.value = currentMap
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    }
                    groupTasksListeners[id] = listener
                    database.reference.child("groupTasks").child(id).addValueEventListener(listener)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun recalculateDashboardData(
        pTasks: List<FirebaseTask>,
        groups: List<Group>,
        gTasksMap: Map<String, List<GroupTask>>,
        filter: DashboardFilter
    ) {
        val uid = auth.currentUser?.uid ?: return
        val result = analyticsUseCase.compute(
            uid = uid,
            personalTasks = pTasks,
            groups = groups,
            groupTasksMap = gTasksMap,
            filter = filter
        )

        _personalCompleted.value = result.personalCompleted
        _groupCompleted.value = result.groupCompleted
        _overdueCount.value = result.overdueCount
        _eisenhowerStats.value = result.eisenhowerStats
        _weeklyWorkload.value = result.workload
        _groupProgress.value = result.groupProgress
        _pendingFocusTasks.value = result.pendingFocusTasks

        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        val uid = auth.currentUser?.uid
        if (uid != null) personalListener?.let { database.reference.child("tasks").child(uid).removeEventListener(it) }
        groupsListener?.let { database.reference.child("groups").removeEventListener(it) }
        groupTasksListeners.forEach { (id, listener) ->
            database.reference.child("groupTasks").child(id).removeEventListener(listener)
        }
    }
}
