package com.phuc.synctask.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuc.synctask.R

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")

    // Hiệu ứng "thở" — scale 1.0 → 1.06 → 1.0
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_welcome_read),
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .scale(breatheScale)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Làm chủ thời gian\ncùng SyncTask",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chào mừng bạn! Hãy cùng khám phá phương pháp quản lý công việc khoa học để tối ưu hóa năng suất mỗi ngày.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Bắt đầu ngay 🚀",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}
