package com.phuc.synctask.model

import com.google.firebase.database.PropertyName

data class Group(
    var id: String = "",
    var name: String = "",
    var inviteCode: String = "",
    var ownerId: String = "",
    var members: List<String> = emptyList()
) {
    // Constructor mặc định cho Firebase deserialization
    constructor() : this("", "", "", "", emptyList())
}

data class GroupTask(
    var id: String = "",
    var groupId: String = "",
    var title: String = "",
    var description: String = "",
    
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    
    var creatorId: String = "",
    var assignedToId: String? = null,
    var timestamp: Long = 0L,
    var dueDate: Long? = null,
    var completedDate: Long? = null
) {
    // Constructor mặc định cho Firebase
    constructor() : this("", "", "", "", false, "", null, 0L, null, null)
}
