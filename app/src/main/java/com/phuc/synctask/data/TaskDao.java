package com.phuc.synctask.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.phuc.synctask.model.Task;

import java.util.List;

/**
 * Data Access Object (DAO) cho bảng "tasks".
 *
 * Version 4: Cập nhật 2 LiveData Query phục vụ cho 2 RecyclerView
 * trong PersonalFragment theo logic phân quyền hiển thị.
 */
@Dao
public interface TaskDao {

    // ========================
    // CRUD cơ bản
    // ========================

    @Insert
    long insertTask(Task task);

    @Insert
    long[] insertAllTasks(List<Task> tasks);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    // ================================================================
    // LiveData Query — Phục vụ 2 RecyclerView trong PersonalFragment
    // ================================================================

    /**
     * Lấy CHỈ task Personal (LiveData).
     * Dùng khi currentUserName rỗng (chưa từng nhập tên).
     * → Chỉ hiện danh sách việc cá nhân, không pha trộn Group task.
     *
     * @return LiveData tự động cập nhật
     */
    @Query("SELECT * FROM tasks WHERE taskType = 'Personal' " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    LiveData<List<Task>> getAllPersonalTasks();

    /**
     * Hàm 1: Lấy TOÀN BỘ việc nhóm.
     * Dùng cho RecyclerView dưới (Khu vực "Việc Nhóm").
     * Hiển thị tất cả task có taskType = 'Group' bất kể assignee.
     *
     * @return LiveData tự động cập nhật khi bảng tasks thay đổi
     */
    @Query("SELECT * FROM tasks WHERE taskType = 'Group' " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    LiveData<List<Task>> getAllGroupTasks();

    /**
     * Hàm 2: Lấy việc CÁ NHÂN kết hợp việc NHÓM ĐƯỢC GIAO cho userName.
     * Dùng cho RecyclerView trên (Khu vực "Việc Cá nhân").
     *
     * Logic SQL:
     * - Lấy tất cả task có taskType = 'Personal' (do user tự tạo)
     * - HOẶC task có taskType = 'Group' VÀ assignee = userName (được nhóm giao)
     *
     * Khi userName = "" (chưa nhập tên): điều kiện assignee = '' không khớp
     * → chỉ hiện task Personal.
     *
     * @param userName Tên người dùng hiện tại (nhập từ dialog Import)
     * @return LiveData tự động cập nhật
     */
    @Query("SELECT * FROM tasks WHERE taskType = 'Personal' " +
            "OR (taskType = 'Group' AND assignee = :userName) " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    LiveData<List<Task>> getPersonalAndAssignedTasks(String userName);

    // ================================================================
    // Query đồng bộ (không LiveData) — dùng cho logic xử lý
    // ================================================================

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(int taskId);

    @Query("SELECT * FROM tasks WHERE assignee = :assignee " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    List<Task> getTasksByAssignee(String assignee);

    @Query("SELECT * FROM tasks WHERE taskType = :taskType " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    List<Task> getTasksByType(String taskType);

    @Query("SELECT * FROM tasks WHERE projectName = :projectName " +
            "ORDER BY dueDate ASC, " +
            "CASE priority " +
            "WHEN 'High' THEN 3 " +
            "WHEN 'Medium' THEN 2 " +
            "WHEN 'Low' THEN 1 " +
            "ELSE 0 END DESC")
    List<Task> getTasksByProject(String projectName);

    @Query("SELECT DISTINCT projectName FROM tasks WHERE projectName IS NOT NULL AND projectName != ''")
    List<String> getDistinctProjectNames();

    // ========================
    // Truy vấn Workload
    // ========================

    @Query("SELECT COALESCE(SUM(effort), 0) FROM tasks WHERE dueDate = :date")
    int getTotalEffortByDate(String date);

    @Query("SELECT COALESCE(SUM(effort), 0) FROM tasks WHERE dueDate = :date AND assignee = :assignee")
    int getTotalEffortByDateAndAssignee(String date, String assignee);

    // ========================
    // Truy vấn bổ sung
    // ========================

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    List<Task> getIncompleteTasks();

    @Query("SELECT COUNT(*) FROM tasks")
    int getTaskCount();

    @Query("DELETE FROM tasks WHERE projectName = :projectName")
    void deleteTasksByProject(String projectName);

    /**
     * Xóa toàn bộ dự án nhóm theo tên dự án.
     * Chỉ xóa task có taskType = 'Group' AND projectName khớp.
     * Phải gọi trên Background Thread (ExecutorService).
     *
     * @param projectName Tên dự án cần xóa
     */
    @Query("DELETE FROM tasks WHERE projectName = :projectName AND taskType = 'Group'")
    void deleteProject(String projectName);

    @Query("DELETE FROM tasks")
    void deleteAllTasks();
}
