package com.phuc.synctask.ui.personal

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.model.quadrant
import com.phuc.synctask.viewmodel.HomeUiState
import com.phuc.synctask.viewmodel.HomeViewModel
import com.phuc.synctask.ui.common.AnimatedLoadingScreen
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PersonalTaskScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToQuadrant: (Quadrant) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val todayCount by viewModel.todayTasksCount.collectAsState()
    val overdueCount by viewModel.overdueTasksCount.collectAsState()
    val completedCount by viewModel.completedTasksCount.collectAsState()

    val isLoading = uiState is HomeUiState.Loading

    // Crossfade mượt mà giữa loading và nội dung (300ms)
    Crossfade(
        targetState = isLoading,
        animationSpec = tween(durationMillis = 300),
        label = "personal_loading"
    ) { loading ->
        if (loading) {
            AnimatedLoadingScreen(message = "Đang đồng bộ công việc...")
            return@Crossfade
        }

        val q1Tasks = tasks.filter { it.quadrant() == Quadrant.DO_NOW }
        val q2Tasks = tasks.filter { it.quadrant() == Quadrant.PLAN }
        val q3Tasks = tasks.filter { it.quadrant() == Quadrant.DELEGATE }
        val q4Tasks = tasks.filter { it.quadrant() == Quadrant.ELIMINATE }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Row 3 Card thống kê
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Hôm nay", todayCount.toString(), Modifier.weight(1f))
                StatCard("Quá hạn", overdueCount.toString(), Modifier.weight(1f))
                StatCard("Hoàn thành", completedCount.toString(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ma trận Ưu tiên
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriorityCard(
                        title = "Làm Ngay",
                        subtitle = "Khẩn & Quan trọng",
                        containerColor = Color(0xFFFFCDD2),
                        icon = Icons.Filled.FlashOn,
                        count = q1Tasks.size,
                        accentColor = Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onNavigateToQuadrant(Quadrant.DO_NOW) }
                    )
                    PriorityCard(
                        title = "Lên Kế Hoạch",
                        subtitle = "Quan trọng, Không khẩn",
                        containerColor = Color(0xFFBBDEFB),
                        icon = Icons.Filled.ListAlt,
                        count = q2Tasks.size,
                        accentColor = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onNavigateToQuadrant(Quadrant.PLAN) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriorityCard(
                        title = "Ủy Quyền",
                        subtitle = "Khẩn, Không quan trọng",
                        containerColor = Color(0xFFFFE0B2),
                        icon = Icons.Filled.PersonAdd,
                        count = q3Tasks.size,
                        accentColor = Color(0xFFE65100),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onNavigateToQuadrant(Quadrant.DELEGATE) }
                    )
                    PriorityCard(
                        title = "Loại Bỏ",
                        subtitle = "Không khẩn & Không quan trọng",
                        containerColor = Color(0xFFEEEEEE),
                        icon = Icons.Filled.Block,
                        count = q4Tasks.size,
                        accentColor = Color(0xFF757575),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onNavigateToQuadrant(Quadrant.ELIMINATE) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatCard(title: String, count: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    icon: ImageVector,
    count: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Trong dark mode, pha màu pastel với surface để không bị chói
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.05f
    val adaptedColor = if (isDark)
        MaterialTheme.colorScheme.surfaceVariant
    else
        containerColor

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = adaptedColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isDark) MaterialTheme.colorScheme.surface
                            else Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(26.dp),
                        tint = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxHeight()) {
                    Text(
                        text = count.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                    Text(
                        text = " task",
                        fontSize = 13.sp,
                        color = accentColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}
