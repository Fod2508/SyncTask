package com.phuc.synctask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.phuc.synctask.model.Group
import com.phuc.synctask.model.GroupTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý dữ liệu cho một nhóm cụ thể (group tasks, members, group info).
 */
class GroupTaskViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Group Info
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    // Members display names: uid -> displayName
    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames: StateFlow<Map<String, String>> = _memberNames.asStateFlow()

    // Group Tasks
    private val _tasks = MutableStateFlow<List<GroupTask>>(emptyList())
    val tasks: StateFlow<List<GroupTask>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Đảm bảo branded loading hiển thị tối thiểu 1500ms
    private var loadStartTime = 0L

    private var groupListener: ValueEventListener? = null
    private var tasksListener: ValueEventListener? = null
    private var currentGroupId: String? = null

    fun loadGroup(groupId: String) {
        if (groupId == currentGroupId) return
        currentGroupId = groupId
        loadStartTime = System.currentTimeMillis()

        // Clean up old listeners
        cleanup()

        listenToGroupInfo(groupId)
        listenToGroupTasks(groupId)
    }

    private fun listenToGroupInfo(groupId: String) {
        val ref = database.reference.child("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val g = snapshot.getValue(Group::class.java)
                _group.value = g
                // Fetch member display names
                g?.members?.let { fetchMemberNames(it) }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        groupListener = listener
        ref.addValueEventListener(listener)
    }

    private fun fetchMemberNames(memberUids: List<String>) {
        val usersRef = database.reference.child("users")
        val nameMap = mutableMapOf<String, String>()

        for (uid in memberUids) {
            usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("displayName").getValue(String::class.java) ?: "User"
                    nameMap[uid] = name
                    // Update state every time a name is fetched
                    _memberNames.value = nameMap.toMap()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun listenToGroupTasks(groupId: String) {
        val ref = database.reference.child("groupTasks").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<GroupTask>()
                for (child in snapshot.children) {
                    val task = child.getValue(GroupTask::class.java)
                    if (task != null) {
                        task.id = child.key ?: ""
                        task.groupId = groupId
                        taskList.add(task)
                    }
                }
                taskList.sortByDescending { it.timestamp }
                _tasks.value = taskList
                // Branded loading: đảm bảo hiển thị tối thiểu 1500ms
                viewModelScope.launch {
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    val remaining = 1500L - elapsed
                    if (remaining > 0) delay(remaining)
                    _isLoading.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        tasksListener = listener
        ref.addValueEventListener(listener)
    }

    fun addGroupTask(groupId: String, title: String, description: String, dueDate: Long?, assignedToId: String? = null) {
        val uid = currentUserId ?: return
        val ref = database.reference.child("groupTasks").child(groupId)
        val taskId = ref.push().key ?: return

        val task = GroupTask(
            id = taskId,
            groupId = groupId,
            title = title,
            description = description,
            isCompleted = false,
            creatorId = uid,
            assignedToId = assignedToId,
            timestamp = System.currentTimeMillis(),
            dueDate = dueDate
        )
        ref.child(taskId).setValue(task).addOnSuccessListener {
            if (assignedToId != null && assignedToId != uid) {
                sendNotificationToUser(groupId, assignedToId, title)
            }
        }
    }

    fun claimTask(groupId: String, taskId: String) {
        val uid = currentUserId ?: return
        database.reference.child("groupTasks").child(groupId).child(taskId)
            .child("assignedToId").setValue(uid)
    }

    fun assignTask(groupId: String, taskId: String, taskTitle: String, assignedToId: String) {
        val uid = currentUserId ?: return
        database.reference.child("groupTasks").child(groupId).child(taskId)
            .child("assignedToId").setValue(assignedToId).addOnSuccessListener {
                if (assignedToId != uid) {
                    sendNotificationToUser(groupId, assignedToId, taskTitle)
                }
            }
    }

    private fun sendNotificationToUser(groupId: String, assignedToId: String, taskTitle: String) {
        val currentUid = currentUserId ?: return
        val currentUserName = _memberNames.value[currentUid] ?: "Ai đó"
        val groupName = _group.value?.name ?: "nhóm"

        // Lấy FCM Token của người được giao và gửi thông báo
        database.reference.child("users").child(assignedToId).child("fcmToken").get()
            .addOnSuccessListener { snapshot ->
                val fcmToken = snapshot.getValue(String::class.java)
                if (!fcmToken.isNullOrEmpty()) {
                    val title = "Có công việc mới từ nhóm $groupName"
                    val body = "$currentUserName vừa phân công cho bạn task: $taskTitle"
                    com.phuc.synctask.util.NotificationHelper.sendPushNotification(fcmToken, title, body)
                }
            }
    }

    fun toggleTaskStatus(groupId: String, task: GroupTask) {
        val newStatus = !task.isCompleted
        database.reference.child("groupTasks").child(groupId).child(task.id)
            .child("isCompleted").setValue(newStatus)
    }

    fun deleteGroupTask(groupId: String, taskId: String) {
        database.reference.child("groupTasks").child(groupId).child(taskId).removeValue()
    }

    private fun cleanup() {
        currentGroupId?.let { gid ->
            groupListener?.let {
                database.reference.child("groups").child(gid).removeEventListener(it)
            }
            tasksListener?.let {
                database.reference.child("groupTasks").child(gid).removeEventListener(it)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
