package com.phuc.synctask.ui.personal

import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.model.quadrant
import com.phuc.synctask.ui.main.AddTaskBottomSheet
import com.phuc.synctask.ui.group.TaskDetailBottomSheet
import com.phuc.synctask.ui.common.DeleteConfirmationDialog
import com.phuc.synctask.ui.common.EmptyTaskState
import com.phuc.synctask.ui.common.AchievementUnlockedDialog
import com.phuc.synctask.viewmodel.HomeViewModel
import com.phuc.synctask.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuadrantDetailScreen(
    quadrant: Quadrant,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val tasksFlow by viewModel.tasks.collectAsState()
    val allTasks = tasksFlow.filter { it.quadrant() == quadrant }

    val uncompletedTasks = allTasks.filter { !it.isCompleted }
    val completedTasks = allTasks.filter { it.isCompleted }

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<FirebaseTask?>(null) }
    var taskToDelete by remember { mutableStateOf<FirebaseTask?>(null) }

    // Lắng nghe StateFlow thành tựu
    val unlockedAchievementId by viewModel.achievementUnlocked.collectAsState()

    val bgColor = when (quadrant) {
        Quadrant.DO_NOW -> Color(0xFFFFF0F0)
        Quadrant.PLAN -> Color(0xFFEBF3FF)
        Quadrant.DELEGATE -> Color(0xFFFFF8EC)
        Quadrant.ELIMINATE -> Color(0xFFF5F5F5)
    }
    val qIcon = when (quadrant) {
        Quadrant.DO_NOW -> Icons.Filled.FlashOn
        Quadrant.PLAN -> Icons.Filled.ListAlt
        Quadrant.DELEGATE -> Icons.Filled.PersonAdd
        Quadrant.ELIMINATE -> Icons.Filled.Block
    }
    val accentColor = when (quadrant) {
        Quadrant.DO_NOW -> Color(0xFFD32F2F)
        Quadrant.PLAN -> Color(0xFF1976D2)
        Quadrant.DELEGATE -> Color(0xFFE65100)
        Quadrant.ELIMINATE -> Color(0xFF757575)
    }
    val title = when (quadrant) {
        Quadrant.DO_NOW -> "Làm ngay"
        Quadrant.PLAN -> "Lên kế hoạch"
        Quadrant.DELEGATE -> "Ủy quyền"
        Quadrant.ELIMINATE -> "Loại bỏ"
    }
    val subtitle = when (quadrant) {
        Quadrant.DO_NOW -> "Khẩn & Quan trọng"
        Quadrant.PLAN -> "Quan trọng, Không khẩn"
        Quadrant.DELEGATE -> "Khẩn, Không quan trọng"
        Quadrant.ELIMINATE -> "Không khẩn & Không quan trọng"
    }

    val totalCount = allTasks.size
    val completedCount = completedTasks.size
    val overdueCount = uncompletedTasks.count {
        it.dueDate != null && it.dueDate!! < System.currentTimeMillis()
    }

    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(3000L)
            showConfetti = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = accentColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── HERO HEADER ──
            val statusBarHeight = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(top = statusBarHeight)
                    .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Column {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(qIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("$totalCount task", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("$completedCount hoàn thành", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        if (overdueCount > 0) {
                            Text("$overdueCount quá hạn", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── BODY ──
            if (allTasks.isEmpty()) {
                EmptyTaskState(
                    imageResId = R.drawable.ic_empty_state_personal,
                    title      = "Yay! Bạn đang rảnh rỗi!",
                    subtitle   = "Hãy bấm nút + để tạo nhiệm vụ đầu tiên nhé."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Section: CÒN LẠI ──
                    if (uncompletedTasks.isNotEmpty()) {
                        stickyHeader(key = "header_pending") {
                            Text(
                                "CÒN LẠI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = uncompletedTasks,
                            key = { task -> task.id.ifBlank { "pending_${task.hashCode()}" } }
                        ) { task ->
                            QuadrantTaskCard(
                                task = task,
                                accentColor = accentColor,
                                onDelete = { taskToDelete = task },
                                onToggle = {
                                    if (!task.isCompleted) showConfetti = true
                                    viewModel.toggleTaskStatus(task)
                                },
                                onClick = { selectedTask = task }
                            )
                        }
                    }

                    // ── Section: HOÀN THÀNH ──
                    if (completedTasks.isNotEmpty()) {
                        stickyHeader(key = "header_completed") {
                            Text(
                                "HOÀN THÀNH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = completedTasks,
                            key = { task -> task.id.ifBlank { "completed_${task.hashCode()}" } }
                        ) { task ->
                            QuadrantTaskCard(
                                task = task,
                                accentColor = accentColor,
                                onDelete = { taskToDelete = task },
                                onToggle = { viewModel.toggleTaskStatus(task) },
                                onClick = { selectedTask = task }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── AddTaskBottomSheet ──
    if (showAddSheet) {
        AddTaskBottomSheet(
            initialQuadrant = quadrant,
            onDismiss = { showAddSheet = false },
            onSave = { task ->
                viewModel.addTask(
                    title = task.title,
                    description = task.description,
                    isUrgent = task.isUrgent,
                    isImportant = task.isImportant,
                    dueDate = task.dueDate
                )
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

    // Dialog xác nhận xóa
    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                viewModel.deleteTask(task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

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

    // Dialog thành tựu mở khóa
    unlockedAchievementId?.let { id ->
        AchievementUnlockedDialog(
            achievementId = id,
            onDismiss = { viewModel.dismissAchievementDialog() }
        )
    }

    } // Đóng Box
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Task Card — stable, no SwipeToDismiss
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun QuadrantTaskCard(
    task: FirebaseTask,
    accentColor: Color,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (task.isCompleted) 0.55f else 1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        uncheckedColor = accentColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    val safeTitle = task.title.ifBlank { "Không có tiêu đề" }
                    Text(
                        safeTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            task.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Deadline footer ──
            if (task.dueDate != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
                val isOverdue = !task.isCompleted && task.dueDate!! < System.currentTimeMillis()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdue) {
                        Icon(Icons.Filled.Warning, contentDescription = "Overdue", tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val diffDays = (System.currentTimeMillis() - task.dueDate!!) / (1000 * 60 * 60 * 24)
                        Text("Quá hạn $diffDays ngày", fontSize = 12.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Deadline", tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        Text(sdf.format(Date(task.dueDate!!)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
