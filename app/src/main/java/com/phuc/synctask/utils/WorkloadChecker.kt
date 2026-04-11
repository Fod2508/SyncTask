package com.phuc.synctask.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.phuc.synctask.data.TaskDao
import com.phuc.synctask.model.Task

/**
 * Utility class xử lý logic kiểm tra tải công việc (Workload Analytics).
 */
object WorkloadChecker {

    private const val EFFORT_THRESHOLD = 7

    /**
     * Kiểm tra tải công việc sau khi insert task mới.
     */
    fun checkAndWarn(context: Context, taskDao: TaskDao, task: Task): Boolean {
        val totalEffort = taskDao.getTotalEffortByDate(task.dueDate ?: return false)

        if (totalEffort >= EFFORT_THRESHOLD) {
            val message = "⚠️ Quá tải trọng số! Ngày ${task.dueDate} " +
                    "đã có tổng effort là $totalEffort. Vui lòng sắp xếp lại lịch trình."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    /**
     * Lấy tổng effort của một ngày.
     */
    fun getTotalEffort(taskDao: TaskDao, date: String): Int {
        return taskDao.getTotalEffortByDate(date)
    }
}
