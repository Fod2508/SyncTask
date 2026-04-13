package com.phuc.synctask.ui.group

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuc.synctask.R
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
                EmptyGroupState()
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
                                com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.AUTH_SUCCESS)
                                onDismiss()
                            } else {
                                com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.ERROR)
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
                                com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.AUTH_SUCCESS)
                                onDismiss()
                            } else {
                                com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.ERROR)
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

@Composable
private fun EmptyGroupState() {
    val infiniteTransition = rememberInfiniteTransition(label = "hover")

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_group),
            contentDescription = null,
            modifier = Modifier
                .size(250.dp)
                .offset(y = hoverOffset.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Dự án nhóm đang trống",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tạo nhóm mới hoặc tham gia nhóm bằng mã mời để bắt đầu.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
