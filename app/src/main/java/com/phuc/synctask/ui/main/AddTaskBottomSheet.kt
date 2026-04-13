package com.phuc.synctask.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Quadrant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    initialQuadrant: Quadrant? = null,
    onDismiss: () -> Unit,
    onSave: (FirebaseTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(initialQuadrant == Quadrant.DO_NOW || initialQuadrant == Quadrant.DELEGATE) }
    var isImportant by remember { mutableStateOf(initialQuadrant == Quadrant.DO_NOW || initialQuadrant == Quadrant.PLAN) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val datePickerState = rememberDatePickerState()

    // Text + màu tự động theo ma trận Eisenhower
    val (quadrantLabel, quadrantColor) = when {
        isUrgent && isImportant   -> "→ Làm Ngay (Đỏ)" to Color(0xFFD32F2F)
        !isUrgent && isImportant  -> "→ Lên Kế Hoạch (Xanh)" to Color(0xFF1976D2)
        isUrgent && !isImportant  -> "→ Ủy Quyền (Vàng)" to Color(0xFFE65100)
        else                      -> "→ Loại Bỏ (Xám)" to Color(0xFF757575)
    }

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
                text = "Thêm công việc mới",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ① Tên công việc
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tên công việc") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ② Mô tả chi tiết
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Mô tả chi tiết") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ③ Hai FilterChip nằm ngang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = isUrgent,
                    onClick = { isUrgent = !isUrgent },
                    label = { Text("🔥 Khẩn cấp") }
                )
                FilterChip(
                    selected = isImportant,
                    onClick = { isImportant = !isImportant },
                    label = { Text("⭐ Quan trọng") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ④ Text tự động cập nhật theo state
            Text(
                text = quadrantLabel,
                style = MaterialTheme.typography.bodySmall,
                color = quadrantColor,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ⑤ Nút chọn deadline (hiển thị cả ngày và giờ)
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

            // ⑥ Hiển thị row giờ cụ thể đã chọn (icon đồng hồ)
            if (dueDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val timeSdf = SimpleDateFormat("HH:mm, dd 'Th'MM", Locale.getDefault())
                    Text(
                        text = "Deadline: ${timeSdf.format(Date(dueDate!!))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ⑦ Nút Lưu Công Việc
            Button(
                onClick = {
                    val task = FirebaseTask(
                        title = title.trim(),
                        description = description.trim(),
                        isUrgent = isUrgent,
                        isImportant = isImportant,
                        dueDate = dueDate
                    )
                    onSave(task)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = title.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Lưu Công Việc", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // DatePicker dialog (Material 3) — sau khi chọn ngày, mở TimePicker
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        // Lưu tạm ngày + giờ mặc định, sau đó mở TimePicker để user chỉnh giờ
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                        cal.set(Calendar.MINUTE, selectedMinute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
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

    // TimePicker dialog — chọn giờ cụ thể cho deadline
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
                    // Gộp ngày đã chọn + giờ mới của user
                    dueDate?.let { existingMillis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = existingMillis }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        dueDate = cal.timeInMillis
                    }
                    showTimePicker = false
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Hủy") }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chọn giờ deadline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}
