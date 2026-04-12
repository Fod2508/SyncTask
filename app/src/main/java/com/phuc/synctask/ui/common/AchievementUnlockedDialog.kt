package com.phuc.synctask.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phuc.synctask.R
import com.phuc.synctask.utils.AchievementManager
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// Map achievementId → tên hiển thị + mô tả
private fun achievementInfo(id: String): Pair<String, String> = when (id) {
    AchievementManager.ROOKIE_BADGE     -> "Khởi đầu tốt"    to "Bạn đã hoàn thành task đầu tiên!"
    AchievementManager.DILIGENT_BADGE   -> "Siêng năng"       to "Hoàn thành 10 task — thật tuyệt vời!"
    AchievementManager.WARRIOR_BADGE    -> "Chiến binh"       to "50 task đã xong — bạn thật kiên cường!"
    AchievementManager.LEGEND_BADGE     -> "Huyền thoại"      to "200 task! Bạn là huyền thoại thực sự!"
    AchievementManager.NIGHT_OWL_BADGE  -> "Cú đêm"           to "Làm việc lúc 1–5 giờ sáng — chăm quá!"
    AchievementManager.ON_TIME_BADGE    -> "Đúng hạn"         to "Hoàn thành trước deadline — chuẩn không cần chỉnh!"
    AchievementManager.TEAM_PLAYER_BADGE -> "Team Player"     to "Hoàn thành 5 task nhóm — đồng đội tuyệt vời!"
    AchievementManager.CAPTAIN_BADGE    -> "Captain"          to "Trưởng nhóm gương mẫu — dẫn đầu bằng hành động!"
    else                                -> "Thành tựu mới"    to "Bạn vừa mở khóa một huy hiệu mới!"
}

@Composable
fun AchievementUnlockedDialog(
    achievementId: String,
    onDismiss: () -> Unit
) {
    val (name, description) = remember(achievementId) { achievementInfo(achievementId) }

    // Spring scale: bắt đầu từ 0f → 1f với hiệu ứng nảy
    var scaleTarget by remember { mutableStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "achievement_scale"
    )
    LaunchedEffect(Unit) { scaleTarget = 1f }

    // Konfetti parties
    val parties = listOf(
        Party(
            speed = 0f, maxSpeed = 35f, damping = 0.9f,
            angle = 45, spread = 60,
            colors = listOf(0xFF4B3FBE.toInt(), 0xFFFFD700.toInt(), 0xFF22C55E.toInt(), 0xFFEF4444.toInt()),
            emitter = Emitter(150, TimeUnit.MILLISECONDS).max(120),
            position = Position.Relative(0.0, 0.4)
        ),
        Party(
            speed = 0f, maxSpeed = 35f, damping = 0.9f,
            angle = 135, spread = 60,
            colors = listOf(0xFF4B3FBE.toInt(), 0xFFFFD700.toInt(), 0xFF22C55E.toInt(), 0xFFEF4444.toInt()),
            emitter = Emitter(150, TimeUnit.MILLISECONDS).max(120),
            position = Position.Relative(1.0, 0.4)
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pháo hoa phía sau card
            KonfettiView(
                parties = parties,
                modifier = Modifier.matchParentSize()
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emoji trang trí phía trên
                    Text("🌟 🎉 ✨", fontSize = 22.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hình bé mập với spring scale
                    Image(
                        painter = painterResource(id = R.drawable.ic_achievement_unlocked),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .scale(scale)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "CHÚC MỪNG!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "🏅 $name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Tuyệt vời! 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
