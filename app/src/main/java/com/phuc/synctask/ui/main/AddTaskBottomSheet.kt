package com.phuc.synctask.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phuc.synctask.model.FirebaseTask
import com.phuc.synctask.model.Quadrant
import java.text.SimpleDateFormat
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

            // ⑤ Nút chọn deadline
            val dateText = if (dueDate != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

            Spacer(modifier = Modifier.height(24.dp))

            // ⑥ Nút Lưu Công Việc
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

    // DatePicker dialog (Material 3)
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            },
            text = { DatePicker(state = datePickerState) }
        )
    }
}
