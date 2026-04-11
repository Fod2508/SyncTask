package com.phuc.synctask.model

import com.google.firebase.database.PropertyName

/**
 * Data class đại diện cho một công việc (Task) trong Firebase Realtime Database.
 * Đường dẫn: /users/{uid}/tasks/{taskId}
 *
 * Constructor mặc định không tham số bắt buộc để Firebase có thể deserialize.
 * Dùng @PropertyName để ép Firebase giữ nguyên tên trường Boolean
 * (mặc định Firebase strip prefix "is" → gây lệch key).
 */
data class FirebaseTask(
    var id: String = "",
    var title: String = "",
    var description: String = "",

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    @get:PropertyName("isUrgent")
    @set:PropertyName("isUrgent")
    var isUrgent: Boolean = false,

    @get:PropertyName("isImportant")
    @set:PropertyName("isImportant")
    var isImportant: Boolean = false,

    var creatorId: String = "",
    var timestamp: Long = 0L,
    var dueDate: Long? = null,
    var completedDate: Long? = null
) {
    /** Constructor mặc định không tham số — bắt buộc cho Firebase deserialization */
    constructor() : this("", "", "", false, false, false, "", 0L, null, null)
}

enum class Quadrant {
    DO_NOW,       // isUrgent=true,  isImportant=true
    PLAN,         // isUrgent=false, isImportant=true
    DELEGATE,     // isUrgent=true,  isImportant=false
    ELIMINATE     // isUrgent=false, isImportant=false
}

fun FirebaseTask.quadrant(): Quadrant = when {
    isUrgent && isImportant   -> Quadrant.DO_NOW
    !isUrgent && isImportant  -> Quadrant.PLAN
    isUrgent && !isImportant  -> Quadrant.DELEGATE
    else                      -> Quadrant.ELIMINATE
}
