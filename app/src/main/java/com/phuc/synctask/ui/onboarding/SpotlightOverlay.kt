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
import androidx.compose.ui.unit.dp
import com.phuc.synctask.R

// ─── Metadata cho mỗi bước ───────────────────────────────────────
/**
 * Mô tả nội dung tooltip của một bước tutorial.
 * Tọa độ spotlight được truyền riêng qua [currentTargetBounds].
 */
data class TutorialStepMeta(
    val title: String,
    val description: String,
    val iconRes: Int = R.drawable.ic_welcome_rocket
)

// Danh sách metadata 4 bước — Single Source of Truth
val TUTORIAL_STEPS: List<TutorialStepMeta> = listOf(
    TutorialStepMeta(
        title       = "Tab Cá nhân",
        description = "Quản lý công việc cá nhân theo Ma trận Eisenhower — phân loại theo mức độ Khẩn cấp & Quan trọng để ưu tiên đúng việc.",
        iconRes     = R.drawable.ic_welcome_rocket
    ),
    TutorialStepMeta(
        title       = "Tab Nhóm",
        description = "Kết nối, tạo dự án và giao việc cho thành viên trong nhóm theo thời gian thực.",
        iconRes     = R.drawable.ic_welcome_rocket
    ),
    TutorialStepMeta(
        title       = "Tab Thành tựu",
        description = "Hoàn thành nhiệm vụ để nhận huy hiệu cực phẩm. Càng nhiều task xong, tên lửa càng bay xa!",
        iconRes     = R.drawable.ic_welcome_rocket
    ),
    TutorialStepMeta(
        title       = "Tổng quan Dashboard",
        description = "Theo dõi thống kê công việc hôm nay và trạng thái hoàn thành của bạn tại đây.",
        iconRes     = R.drawable.ic_welcome_rocket
    )
)

// ─── Overlay chính ────────────────────────────────────────────────
/**
 * Spotlight overlay responsive 100%.
 *
 * @param currentTargetBounds  Rect tuyệt đối (boundsInRoot) của vùng cần chiếu sáng.
 *                             null → không vẽ lỗ (chờ bounds được đo).
 * @param currentStep          Index bước hiện tại (0-based).
 * @param totalSteps           Tổng số bước.
 * @param screenSize           Kích thước màn hình px.
 * @param spotlightPadding     Padding px thêm vào xung quanh bounds.
 * @param onNext               Callback khi bấm "Tiếp theo" / "Hoàn tất".
 * @param onSkip               Callback khi bấm "Bỏ qua".
 */
@Composable
fun SpotlightOverlay(
    currentTargetBounds: Rect?,
    currentStep: Int,
    totalSteps: Int,
    screenSize: Size,
    spotlightPadding: Float = 20f,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (currentStep < 0 || currentStep >= totalSteps) return

    val meta = TUTORIAL_STEPS.getOrNull(currentStep) ?: return

    // Tính Rect spotlight có padding
    val spotRect: Rect? = currentTargetBounds?.let { raw ->
        Rect(
            left   = (raw.left   - spotlightPadding).coerceAtLeast(0f),
            top    = (raw.top    - spotlightPadding).coerceAtLeast(0f),
            right  = (raw.right  + spotlightPadding).coerceAtMost(screenSize.width),
            bottom = (raw.bottom + spotlightPadding).coerceAtMost(screenSize.height)
        )
    }

    // Fallback khi chưa có bounds: dùng toàn màn hình (overlay tối, chưa đục lỗ)
    val safeRect = spotRect ?: Rect(0f, 0f, screenSize.width, screenSize.height)

    // Animate 4 cạnh riêng biệt → chuyển bước mượt
    val animLeft   by animateFloatAsState(safeRect.left,   spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sl")
    val animTop    by animateFloatAsState(safeRect.top,    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "st")
    val animRight  by animateFloatAsState(safeRect.right,  spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sr")
    val animBottom by animateFloatAsState(safeRect.bottom, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "sb")

    val density = LocalDensity.current

    // Vùng sáng ở nửa dưới màn hình → tooltip lên trên; nửa trên → tooltip xuống dưới
    val targetMidY   = (animTop + animBottom) / 2f
    val tooltipAbove = targetMidY > screenSize.height * 0.5f

    val spotlightTopDp    = with(density) { animTop.toDp() }
    val spotlightBottomDp = with(density) { animBottom.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Lớp tối + đục lỗ sáng ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* chặn click xuyên qua */ }
        ) {
            // Nền tối
            drawRect(color = Color.Black.copy(alpha = 0.75f))

            // Chỉ đục lỗ khi đã có bounds thực (không phải fallback toàn màn hình)
            if (currentTargetBounds != null) {
                val cx     = (animLeft + animRight) / 2f
                val cy     = (animTop  + animBottom) / 2f
                val halfW  = (animRight - animLeft) / 2f
                val halfH  = (animBottom - animTop) / 2f
                val radius = maxOf(halfW, halfH)

                // Dùng drawRoundRect để lỗ sáng có bo góc đẹp
                drawRoundRect(
                    color        = Color.Black,
                    topLeft      = Offset(animLeft, animTop),
                    size         = Size(animRight - animLeft, animBottom - animTop),
                    cornerRadius = CornerRadius(radius * 0.25f, radius * 0.25f),
                    blendMode    = BlendMode.Clear
                )
            }
        }

        // ── Tooltip card ──
        TooltipCard(
            meta              = meta,
            currentStep       = currentStep,
            totalSteps        = totalSteps,
            tooltipAbove      = tooltipAbove,
            spotlightTopDp    = spotlightTopDp,
            spotlightBottomDp = spotlightBottomDp,
            onNext            = onNext,
            onSkip            = onSkip
        )
    }
}

// ─── Tooltip Card ─────────────────────────────────────────────────
@Composable
private fun TooltipCard(
    meta: TutorialStepMeta,
    currentStep: Int,
    totalSteps: Int,
    tooltipAbove: Boolean,      // true = tooltip nằm phía trên vùng sáng
    spotlightTopDp: androidx.compose.ui.unit.Dp,
    spotlightBottomDp: androidx.compose.ui.unit.Dp,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier          = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentAlignment  = Alignment.TopCenter
    ) {
        val topPad = if (tooltipAbove) {
            // Tooltip bên trên vùng sáng → đặt sát đỉnh màn hình
            24.dp
        } else {
            // Tooltip bên dưới vùng sáng
            (spotlightBottomDp + 16.dp).coerceAtLeast(100.dp)
        }

        Surface(
            shape          = RoundedCornerShape(24.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier       = Modifier.fillMaxWidth().padding(top = topPad)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter            = painterResource(id = meta.iconRes),
                    contentDescription = null,
                    modifier           = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text       = meta.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign  = TextAlign.Center,
                    color      = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text      = meta.description,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dots indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier              = Modifier.fillMaxWidth()
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
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(
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
