package com.phuc.synctask.viewmodel

import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Group
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.model.quadrant
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class DashboardComputationResult(
    val personalCompleted: Int,
    val groupCompleted: Int,
    val overdueCount: Int,
    val eisenhowerStats: List<EisenhowerData>,
    val workload: List<DailyWorkload>,
    val groupProgress: List<GroupProgressData>,
    val pendingFocusTasks: List<FocusTask>
)

class DashboardAnalyticsUseCase {

    fun compute(
        uid: String,
        personalTasks: List<FirebaseTask>,
        groups: List<Group>,
        groupTasksMap: Map<String, List<GroupTask>>,
        filter: DashboardFilter,
        now: Long = System.currentTimeMillis()
    ): DashboardComputationResult {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val (startTime, endTime, labels) = if (filter == DashboardFilter.WEEK) {
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(7)
            Triple(
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                weekEnd.atStartOfDay(zone).toInstant().toEpochMilli(),
                listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            )
        } else {
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = monthStart.plusMonths(1)
            Triple(
                monthStart.atStartOfDay(zone).toInstant().toEpochMilli(),
                monthEnd.atStartOfDay(zone).toInstant().toEpochMilli(),
                listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4", "Tuần 5")
            )
        }

        val allGroupTasks = groupTasksMap.values.flatten()
        val assignedGroupTasks = allGroupTasks.filter { it.assignedToId == uid }

        val inRangePersonal = personalTasks.filter { (it.dueDate ?: it.timestamp) in startTime until endTime }
        val inRangeGroup = assignedGroupTasks.filter { (it.dueDate ?: it.timestamp) in startTime until endTime }

        val personalCompleted = inRangePersonal.count { it.isCompleted }
        val groupCompleted = inRangeGroup.count { it.isCompleted }

        val overdueCount = personalTasks.count {
            val dueDate = it.dueDate
            !it.isCompleted && dueDate != null && dueDate < now
        } + assignedGroupTasks.count {
            val dueDate = it.dueDate
            !it.isCompleted && dueDate != null && dueDate < now
        }

        val eisenhowerStats = buildEisenhowerStats(inRangePersonal)
        val workload = buildWorkload(filter, labels, inRangePersonal, inRangeGroup, startTime, now, zone)
        val groupProgress = buildGroupProgress(groups, groupTasksMap, uid)
        val pendingFocusTasks = buildPendingFocusTasks(personalTasks, assignedGroupTasks, groups, now)

        return DashboardComputationResult(
            personalCompleted = personalCompleted,
            groupCompleted = groupCompleted,
            overdueCount = overdueCount,
            eisenhowerStats = eisenhowerStats,
            workload = workload,
            groupProgress = groupProgress,
            pendingFocusTasks = pendingFocusTasks
        )
    }

    private fun buildEisenhowerStats(tasks: List<FirebaseTask>): List<EisenhowerData> {
        var doNowTotal = 0
        var doNowDone = 0
        var planTotal = 0
        var planDone = 0
        var delegateTotal = 0
        var delegateDone = 0
        var eliminateTotal = 0
        var eliminateDone = 0

        tasks.forEach { task ->
            val done = task.isCompleted
            when (task.quadrant()) {
                Quadrant.DO_NOW -> {
                    doNowTotal++
                    if (done) doNowDone++
                }

                Quadrant.PLAN -> {
                    planTotal++
                    if (done) planDone++
                }

                Quadrant.DELEGATE -> {
                    delegateTotal++
                    if (done) delegateDone++
                }

                Quadrant.ELIMINATE -> {
                    eliminateTotal++
                    if (done) eliminateDone++
                }
            }
        }

        return listOf(
            EisenhowerData(doNowTotal, doNowDone),
            EisenhowerData(planTotal, planDone),
            EisenhowerData(delegateTotal, delegateDone),
            EisenhowerData(eliminateTotal, eliminateDone)
        )
    }

