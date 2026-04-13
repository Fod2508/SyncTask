package com.phuc.synctask.ui.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.R
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.ui.common.AchievementUnlockedDialog
import com.phuc.synctask.ui.common.AnimatedLoadingScreen
import com.phuc.synctask.ui.common.DeleteConfirmationDialog
import com.phuc.synctask.ui.common.EmptyTaskState
import com.phuc.synctask.utils.AppSoundPlayer
import com.phuc.synctask.viewmodel.GroupTaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTaskScreen(
    groupId: String,
    onBack: () -> Unit = {},
    viewModel: GroupTaskViewModel = viewModel()
) {
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val group by viewModel.group.collectAsState()
    val memberNames by viewModel.memberNames.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unlockedAchievementId by viewModel.achievementUnlocked.collectAsState()
    val currentUid = viewModel.currentUserId
    val isOwner = currentUid != null && currentUid == group?.ownerId

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<GroupTask?>(null) }
    var taskToDelete by remember { mutableStateOf<GroupTask?>(null) }
    var recentlyDeletedTask by remember { mutableStateOf<GroupTask?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showGroupConfetti by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.soundEvent.collectLatest { effect ->
            AppSoundPlayer.play(effect)
        }
    }

    LaunchedEffect(showGroupConfetti) {
        if (showGroupConfetti) {
            delay(3000L)
            showGroupConfetti = false
        }
    }

    LaunchedEffect(recentlyDeletedTask?.id) {
        val deletedTask = recentlyDeletedTask ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Đã xóa task: ${deletedTask.title}",
            actionLabel = "Hoàn tác",
            duration = SnackbarDuration.Short
        )

        if (result == SnackbarResult.ActionPerformed) {
            viewModel.restoreGroupTask(groupId, deletedTask)
        }
        recentlyDeletedTask = null
    }

    if (isLoading) {
        AnimatedLoadingScreen(message = "Đang tải nhóm...")
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                GroupTaskTopBar(
                    groupName = group?.name ?: "Nhóm",
                    isOwner = isOwner,
                    onBack = onBack,
                    onShowLeaveDialog = { showLeaveDialog = true }
                )
            },
            floatingActionButton = {
                GroupTaskFab(onClick = { showAddSheet = true })
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    ProductivityRingsSection(
                        memberUids = group?.members ?: emptyList(),
                        memberNames = memberNames,
                        tasks = tasks,
                        ownerId = group?.ownerId
                    )
                }

                item {
                    Text(
                        text = "BẢNG TIN HOẠT ĐỘNG",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                if (tasks.isEmpty()) {
                    item {
                        EmptyTaskState(
                            imageResId = R.drawable.ic_empty_state_group,
                            title = "Chưa có dự án nào!",
                            subtitle = "Hãy bấm nút + để tạo dự án nhóm mới và giao việc."
                        )
                    }
                } else {
                    items(items = tasks, key = { it.id.ifBlank { it.hashCode() } }) { task ->
                        ActivityFeedCard(
                            task = task,
                            memberNames = memberNames,
                            currentUid = currentUid,
                            isGroupOwner = isOwner,
                            onClaim = { viewModel.claimTask(groupId, task.id) },
                            onToggle = {
                                if (!task.isCompleted) showGroupConfetti = true
                                viewModel.toggleTaskStatus(groupId, task)
                            },
                            onDelete = { taskToDelete = task },
                            onClick = { selectedTask = task }
                        )
                    }
                }
            }
        }

        GroupTaskConfettiOverlay(show = showGroupConfetti)
    }

    if (showAddSheet) {
        AddGroupTaskSheet(
            memberNames = memberNames,
            onDismiss = { showAddSheet = false },
            onSave = { title, desc, dueDate, assignedToId ->
                viewModel.addGroupTask(groupId, title, desc, dueDate, assignedToId)
                showAddSheet = false
            }
        )
    }

    selectedTask?.let { task ->
        TaskDetailBottomSheet(
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            onDismiss = { selectedTask = null }
        )
    }

    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                viewModel.deleteGroupTask(groupId, task.id)
                recentlyDeletedTask = task
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    unlockedAchievementId?.let { id ->
        AchievementUnlockedDialog(
            achievementId = id,
            onDismiss = { viewModel.dismissAchievementDialog() }
        )
    }

    if (showLeaveDialog) {
        LeaveGroupDialog(
            isOwner = isOwner,
            groupName = group?.name ?: "nhóm",
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveOrDeleteGroup(groupId) { onBack() }
            },
            onDismiss = { showLeaveDialog = false }
        )
    }
}
