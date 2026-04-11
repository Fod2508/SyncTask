package com.phuc.synctask.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.phuc.synctask.model.Task

/**
 * Data Access Object (DAO) cho bảng "tasks".
 */
@Dao
interface TaskDao {

    // ========================
    // CRUD cơ bản
    // ========================

    @Insert
    fun insertTask(task: Task): Long

    @Insert
    fun insertAllTasks(tasks: List<Task>): LongArray

    @Update
    fun updateTask(task: Task)

    @Delete
    fun deleteTask(task: Task)

    // ================================================================
    // LiveData Query — Phục vụ giao diện Compose
    // ================================================================

    @Query(
        """SELECT * FROM tasks WHERE taskType = 'Personal' 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getAllPersonalTasks(): LiveData<List<Task>>

    @Query(
        """SELECT * FROM tasks WHERE taskType = 'Group' 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getAllGroupTasks(): LiveData<List<Task>>

    @Query(
        """SELECT * FROM tasks WHERE taskType = 'Personal' 
        OR (taskType = 'Group' AND assignee = :userName) 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getPersonalAndAssignedTasks(userName: String): LiveData<List<Task>>

    // ================================================================
    // Query đồng bộ (không LiveData)
    // ================================================================

    @Query(
        """SELECT * FROM tasks ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): Task?

    @Query(
        """SELECT * FROM tasks WHERE assignee = :assignee 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getTasksByAssignee(assignee: String): List<Task>

    @Query(
        """SELECT * FROM tasks WHERE taskType = :taskType 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getTasksByType(taskType: String): List<Task>

    @Query(
        """SELECT * FROM tasks WHERE projectName = :projectName 
        ORDER BY dueDate ASC, 
        CASE priority WHEN 'High' THEN 3 WHEN 'Medium' THEN 2 WHEN 'Low' THEN 1 ELSE 0 END DESC"""
    )
    fun getTasksByProject(projectName: String): List<Task>

    @Query("SELECT DISTINCT projectName FROM tasks WHERE projectName IS NOT NULL AND projectName != ''")
    fun getDistinctProjectNames(): List<String>

    // ========================
    // Truy vấn Workload
    // ========================

    @Query("SELECT COALESCE(SUM(effort), 0) FROM tasks WHERE dueDate = :date")
    fun getTotalEffortByDate(date: String): Int

    @Query("SELECT COALESCE(SUM(effort), 0) FROM tasks WHERE dueDate = :date AND assignee = :assignee")
    fun getTotalEffortByDateAndAssignee(date: String, assignee: String): Int

    // ========================
    // Truy vấn bổ sung
    // ========================

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getIncompleteTasks(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTaskCount(): Int

    @Query("DELETE FROM tasks WHERE projectName = :projectName")
    fun deleteTasksByProject(projectName: String)

    @Query("DELETE FROM tasks WHERE projectName = :projectName AND taskType = 'Group'")
    fun deleteProject(projectName: String)

    @Query("DELETE FROM tasks")
    fun deleteAllTasks()
}