    private fun buildWorkload(
        filter: DashboardFilter,
        labels: List<String>,
        inRangePersonal: List<FirebaseTask>,
        inRangeGroup: List<GroupTask>,
        startTime: Long,
        now: Long,
        zone: ZoneId
    ): List<DailyWorkload> {
        val workload = mutableListOf<DailyWorkload>()

        if (filter == DashboardFilter.WEEK) {
            for (i in 0..6) {
                val pDone = inRangePersonal.count {
                    it.isCompleted && getLocalDayIndex(it.dueDate ?: it.timestamp, zone) == i
                }
                val gDone = inRangeGroup.count {
                    it.isCompleted && getLocalDayIndex(it.dueDate ?: it.timestamp, zone) == i
                }
                val overdue = inRangePersonal.count {
                    val dueDate = it.dueDate
                    !it.isCompleted && dueDate != null && dueDate < now &&
                        getLocalDayIndex(dueDate, zone) == i
                } + inRangeGroup.count {
                    val dueDate = it.dueDate
                    !it.isCompleted && dueDate != null && dueDate < now &&
                        getLocalDayIndex(dueDate, zone) == i
                }
                workload.add(DailyWorkload(labels[i], pDone, gDone, overdue))
            }
            return workload
        }

        for (i in labels.indices) {
            val weekStart = startTime + i * 7L * 24 * 3600 * 1000
            val weekEnd = weekStart + 7L * 24 * 3600 * 1000
            val pDone = inRangePersonal.count { it.isCompleted && (it.dueDate ?: it.timestamp) in weekStart until weekEnd }
            val gDone = inRangeGroup.count { it.isCompleted && (it.dueDate ?: it.timestamp) in weekStart until weekEnd }
            val overdue = inRangePersonal.count {
                val dueDate = it.dueDate
                !it.isCompleted && dueDate != null && dueDate in weekStart until weekEnd && dueDate < now
            } + inRangeGroup.count {
                val dueDate = it.dueDate
                !it.isCompleted && dueDate != null && dueDate in weekStart until weekEnd && dueDate < now
            }
            workload.add(DailyWorkload(labels[i], pDone, gDone, overdue))
        }

        return workload
    }

    private fun buildGroupProgress(
        groups: List<Group>,
        groupTasksMap: Map<String, List<GroupTask>>,
        uid: String
    ): List<GroupProgressData> {
        return groups.map { group ->
            val tasks = groupTasksMap[group.id] ?: emptyList()
            GroupProgressData(
                groupName = group.name,
                progress = if (tasks.isNotEmpty()) tasks.count { it.isCompleted }.toFloat() / tasks.size else 0f,
                userContributions = tasks.count { it.assignedToId == uid }
            )
        }
    }

    private fun buildPendingFocusTasks(
        personalTasks: List<FirebaseTask>,
        assignedGroupTasks: List<GroupTask>,
        groups: List<Group>,
        now: Long
    ): List<FocusTask> {
        val groupNames = groups.associateBy({ it.id }, { it.name })

        val pendingPersonal = personalTasks.filter { !it.isCompleted }.map {
            FocusTask(
                id = it.id,
                title = it.title,
                origin = "Cá nhân",
                deadlineStatus = getDeadlineStatus(it.dueDate, now),
                isOverdue = (it.dueDate ?: Long.MAX_VALUE) < now,
                dueDateMillis = it.dueDate
            )
        }

        val pendingGroup = assignedGroupTasks.filter { !it.isCompleted }.map {
            FocusTask(
                id = it.id,
                title = it.title,
                origin = groupNames[it.groupId] ?: "Nhóm",
                deadlineStatus = getDeadlineStatus(it.dueDate, now),
                isOverdue = (it.dueDate ?: Long.MAX_VALUE) < now,
                dueDateMillis = it.dueDate
            )
        }

        return (pendingPersonal + pendingGroup)
            .sortedWith(
                compareBy<FocusTask> { !it.isOverdue }
                    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            .take(3)
    }

    private fun getLocalDayIndex(time: Long, zone: ZoneId): Int {
        val date = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
        return date.dayOfWeek.value - 1
    }

    private fun getDeadlineStatus(dueDate: Long?, now: Long): String {
        if (dueDate == null) return "Không hạn chót"
        val diff = dueDate - now
        val days = TimeUnit.MILLISECONDS.toDays(abs(diff))
        return when {
            diff < 0 -> "Quá hạn $days ngày"
            days == 0L -> "Hạn hôm nay"
            else -> "Còn $days ngày"
        }
    }
}
