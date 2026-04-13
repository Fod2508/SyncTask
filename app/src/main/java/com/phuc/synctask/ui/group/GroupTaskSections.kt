package com.phuc.synctask.ui.group

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuc.synctask.model.GroupTask
import com.phuc.synctask.ui.main.getAvatarColor
import com.phuc.synctask.ui.main.getInitials
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductivityRingsSection(
    memberUids: List<String>,
    memberNames: Map<String, String>,
    tasks: List<GroupTask>,
    ownerId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroupTaskUiTokens.IndigoPrimary)
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
                    progress >= 0.7f -> GroupTaskUiTokens.SuccessGreen
                    progress >= 0.4f -> GroupTaskUiTokens.WarnYellow
                    else -> GroupTaskUiTokens.DangerRed
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
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.15f),
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                strokeWidth = 4.dp,
                strokeCap = StrokeCap.Round
            )
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
                        .background(GroupTaskUiTokens.WarnYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Trưởng nhóm",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
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

@Composable
fun ActivityFeedCard(
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarInitials = if (isAssigned) getInitials(assignedName ?: "?") else "?"
                val avatarColor = if (isAssigned) {
                    getAvatarColor(assignedName ?: "?")
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
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
                        color = if (task.isCompleted) GroupTaskUiTokens.SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isGroupOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa", tint = GroupTaskUiTokens.DangerRed)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GroupTaskUiTokens.IndigoPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dueDate = task.dueDate
                if (dueDate != null) {
                    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                    val dateStr = sdf.format(Date(dueDate))
                    val isOverdue = !task.isCompleted && dueDate < System.currentTimeMillis()

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isOverdue) GroupTaskUiTokens.DangerRed.copy(alpha = 0.1f)
                        else GroupTaskUiTokens.IndigoPrimary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = "Hạn chót nhiệm vụ",
                                modifier = Modifier.size(14.dp),
                                tint = if (isOverdue) GroupTaskUiTokens.DangerRed else GroupTaskUiTokens.IndigoPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                dateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverdue) GroupTaskUiTokens.DangerRed else GroupTaskUiTokens.IndigoPrimary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GroupTaskUiTokens.WarnYellow.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = "Nhiệm vụ nhóm",
                            modifier = Modifier.size(14.dp),
                            tint = GroupTaskUiTokens.WarnYellow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Nhóm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GroupTaskUiTokens.WarnYellow
                        )
                    }
                }
            }

            if (!isAssigned && !task.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GroupTaskUiTokens.IndigoPrimary)
                ) {
                    Text("🙋 Nhận việc", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
