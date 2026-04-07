package com.phuc.synctask.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity đại diện cho một công việc (Task) trong cơ sở dữ liệu Room.
 * Mỗi đối tượng Task tương ứng với một hàng trong bảng "tasks".
 *
 * Version 2: Thêm trường projectName để phân biệt task thuộc project nào.
 */
@Entity(tableName = "tasks")
public class Task {

    // Khóa chính, tự động tăng
    @PrimaryKey(autoGenerate = true)
    private int id;

    // Tiêu đề công việc
    private String title;

    // Ngày hết hạn, định dạng YYYY-MM-DD
    private String dueDate;

    // Mức độ ưu tiên: "Low", "Medium", "High"
    private String priority;

    // Trọng số / thời gian tiêu tốn (1 đến 3)
    private int effort;

    // Trạng thái hoàn thành
    private boolean isCompleted;

    // Loại công việc: "Personal" hoặc "Group"
    private String taskType;

    // Tên dự án/project mà task thuộc về (dùng cho Tab Nhóm)
    private String projectName;

    // Tên người được giao việc
    private String assignee;

    // ========================
    // Constructor
    // ========================

    /**
     * Constructor đầy đủ (không bao gồm id vì Room tự sinh).
     *
     * @param title       Tiêu đề công việc
     * @param dueDate     Ngày hết hạn (YYYY-MM-DD)
     * @param priority    Mức ưu tiên ("Low", "Medium", "High")
     * @param effort      Trọng số công việc (1-3)
     * @param isCompleted Đã hoàn thành hay chưa
     * @param taskType    Loại task ("Personal", "Group")
     * @param projectName Tên project (null nếu Personal)
     * @param assignee    Người được giao
     */
    public Task(String title, String dueDate, String priority, int effort,
                boolean isCompleted, String taskType, String projectName, String assignee) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.effort = effort;
        this.isCompleted = isCompleted;
        this.taskType = taskType;
        this.projectName = projectName;
        this.assignee = assignee;
    }

    // ========================
    // Getter & Setter
    // ========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getEffort() {
        return effort;
    }

    public void setEffort(int effort) {
        this.effort = effort;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    // ========================
    // Utility Methods
    // ========================

    /**
     * Chuyển đổi giá trị priority thành số để sắp xếp.
     * High = 3 (cao nhất), Medium = 2, Low = 1 (thấp nhất).
     *
     * @return giá trị số tương ứng với mức ưu tiên
     */
    public int getPriorityValue() {
        switch (priority) {
            case "High":
                return 3;
            case "Medium":
                return 2;
            case "Low":
            default:
                return 1;
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", priority='" + priority + '\'' +
                ", effort=" + effort +
                ", isCompleted=" + isCompleted +
                ", taskType='" + taskType + '\'' +
                ", projectName='" + projectName + '\'' +
                ", assignee='" + assignee + '\'' +
                '}';
    }
}
