package com.phuc.synctask.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuc.synctask.R

// ─── Model ───────────────────────────────────────────────────────
data class TutorialStep(
    val spotlightOffset: Offset,
    val spotlightSize: Size,
    val title: String,
    val isEisenhowerStep: Boolean = false
)

// ─── Overlay chính ────────────────────────────────────────────────
@Composable
fun SpotlightOverlay(
    steps: List<TutorialStep>,
    currentStep: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep < 0 || currentStep >= steps.size) return
    val step = steps[currentStep]

    val animOffset by animateOffsetAsState(
        targetValue = step.spotlightOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "spotlight_offset"
    )
    val animSize by animateSizeAsState(
        targetValue = step.spotlightSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "spotlight_size"
    )

    val density = LocalDensity.current
    val config  = LocalConfiguration.current
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }

    // Spotlight ở nửa dưới màn hình → tooltip hiện phía trên
    val spotlightMidY = animOffset.y + animSize.height / 2f
    val tooltipAbove  = spotlightMidY > screenH * 0.55f

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Lớp tối + lỗ sáng ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* chặn click xuyên qua */ }
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.78f))
            drawRoundRect(
                color        = Color.Black,
                topLeft      = animOffset,
                size         = animSize,
                cornerRadius = CornerRadius(24f, 24f),
                blendMode    = BlendMode.Clear
            )
        }

        // ── Tooltip ──
        TooltipCard(
            step         = step,
            currentStep  = currentStep,
            totalSteps   = steps.size,
            above        = tooltipAbove,
            spotlightY   = with(density) { animOffset.y.toDp() },
            spotlightBottomY = with(density) { (animOffset.y + animSize.height).toDp() },
            onNext       = onNext,
            onSkip       = onSkip
        )
    }
}

@Composable
private fun TooltipCard(
    step: TutorialStep,
    currentStep: Int,
    totalSteps: Int,
    above: Boolean,
    spotlightY: Dp,
    spotlightBottomY: Dp,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = if (above) Alignment.TopCenter else Alignment.TopCenter
    ) {
        val topPad = if (above) {
            // Tooltip phía trên spotlight — đặt cách đỉnh màn hình 24dp
            24.dp
        } else {
            // Tooltip phía dưới spotlight
            (spotlightBottomY + 16.dp).coerceAtLeast(200.dp)
        }

        Surface(
            shape         = RoundedCornerShape(24.dp),
            color         = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(top = topPad)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tutorial_rocket),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text       = step.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (step.isEisenhowerStep) {
                    EisenhowerExplanation()
                } else {
                    StepDescription(currentStep)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(totalSteps) { i ->
                        val dotColor = if (i == currentStep)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(dotColor) }
                        if (i < totalSteps - 1) Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Bỏ qua", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onNext,   // ← gọi thẳng lambda, không có logic nào ở đây
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        val label = if (currentStep == totalSteps - 1) "Hoàn tất 🎉" else "Tiếp theo →"
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Bước 1: Eisenhower ───────────────────────────────────────────
@Composable
private fun EisenhowerExplanation() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text      = "Phân loại công việc theo Khẩn cấp & Quan trọng:",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        EisenhowerRow("🔴 Làm ngay",     "Vừa gấp vừa quan trọng — làm ngay!",         Color(0xFFD32F2F))
        EisenhowerRow("🟡 Lên kế hoạch", "Quan trọng, chưa gấp — lên lịch làm sau.",   Color(0xFFF9A825))
        EisenhowerRow("🔵 Ủy quyền",     "Gấp nhưng không quan trọng — nhờ người khác.",Color(0xFF1976D2))
        EisenhowerRow("⚫ Loại bỏ",       "Không gấp, không quan trọng — xóa bỏ.",      Color(0xFF757575))
    }
}

@Composable
private fun EisenhowerRow(label: String, desc: String, color: Color) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = color,
            modifier   = Modifier.width(116.dp)
        )
        Text(
            text     = desc,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Bước 2 & 3 ──────────────────────────────────────────────────
@Composable
private fun StepDescription(step: Int) {
    val text = when (step) {
        1    -> "Kết nối, tạo dự án và giao việc cho thành viên trong nhóm theo thời gian thực."
        2    -> "Hoàn thành nhiệm vụ để nhận huy hiệu cực phẩm. Càng nhiều task xong, tên lửa càng bay xa!"
        else -> ""
    }
    if (text.isNotEmpty()) {
        Text(
            text      = text,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
