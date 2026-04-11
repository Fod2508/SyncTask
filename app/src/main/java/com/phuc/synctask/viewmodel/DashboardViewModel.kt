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
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.model.quadrant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

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
    val isOverdue: Boolean
)

data class EisenhowerData(
    val totalCount: Int,
    val completedCount: Int
)

class DashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

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
        val now = System.currentTimeMillis()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val startTime: Long
        val endTime: Long
        val labels: List<String>

        if (filter == DashboardFilter.WEEK) {
            // Tuần bắt đầu từ Thứ Hai (chuẩn ISO)
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(7)
            startTime = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            endTime = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
            labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        } else {
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1)
            startTime = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
            endTime = monthEnd.atStartOfDay(zone).toInstant().toEpochMilli()
            labels = listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4", "Tuần 5")
        }

        // Filtering tasks within period — dùng createdAt (timestamp) làm fallback khi dueDate == null
        val inRangePersonal = pTasks.filter { (it.dueDate ?: it.timestamp) in startTime until endTime }
        val inRangeGroup = gTasksMap.values.flatten().filter {
            it.assignedToId == uid && (it.dueDate ?: it.timestamp) in startTime until endTime
        }

        // 1. Basic Stats
        _personalCompleted.value = inRangePersonal.count { it.isCompleted }
        _groupCompleted.value = inRangeGroup.count { it.isCompleted }
        
        // Overdue count is global (not just in range) - according to general dashboard best practice
        val allPersonalOverdue = pTasks.count { !it.isCompleted && it.dueDate != null && it.dueDate!! < now }
        val allGroupOverdue = gTasksMap.values.flatten().count { it.assignedToId == uid && !it.isCompleted && it.dueDate != null && it.dueDate!! < now }
        _overdueCount.value = allPersonalOverdue + allGroupOverdue

        // 2. Eisenhower (Personal only)
        var doNowTotal = 0; var doNowDone = 0;
        var planTotal = 0; var planDone = 0;
        var delegateTotal = 0; var delegateDone = 0;
        var eliminateTotal = 0; var eliminateDone = 0;

        inRangePersonal.forEach {
            val isDone = it.isCompleted
            when (it.quadrant()) {
                Quadrant.DO_NOW -> { doNowTotal++; if (isDone) doNowDone++ }
                Quadrant.PLAN -> { planTotal++; if (isDone) planDone++ }
                Quadrant.DELEGATE -> { delegateTotal++; if (isDone) delegateDone++ }
                Quadrant.ELIMINATE -> { eliminateTotal++; if (isDone) eliminateDone++ }
            }
        }
        _eisenhowerStats.value = listOf(
            EisenhowerData(doNowTotal, doNowDone),
            EisenhowerData(planTotal, planDone),
            EisenhowerData(delegateTotal, delegateDone),
            EisenhowerData(eliminateTotal, eliminateDone)
        )

        // 3. Workload Stats (Stacked Bar Chart Data)
        val workloadList = mutableListOf<DailyWorkload>()
        if (filter == DashboardFilter.WEEK) {
            // index 0 = Monday (dayOfWeek.value=1), index 6 = Sunday (dayOfWeek.value=7)
            for (i in 0..6) {
                val pDone = inRangePersonal.count {
                    it.isCompleted && getLocalDayIndex(it.dueDate ?: it.timestamp, zone) == i
                }
                val gDone = inRangeGroup.count {
                    it.isCompleted && getLocalDayIndex(it.dueDate ?: it.timestamp, zone) == i
                }
                val overdue = inRangePersonal.count {
                    !it.isCompleted && it.dueDate != null && it.dueDate!! < now &&
                        getLocalDayIndex(it.dueDate!!, zone) == i
                } + inRangeGroup.count {
                    !it.isCompleted && it.dueDate != null && it.dueDate!! < now &&
                        getLocalDayIndex(it.dueDate!!, zone) == i
                }
                workloadList.add(DailyWorkload(labels[i], pDone, gDone, overdue))
            }
        } else {
            // Simplify for Month: Show 4-5 weeks
            for (i in 0 until labels.size) {
                val weekStart = startTime + i * 7L * 24 * 3600 * 1000
                val weekEnd = weekStart + 7L * 24 * 3600 * 1000
                val pDone = inRangePersonal.count { it.isCompleted && (it.dueDate ?: it.timestamp) in weekStart until weekEnd }
                val gDone = inRangeGroup.count { it.isCompleted && (it.dueDate ?: it.timestamp) in weekStart until weekEnd }
                val overdue = inRangePersonal.count { !it.isCompleted && it.dueDate != null && it.dueDate!! in weekStart until weekEnd && it.dueDate!! < now } +
                              inRangeGroup.count { !it.isCompleted && it.dueDate != null && it.dueDate!! in weekStart until weekEnd && it.dueDate!! < now }
                workloadList.add(DailyWorkload(labels[i], pDone, gDone, overdue))
            }
        }
        _weeklyWorkload.value = workloadList.toList()

        // 4. Group Progress
        _groupProgress.value = groups.map { group ->
            val gTasks = gTasksMap[group.id] ?: emptyList()
            GroupProgressData(
                groupName = group.name,
                progress = if (gTasks.isNotEmpty()) gTasks.count { it.isCompleted }.toFloat() / gTasks.size else 0f,
                userContributions = gTasks.count { it.assignedToId == uid }
            )
        }

        // 5. Pending Focus Tasks (Top 3 priority)
        val pendingPersonal = pTasks.filter { !it.isCompleted }.map {
            FocusTask(it.id, it.title, "Cá nhân", getDeadlineStatus(it.dueDate, now), (it.dueDate ?: Long.MAX_VALUE) < now)
        }
        val pendingGroup = gTasksMap.values.flatten().filter { it.assignedToId == uid && !it.isCompleted }.map {
            val groupName = groups.find { g -> g.id == it.groupId }?.name ?: "Nhóm"
            FocusTask(it.id, it.title, groupName, getDeadlineStatus(it.dueDate, now), (it.dueDate ?: Long.MAX_VALUE) < now)
        }
        
        // Sort by overdue first, then by earliest due date
        _pendingFocusTasks.value = (pendingPersonal + pendingGroup)
            .sortedWith(compareBy({ !it.isOverdue }, { it.deadlineStatus })) // A bit naive but works for demonstration
            .take(3)

        _isLoading.value = false
    }

    /**
     * Trả về index 0-6 theo chuẩn ISO: Monday=0, Tuesday=1, ..., Sunday=6
     * Dùng LocalDate.dayOfWeek.value (Mon=1..Sun=7) rồi trừ 1.
     */
    private fun getLocalDayIndex(time: Long, zone: ZoneId): Int {
        val date = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
        return date.dayOfWeek.value - 1  // Mon=0 ... Sun=6
    }

    private fun getDeadlineStatus(dueDate: Long?, now: Long): String {
        if (dueDate == null) return "Không hạn chót"
        val diff = dueDate - now
        val days = TimeUnit.MILLISECONDS.toDays(Math.abs(diff))
        return when {
            diff < 0 -> "Quá hạn $days ngày"
            days == 0L -> "Hạn hôm nay"
            else -> "Còn $days ngày"
        }
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
