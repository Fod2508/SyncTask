package com.phuc.synctask.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.viewmodel.DashboardViewModel
import com.phuc.synctask.viewmodel.DailyWorkload
import com.phuc.synctask.viewmodel.GroupProgressData
import com.phuc.synctask.viewmodel.FocusTask
import com.phuc.synctask.viewmodel.DashboardFilter
import com.phuc.synctask.viewmodel.EisenhowerData
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// ─── Design Tokens ───
private val ScreenBackground = Color(0xFFF8FAFC)
private val CardBackground = Color.White
private val PersonalBlue = Color(0xFF3B82F6)
private val PersonalBlueLight = Color(0xFFEFF6FF)
private val GroupOrange = Color(0xFFF97316)
private val GroupOrangeLight = Color(0xFFFFF7ED)
private val SubtleText = Color(0xFF64748B)
private val TextPrimary = Color(0xFF0F172A)

// Mảng màu cho Donut Chart
private val DonutColors = listOf(
    Color(0xFFEF4444), // Do Now - Đỏ
    Color(0xFF3B82F6), // Plan - Xanh
    Color(0xFFF59E0B), // Delegate - Vàng
    Color(0xFF94A3B8)  // Eliminate - Xám
)
private val DonutLabels = listOf("Làm Ngay", "Lên Kế Hoạch", "Ủy Quyền", "Loại Bỏ")

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val scrollState = rememberScrollState()
    
    val personalCompleted by viewModel.personalCompleted.collectAsState()
    val groupCompleted by viewModel.groupCompleted.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val eisenhowerStats by viewModel.eisenhowerStats.collectAsState()
    val weeklyWorkload by viewModel.weeklyWorkload.collectAsState()
    val groupProgress by viewModel.groupProgress.collectAsState()
    val pendingFocusTasks by viewModel.pendingFocusTasks.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp) // padding để không bị che bởi BottomBar
    ) {
        // ─── 1. Header & Tổng quan ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Thống kê",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Box {
                OutlinedButton(
                    onClick = { showFilterMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (filterType == DashboardFilter.WEEK) "Tuần này" else "Tháng này", color = TextPrimary)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tuần này") },
                        onClick = {
                            viewModel.setFilter(DashboardFilter.WEEK)
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tháng này") },
                        onClick = {
                            viewModel.setFilter(DashboardFilter.MONTH)
                            showFilterMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(
                title = "Cá nhân",
                value = personalCompleted.toString(),
                bgColor = PersonalBlueLight,
                contentColor = PersonalBlue,
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                title = "Nhóm",
                value = groupCompleted.toString(),
                bgColor = GroupOrangeLight,
                contentColor = GroupOrange,
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                title = "Quá hạn",
                value = overdueCount.toString(),
                bgColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── 2. Tầng 1: Biểu đồ Cân bằng Công việc (Stacked Bar) ───
        Text(
            "Cân bằng Công việc",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        StackedBarChart(weeklyWorkload = weeklyWorkload, modifier = Modifier.fillMaxWidth().height(200.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // ─── 3. Tầng 2: Phân bổ Cá nhân (Eisenhower Donut Chart) ───
        Text(
            "Phân bổ Cá nhân (Eisenhower)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        DonutChartCard(eisenhowerStats = eisenhowerStats, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // ─── 4. Tầng 3: Tiến độ Dự án (Group Progress) ───
        Text(
            "Tiến độ Dự án",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        GroupProgressList(progressList = groupProgress)

        Spacer(modifier = Modifier.height(32.dp))

        // ─── 5. Tầng 4: Tiêu điểm cần xử lý ───
        Text(
            "Tiêu điểm cần xử lý",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        FocusTasksList(focusTasks = pendingFocusTasks)
    }
}

// ─────────────────────────────────────────────────────────────────
// Các Component Sub-UI
// ─────────────────────────────────────────────────────────────────

@Composable
fun OverviewCard(title: String, value: String, bgColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun StackedBarChart(weeklyWorkload: List<DailyWorkload>, modifier: Modifier = Modifier) {
    if (weeklyWorkload.isEmpty()) return

    val maxCount = weeklyWorkload.maxOf { it.personalCount + it.groupCount + it.overdueCount }.coerceAtLeast(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyWorkload.forEach { day ->
                val personalRatio = day.personalCount.toFloat() / maxCount
                val groupRatio = day.groupCount.toFloat() / maxCount
                val overdueRatio = day.overdueCount.toFloat() / maxCount

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .weight(1f, fill = false),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(verticalArrangement = Arrangement.Bottom) {
                            // Cột Quá hạn (Đỏ) đặt trên cùng
                            if (day.overdueCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(overdueRatio + groupRatio + personalRatio)
                                        .background(
                                            Color(0xFFEF4444),
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                            // Cột Nhóm (Cam)
                            if (day.groupCount > 0 || day.personalCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(groupRatio + personalRatio)
                                        .background(
                                            GroupOrange,
                                            shape = if (day.overdueCount == 0) 
                                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp) 
                                            else 
                                                RoundedCornerShape(0.dp)
                                        )
                                )
                            }
                            // Cột Cá nhân (Xanh) đặt dưới cùng
                            if (day.personalCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(personalRatio)
                                        .background(
                                            PersonalBlue,
                                            shape = if (day.groupCount == 0 && day.overdueCount == 0)
                                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                            else
                                                RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtleText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChartCard(eisenhowerStats: List<EisenhowerData>, modifier: Modifier = Modifier) {
    val totalTasks = eisenhowerStats.sumOf { it.totalCount }
    val completedTasks = eisenhowerStats.sumOf { it.completedCount }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Biểu đồ Canvas
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 36f
                    if (totalTasks == 0) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            radius = (size.minDimension - strokeWidth) / 2f,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                            )
                        )
                    } else {
                        var startAngle = -90f
                        
                        eisenhowerStats.forEachIndexed { index, stat ->
                            val ratio = stat.totalCount.toFloat() / totalTasks
                            if (ratio > 0f) {
                                val sweepAngle = ratio * 360f
                                
                                drawArc(
                                    color = DonutColors[index].copy(alpha = 0.2f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 3f, // Trừ 3f tạo khoảng trống nhỏ
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                    size = Size(size.width, size.height)
                                )
                                
                                if (stat.completedCount > 0) {
                                    val completedSweepAngle = (stat.completedCount.toFloat() / stat.totalCount) * (sweepAngle - 3f)
                                    drawArc(
                                        color = DonutColors[index],
                                        startAngle = startAngle,
                                        sweepAngle = completedSweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                        size = Size(size.width, size.height)
                                    )
                                }
                                startAngle += sweepAngle
                            }
                        }
                    }
                }
                
                // Text ở giữa
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (totalTasks == 0) {
                        Text(
                            text = "0 / 0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SubtleText
                        )
                    } else {
                        Text(
                            text = "$completedTasks / $totalTasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Tiến độ",
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtleText
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Chú thích (Legend)
            Column {
                DonutLabels.forEachIndexed { index, label ->
                    val stat = eisenhowerStats.getOrNull(index)
                    val labelText = if (stat != null) "$label: ${stat.completedCount}/${stat.totalCount}" else label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(DonutColors[index], CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupProgressList(progressList: List<GroupProgressData>) {
    if (progressList.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bạn chưa tham gia dự án nào",
                    color = SubtleText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        progressList.forEach { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.groupName,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(p.progress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = GroupOrange
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { p.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = GroupOrange,
                        trackColor = GroupOrangeLight,
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Bạn đóng góp: ${p.userContributions} task",
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtleText
                    )
                }
            }
        }
    }
}

@Composable
fun FocusTasksList(focusTasks: List<FocusTask>) {
    if (focusTasks.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mọi thứ đã hoàn thành! 🌟",
                    color = Color(0xFF22C55E),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        focusTasks.forEach { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = task.origin,
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtleText
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (task.isOverdue) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = task.deadlineStatus,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (task.isOverdue) Color(0xFFD32F2F) else Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

