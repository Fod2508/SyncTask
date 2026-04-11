package com.phuc.synctask.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một công việc (Task) trong cơ sở dữ liệu Room.
 * Mỗi đối tượng Task tương ứng với một hàng trong bảng "tasks".
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    var title: String = "",
    var dueDate: String? = null,
    var priority: String = "Medium",
    var effort: Int = 1,
    var isCompleted: Boolean = false,
    var taskType: String = "Personal",
    var projectName: String? = null,
    var assignee: String? = null
) {
    /**
     * Chuyển đổi giá trị priority thành số để sắp xếp.
     * High = 3, Medium = 2, Low = 1.
     */
    fun getPriorityValue(): Int = when (priority) {
        "High" -> 3
        "Medium" -> 2
        else -> 1
    }
}
