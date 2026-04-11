package com.phuc.synctask.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.model.Group
import com.phuc.synctask.viewmodel.GroupUiState
import com.phuc.synctask.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    viewModel: GroupViewModel = viewModel(),
    onNavigateToGroup: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tạo Nhóm")
            }

            OutlinedButton(
                onClick = { showJoinDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tham Gia")
            }
        }

        // List Content
        when (val state = uiState) {
            is GroupUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GroupUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GroupUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bạn chưa tham gia nhóm nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is GroupUiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.groups) { group ->
                        GroupCard(group = group, onClick = { onNavigateToGroup(group.id) })
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            viewModel = viewModel,
            onDismiss = { showJoinDialog = false }
        )
    }
}

@Composable
fun GroupCard(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = group.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${group.members.size} thành viên",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Mã mời: ${group.inviteCode}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(viewModel: GroupViewModel, onDismiss: () -> Unit) {
    var groupName by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhóm mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Tên nhóm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (resultMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(resultMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createGroup(groupName) { success, msg ->
                            resultMessage = msg
                            if (success) {
                                // Wait a bit then dismiss
                                onDismiss()
                            }
                        }
                    }
                }
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupDialog(viewModel: GroupViewModel, onDismiss: () -> Unit) {
    var inviteCode by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tham gia nhóm") },
        text = {
            Column {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase() },
                    label = { Text("Nhập mã mời (6 ký tự)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (resultMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resultMessage!!,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inviteCode.isNotBlank()) {
                        viewModel.joinGroup(inviteCode) { success, msg ->
                            isError = !success
                            resultMessage = msg
                            if (success) {
                                onDismiss()
                            }
                        }
                    }
                }
            ) {
                Text("Vào nhóm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
