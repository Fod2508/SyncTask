package com.phuc.synctask.ui.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuc.synctask.R

// ─── Model ───────────────────────────────────────────────────────
/**
 * Mỗi bước tutorial mô tả vùng cần spotlight (Rect tuyệt đối từ boundsInRoot)
 * và nội dung tooltip.
 *
 * @param targetBounds  Rect pixel tuyệt đối (boundsInRoot) của vùng cần chiếu sáng.
 *                      null = chưa đo được → dùng fallback toàn màn hình.
 * @param padding       Padding thêm vào xung quanh vùng sáng (px).
 * @param title         Tiêu đề tooltip.
 * @param iconRes       Drawable resource cho icon trong tooltip.
 * @param isEisenhowerStep  true → hiển thị bảng giải thích Eisenhower.
 * @param tooltipBelow  true → tooltip hiện bên dưới vùng sáng, false → bên trên.
 */
data class TutorialStep(
    val targetBounds: Rect?,
    val padding: Float = 50f,
    val title: String,
    val iconRes: Int = R.drawable.ic_tutorial_rocket,
    val isEisenhowerStep: Boolean = false,
    val tooltipBelow: Boolean = false
)

// ─── Overlay chính ────────────────────────────────────────────────
@Composable
fun SpotlightOverlay(
    steps: List<TutorialStep>,
    currentStep: Int,
    screenSize: Size,           // kích thước màn hình tính bằng px (từ LocalDensity)
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep < 0 || currentStep >= steps.size) return
    val step = steps[currentStep]

    // Tính Rect spotlight có padding, fallback = toàn màn hình
    val rawRect = step.targetBounds ?: Rect(
        left   = 0f,
        top    = 0f,
        right  = screenSize.width,
        bottom = screenSize.height
    )
    val spotRect = Rect(
        left   = (rawRect.left   - step.padding).coerceAtLeast(0f),
        top    = (rawRect.top    - step.padding).coerceAtLeast(0f),
        right  = (rawRect.right  + step.padding).coerceAtMost(screenSize.width),
        bottom = (rawRect.bottom + step.padding).coerceAtMost(screenSize.height)
    )

    // Animate các cạnh của Rect để chuyển mượt giữa các bước
    val animLeft   by animateFloatAsState(spotRect.left,   spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sl")
    val animTop    by animateFloatAsState(spotRect.top,    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "st")
    val animRight  by animateFloatAsState(spotRect.right,  spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sr")
    val animBottom by animateFloatAsState(spotRect.bottom, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sb")

    val density = LocalDensity.current

    // Tooltip bên dưới hay bên trên?
    // Nếu step.tooltipBelow = true → luôn hiện bên dưới (ưu tiên tuyệt đối, dùng cho tab bottom bar)
    // Nếu false → tự động: spotlight ở nửa trên → tooltip bên dưới; nửa dưới → bên trên
    val tooltipBelow: Boolean = if (step.tooltipBelow) {
        true
    } else {
        animTop < screenSize.height * 0.45f
    }
    val spotlightBottomDp: Dp = with(density) { animBottom.toDp() }
    val spotlightTopDp: Dp    = with(density) { animTop.toDp() }

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
            // Nền tối
            drawRect(color = Color.Black.copy(alpha = 0.78f))
            // Đục lỗ sáng — dùng boundsInRoot nên tọa độ đã tuyệt đối
            drawRoundRect(
                color        = Color.Black,
                topLeft      = Offset(animLeft, animTop),
                size         = Size(animRight - animLeft, animBottom - animTop),
                cornerRadius = CornerRadius(24f, 24f),
                blendMode    = BlendMode.Clear
            )
        }

        // ── Tooltip ──
        TooltipCard(
            step             = step,
            currentStep      = currentStep,
            totalSteps       = steps.size,
            tooltipBelow     = tooltipBelow,
            spotlightTopDp   = spotlightTopDp,
            spotlightBottomDp = spotlightBottomDp,
            onNext           = onNext,
            onSkip           = onSkip
        )
    }
}

@Composable
private fun TooltipCard(
    step: TutorialStep,
    currentStep: Int,
    totalSteps: Int,
    tooltipBelow: Boolean,
    spotlightTopDp: Dp,
    spotlightBottomDp: Dp,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val topPad = if (tooltipBelow) {
            // Tooltip bên dưới vùng sáng
            (spotlightBottomDp + 16.dp).coerceAtLeast(120.dp)
        } else {
            // Tooltip bên trên vùng sáng — đặt cách đỉnh màn hình 24dp
            24.dp
        }

        Surface(
            shape          = RoundedCornerShape(24.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier       = Modifier
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
                    painter            = painterResource(id = step.iconRes),
                    contentDescription = null,
                    modifier           = Modifier.size(72.dp)
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
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Bỏ qua", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onNext,
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

// ─── Bước Eisenhower ─────────────────────────────────────────────
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
        EisenhowerRow("🔴 Làm ngay",     "Vừa gấp vừa quan trọng — làm ngay!",          Color(0xFFD32F2F))
        EisenhowerRow("🟡 Lên kế hoạch", "Quan trọng, chưa gấp — lên lịch làm sau.",    Color(0xFFF9A825))
        EisenhowerRow("🔵 Ủy quyền",     "Gấp nhưng không quan trọng — nhờ người khác.", Color(0xFF1976D2))
        EisenhowerRow("⚫ Loại bỏ",       "Không gấp, không quan trọng — xóa bỏ.",       Color(0xFF757575))
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

// ─── Mô tả từng bước ─────────────────────────────────────────────
@Composable
private fun StepDescription(step: Int) {
    val text = when (step) {
        1 -> "Kết nối, tạo dự án và giao việc cho thành viên trong nhóm theo thời gian thực."
        2 -> "Hoàn thành nhiệm vụ để nhận huy hiệu cực phẩm. Càng nhiều task xong, tên lửa càng bay xa!"
        3 -> "Tổng quan Dashboard: Theo dõi thống kê công việc hôm nay và trạng thái hoàn thành của bạn tại đây."
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
