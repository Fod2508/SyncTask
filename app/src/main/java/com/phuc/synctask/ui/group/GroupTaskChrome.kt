package com.phuc.synctask.ui.group

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTaskTopBar(
    groupName: String,
    isOwner: Boolean,
    onBack: () -> Unit,
    onShowLeaveDialog: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = groupName,
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
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isOwner) "Giải tán nhóm" else "Rời nhóm",
                            color = GroupTaskUiTokens.DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isOwner) Icons.Filled.DeleteOutline else Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = GroupTaskUiTokens.DangerRed
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onShowLeaveDialog()
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GroupTaskUiTokens.IndigoPrimary)
    )
}

@Composable
fun GroupTaskFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = GroupTaskUiTokens.IndigoPrimary,
        contentColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Thêm task nhóm")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Task Mới", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GroupTaskConfettiOverlay(show: Boolean) {
    if (!show) return

    val partyLeft = Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        angle = 45,
        spread = 45,
        colors = GroupTaskUiTokens.ConfettiColors,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
        position = Position.Relative(0.0, 0.5)
    )
    val partyRight = Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        angle = 135,
        spread = 45,
        colors = GroupTaskUiTokens.ConfettiColors,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
        position = Position.Relative(1.0, 0.5)
    )
    KonfettiView(
        parties = listOf(partyLeft, partyRight),
        modifier = Modifier.fillMaxSize()
    )
}
