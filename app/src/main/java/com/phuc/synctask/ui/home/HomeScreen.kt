package com.phuc.synctask.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.viewmodel.HomeUiState
import com.phuc.synctask.viewmodel.HomeViewModel
import com.phuc.synctask.ui.main.AddTaskBottomSheet
import com.phuc.synctask.ui.group.TaskDetailBottomSheet
import com.phuc.synctask.util.LocalSoundManager
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<FirebaseTask?>(null) }

    var showConfetti by remember { mutableStateOf(false) }
    val achievementQueue by viewModel.achievementQueue.collectAsState()
    val soundManager = LocalSoundManager.current

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(3000L)
            showConfetti = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SyncTask",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Đăng xuất", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm công việc")
            }
        }
    ) { padding ->

        // Xử lý 4 trạng thái UiState
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có công việc nào. Nhấn + để thêm!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.tasks,
                        key = { it.id }
                    ) { task ->
                        SwipeableTaskItem(
                            task = task,
                            onDelete = { viewModel.deleteTask(task.id) },
                            onToggle = {
                                if (!task.isCompleted) {
                                    showConfetti = true
                                    soundManager?.playFireworks()
                                }
                                viewModel.toggleTaskStatus(task)
                            },
                            onClick = { selectedTask = task }
                        )
                    }
                }
            }
        }
    }

    // Effect Pháo hoa Overlay
    if (showConfetti) {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xFF4CAF50.toInt(), 0xFFFFEB3B.toInt(), 0xFFF44336.toInt(), 0xFF3F51B5.toInt(), 0xFF2196F3.toInt()),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.2)
        )
        KonfettiView(
            parties = listOf(party),
            modifier = Modifier.fillMaxSize()
        )
    }
    } // Đóng Box

    // ModalBottomSheet để thêm task mới
    if (showBottomSheet) {
        AddTaskBottomSheet(
            onDismiss = { showBottomSheet = false },
            onSave = { task ->
                viewModel.addTask(
                    title = task.title,
                    description = task.description,
                    isUrgent = task.isUrgent,
                    isImportant = task.isImportant,
                    dueDate = task.dueDate
                )
                showBottomSheet = false
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

    // ─── Achievement queue dialog ───
    if (achievementQueue.isNotEmpty()) {
        val currentAchievement = achievementQueue.first()
        LaunchedEffect(currentAchievement) {
            soundManager?.playAchievement()
            delay(3000L)
            viewModel.dismissCurrentAchievement()
        }
        com.phuc.synctask.ui.common.AchievementUnlockedDialog(
            achievementId = currentAchievement,
            onDismiss = { viewModel.dismissCurrentAchievement() }
        )
    }
}

/**
 * Item task có hỗ trợ swipe:
 * - Vuốt END (sang trái) → xoá (nền đỏ, icon thùng rác)
 * - Vuốt START (sang phải) → toggle trạng thái (nền xanh, icon check)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: FirebaseTask,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggle()
                    false // Trả về false để reset lại vị trí item sau khi toggle
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    // Reset state sau khi swipe toggle (START → SETTLED)
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val backgroundColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    else -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "swipe_bg_color"
            )

            val iconAlignment = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            val icon = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.CheckCircle
                else -> Icons.Filled.Delete
            }

            val iconTint = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                SwipeToDismissBoxValue.StartToEnd -> Color.White
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = iconAlignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        TaskCard(task = task, onToggle = onToggle, onClick = onClick)
    }
}

/**
 * Card hiển thị thông tin một Task.
 * Bo góc 16.dp, tối giản, hiển thị title + description + icon trạng thái.
 */
@Composable
fun TaskCard(
    task: FirebaseTask,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon trạng thái (checkbox)
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (task.isCompleted)
                        Icons.Filled.CheckCircle
                    else
                        Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "Hoàn thành" else "Chưa hoàn thành",
                    tint = if (task.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nội dung task
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (task.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Hiển thị deadline với giờ
                if (task.dueDate != null && !task.isCompleted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val now = System.currentTimeMillis()
                    val isOverdue = task.dueDate!! < now
                    val isSoonWarning = !isOverdue && (task.dueDate!! - now) <= 60 * 60 * 1000L
                    val deadlineColor = when {
                        isOverdue -> MaterialTheme.colorScheme.error
                        isSoonWarning -> Color(0xFFE65100) // Cam cảnh báo
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val sdf = SimpleDateFormat("HH:mm, dd 'Th'MM", Locale.getDefault())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = deadlineColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sdf.format(Date(task.dueDate!!)),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = deadlineColor,
                            fontWeight = if (isSoonWarning || isOverdue) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}


