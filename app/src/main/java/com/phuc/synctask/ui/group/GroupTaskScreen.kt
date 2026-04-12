package com.phuc.synctask.ui.group

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.ui.main.getAvatarColor
import com.phuc.synctask.ui.main.getInitials
import com.phuc.synctask.viewmodel.GroupTaskViewModel
import com.phuc.synctask.ui.common.AnimatedLoadingScreen
import com.phuc.synctask.ui.common.DeleteConfirmationDialog
import com.phuc.synctask.ui.common.EmptyTaskState
import com.phuc.synctask.ui.common.AchievementUnlockedDialog
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

// ─── Design Tokens (màu chức năng cố định giữ nguyên) ───
private val IndigoPrimary = Color(0xFF4B3FBE)
private val SuccessGreen  = Color(0xFF22C55E)
private val WarnYellow    = Color(0xFFEAB308)
private val DangerRed     = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTaskScreen(
    groupId: String,
    onBack: () -> Unit = {},
    viewModel: GroupTaskViewModel = viewModel()
) {
    // Load data for this group
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    val group by viewModel.group.collectAsState()
    val memberNames by viewModel.memberNames.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val achievementQueue by viewModel.achievementQueue.collectAsState()
    val currentUid = viewModel.currentUserId
    val isOwner = currentUid != null && currentUid == group?.ownerId

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<GroupTask?>(null) }
    var taskToDelete by remember { mutableStateOf<GroupTask?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    var showGroupConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(showGroupConfetti) {
        if (showGroupConfetti) {
            delay(3000L)
            showGroupConfetti = false
        }
    }

    // Hiển thị loading screen khi chưa có dữ liệu lần đầu
    if (isLoading) {
        AnimatedLoadingScreen(message = "Đang tải nhóm...")
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group?.name ?: "Nhóm",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Tùy chọn",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isOwner) "Giải tán nhóm" else "Rời nhóm",
                                        color = DangerRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isOwner) Icons.Filled.DeleteOutline
                                                      else Icons.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = DangerRed
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showLeaveDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndigoPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Task Mới", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ─── Section 1: Productivity Rings ───
            item {
                ProductivityRingsSection(
                    memberUids = group?.members ?: emptyList(),
                    memberNames = memberNames,
                    tasks = tasks,
                    ownerId = group?.ownerId
                )
            }

            // ─── Section 2: Activity Feed Header ───
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

            // ─── Section 3: Task Cards (Activity Feed) ───
            if (tasks.isEmpty()) {
                item {
                    EmptyTaskState(
                        imageResId = R.drawable.ic_empty_state_group,
                        title      = "Chưa có dự án nào!",
                        subtitle   = "Hãy bấm nút + để tạo dự án nhóm mới và giao việc."
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
                            if (!task.isCompleted) {
                                showGroupConfetti = true
                            }
                            viewModel.toggleTaskStatus(groupId, task)
                        },
                        onDelete = { taskToDelete = task },
                        onClick = { selectedTask = task }
                    )
                }
            }
        }
    }

    // Effect Pháo hoa Nhóm (bắn từ 2 góc)
    if (showGroupConfetti) {
        val partyLeft = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            angle = 45, // Bắn chéo lên từ bên trái
            spread = 45,
            colors = listOf(0xFF4B3FBE.toInt(), 0xFFFFD700.toInt(), 0xFF6C63FF.toInt()), // Indigo + Gold
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.0, 0.5) // Từ cạnh trái ở giữa màn hình
        )
        val partyRight = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            angle = 135, // Bắn chéo lên từ bên phải
            spread = 45,
            colors = listOf(0xFF4B3FBE.toInt(), 0xFFFFD700.toInt(), 0xFF6C63FF.toInt()), // Indigo + Gold
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(1.0, 0.5) // Từ cạnh phải ở giữa màn hình
        )
        KonfettiView(
            parties = listOf(partyLeft, partyRight),
            modifier = Modifier.fillMaxSize()
        )
    }
    } // Đóng Box

    // ─── Add Group Task Bottom Sheet ───
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

    // ─── Task Detail Bottom Sheet ───
    selectedTask?.let { task ->
        TaskDetailBottomSheet(
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            onDismiss = { selectedTask = null }
        )
    }

    // ─── Dialog xác nhận xóa ───
    taskToDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                viewModel.deleteGroupTask(groupId, task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    // ─── Dialog thành tựu nhóm (queue) ───
    if (achievementQueue.isNotEmpty()) {
        val currentAchievement = achievementQueue.first()
        LaunchedEffect(currentAchievement) {
            delay(3000L)
            viewModel.dismissCurrentAchievement()
        }
        AchievementUnlockedDialog(
            achievementId = currentAchievement,
            onDismiss = { viewModel.dismissCurrentAchievement() }
        )
    }

    // ─── Dialog rời / giải tán nhóm ───
    if (showLeaveDialog) {        LeaveGroupDialog(
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

// ───────────────────────────────────────────
// Productivity Rings Section
// ───────────────────────────────────────────
@Composable
private fun ProductivityRingsSection(
    memberUids: List<String>,
    memberNames: Map<String, String>,
    tasks: List<GroupTask>,
    ownerId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IndigoPrimary)
            .padding(vertical = 20.dp)
    ) {
        Text(
            text = "TIẾN ĐỘ THÀNH VIÊN",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(memberUids) { uid ->
                val name = memberNames[uid] ?: "..."
                val initials = getInitials(name)

                // Calculate progress for this member
                val memberTasks = tasks.filter { it.assignedToId == uid }
                val progress = if (memberTasks.isNotEmpty()) {
                    memberTasks.count { it.isCompleted }.toFloat() / memberTasks.size
                } else {
                    0f
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 800),
                    label = "progress"
                )

                val ringColor = when {
                    progress >= 0.7f -> SuccessGreen
                    progress >= 0.4f -> WarnYellow
                    else -> DangerRed
                }

                ProductivityRingItem(
                    initials = initials,
                    name = name,
                    progress = animatedProgress,
                    ringColor = if (memberTasks.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else ringColor,
                    isOwner = uid == ownerId
                )
            }
        }
    }
}

@Composable
private fun ProductivityRingItem(
    initials: String,
    name: String,
    progress: Float,
    ringColor: Color,
    isOwner: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(60.dp)
        ) {
            // Background ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.15f),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
            // Progress ring
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(name)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            if (isOwner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-2).dp)
                        .size(16.dp)
                        .background(WarnYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Owner", tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ───────────────────────────────────────────
// Activity Feed Card (Social-media style post)
// ───────────────────────────────────────────
@Composable
private fun ActivityFeedCard(
    task: GroupTask,
    memberNames: Map<String, String>,
    currentUid: String?,
    isGroupOwner: Boolean,
    onClaim: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val assignedName = task.assignedToId?.let { memberNames[it] }
    val isAssigned = task.assignedToId != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ─── Row 1: Avatar + Status ───
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mini avatar
                val avatarInitials = if (isAssigned) {
                    getInitials(assignedName ?: "?")
                } else {
                    "?"
                }
                val avatarColor = if (isAssigned) getAvatarColor(assignedName ?: "?") else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        avatarInitials,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val statusText = when {
                        task.isCompleted && isAssigned -> "${assignedName} vừa hoàn thành ✓"
                        isAssigned -> "${assignedName} đang làm"
                        else -> "Task mới, chưa phân công"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Checkbox & Delete
                if (isGroupOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa", tint = DangerRed)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = IndigoPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── Row 2: Task Title ───
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (task.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Row 3: Footer Chips ───
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Deadline chip
                if (task.dueDate != null) {
                    val now = System.currentTimeMillis()
                    val isOverdue = !task.isCompleted && task.dueDate!! < now
                    val isSoonWarning = !task.isCompleted && !isOverdue && (task.dueDate!! - now) <= 60 * 60 * 1000L
                    val sdf = SimpleDateFormat("HH:mm, dd 'Th'MM", Locale.getDefault())
                    val dateStr = sdf.format(Date(task.dueDate!!))
                    val chipColor = when {
                        isOverdue -> DangerRed
                        isSoonWarning -> Color(0xFFE65100)
                        else -> IndigoPrimary
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = chipColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSoonWarning) Icons.Filled.AccessTime else Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = chipColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                dateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = chipColor
                            )
                        }
                    }
                }

                // Priority chip (placeholder based on task position)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = WarnYellow.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = WarnYellow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Nhóm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WarnYellow
                        )
                    }
                }
            }

            // ─── "Nhận việc" button if unassigned ───
            if (!isAssigned && !task.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = IndigoPrimary
                    )
                ) {
                    Text("🙋 Nhận việc", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ───────────────────────────────────────────
// Add Group Task Bottom Sheet
// ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupTaskSheet(
    memberNames: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, dueDate: Long?, assignedToId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var selectedAssigneeId by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Tạo công việc nhóm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên công việc") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Mô tả chi tiết (tuỳ chọn)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            val dateText = if (dueDate != null) {
                val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                "📅 ${sdf.format(Date(dueDate!!))}"
            } else {
                "📅 Chọn hạn chót"
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(dateText)
            }

            if (dueDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val timeSdf = SimpleDateFormat("HH:mm, dd 'Th'MM", Locale.getDefault())
                    Text(
                        text = "Deadline: ${timeSdf.format(Date(dueDate!!))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndigoPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Giao cho:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(memberNames.entries.toList()) { (uid, name) ->
                    val isSelected = selectedAssigneeId == uid
                    val initials = getInitials(name)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedAssigneeId = if (isSelected) null else uid
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(getAvatarColor(name))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) IndigoPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
                                        .background(SuccessGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center,
                            color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(title.trim(), description.trim(), dueDate, selectedAssigneeId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = title.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Text("Lưu Công Việc Nhóm", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
                        cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                        cal.set(java.util.Calendar.MINUTE, selectedMinute)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        dueDate = cal.timeInMillis
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Tiếp theo") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            },
            text = { DatePicker(state = datePickerState) }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    dueDate?.let { existingMillis ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = existingMillis }
                        cal.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        dueDate = cal.timeInMillis
                    }
                    showTimePicker = false
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn giờ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    title: String,
    description: String,
    dueDate: Long?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Mô tả",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (description.isNotBlank()) description else "Không có mô tả thêm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Thông tin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            val dateText = if (dueDate != null) {
                val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(dueDate))
            } else "Không có hạn chót"
            
            Text(
                text = "Hạn chót: $dateText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đóng", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────────────────────────
// Leave / Delete Group Confirmation Dialog
// ───────────────────────────────────────────
@Composable
private fun LeaveGroupDialog(
    isOwner: Boolean,
    groupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hình minh họa tái sử dụng ic_delete_confirm
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_delete_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isOwner) "Giải tán nhóm?" else "Rời khỏi nhóm?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isOwner)
                        "Hành động này sẽ xóa toàn bộ dữ liệu công việc của nhóm \"$groupName\". Bạn có chắc chắn?"
                    else
                        "Bạn sẽ không thể xem hay chỉnh sửa công việc của nhóm \"$groupName\" nữa.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = if (isOwner) "Giải tán" else "Rời nhóm",
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}
