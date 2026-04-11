package com.phuc.synctask.ui.personal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.model.quadrant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListBottomSheet(
    quadrant: Quadrant,
    allTasks: List<FirebaseTask>,
    onDismiss: () -> Unit,
    onToggleStatus: (FirebaseTask) -> Unit,
    onDelete: (FirebaseTask) -> Unit
) {
    val tasks = allTasks.filter { it.quadrant() == quadrant }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val title = when (quadrant) { Quadrant.DO_NOW -> "Làm ngay"; Quadrant.PLAN -> "Lên kế hoạch"; Quadrant.DELEGATE -> "Ủy quyền"; Quadrant.ELIMINATE -> "Loại bỏ" }
    val subtitle = when (quadrant) { Quadrant.DO_NOW -> "Khẩn & Quan trọng"; Quadrant.PLAN -> "Quan trọng, Không khẩn"; Quadrant.DELEGATE -> "Khẩn, Không quan trọng"; Quadrant.ELIMINATE -> "Không khẩn & Không quan trọng" }
    val icon = when (quadrant) { Quadrant.DO_NOW -> Icons.Filled.FlashOn; Quadrant.PLAN -> Icons.Filled.ListAlt; Quadrant.DELEGATE -> Icons.Filled.PersonAdd; Quadrant.ELIMINATE -> Icons.Filled.Block }
    val accentColor = when (quadrant) { Quadrant.DO_NOW -> Color(0xFFD32F2F); Quadrant.PLAN -> Color(0xFF1976D2); Quadrant.DELEGATE -> Color(0xFFE65100); Quadrant.ELIMINATE -> Color(0xFF757575) }
    val lightBgColor = when (quadrant) { Quadrant.DO_NOW -> Color(0xFFFFEBEE); Quadrant.PLAN -> Color(0xFFE3F2FD); Quadrant.DELEGATE -> Color(0xFFFFF3E0); Quadrant.ELIMINATE -> Color(0xFFF5F5F5) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(lightBgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.background(lightBgColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("${tasks.size} task", style = MaterialTheme.typography.bodySmall, color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có task nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDelete(task)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                        MaterialTheme.colorScheme.errorContainer
                                    else Color.Transparent,
                                    animationSpec = tween(200), label = "SwipeBg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 8.dp)
                                        .background(color, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Xoá", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).alpha(if (task.isCompleted) 0.6f else 1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleStatus(task) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            uncheckedColor = accentColor
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (task.dueDate != null) {
                                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFD32F2F))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                Text(sdf.format(Date(task.dueDate!!)), style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                                            } else {
                                                Text("Không có deadline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
