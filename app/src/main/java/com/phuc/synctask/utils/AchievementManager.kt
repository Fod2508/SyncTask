package com.phuc.synctask.utils

import com.phuc.synctask.model.UserProfile
import java.util.Calendar

/**
 * Quản lý logic kiểm tra và mở khóa thành tựu.
 * Hoàn toàn pure — không phụ thuộc Android/Firebase, dễ test.
 */
object AchievementManager {

    // ─── ID huy hiệu cá nhân ─────────────────────────────────────
    const val ROOKIE_BADGE     = "rookie_badge"       // Hoàn thành task đầu tiên
    const val NIGHT_OWL_BADGE  = "night_owl_badge"    // Hoàn thành task lúc 1–5 giờ sáng
    const val DILIGENT_BADGE   = "diligent_badge"     // Hoàn thành 10 task
    const val WARRIOR_BADGE    = "warrior_badge"      // Hoàn thành 50 task
    const val LEGEND_BADGE     = "legend_badge"       // Hoàn thành 200 task
    const val ON_TIME_BADGE    = "on_time_badge"      // Hoàn thành task trước hạn

    // ─── ID huy hiệu nhóm ────────────────────────────────────────
    const val TEAM_PLAYER_BADGE = "team_player_badge" // Hoàn thành 5 task nhóm
    const val CAPTAIN_BADGE     = "captain_badge"     // Trưởng nhóm hoàn thành task nhóm

    /**
     * Kiểm tra tất cả điều kiện thành tựu sau khi một task được đánh dấu hoàn thành.
     *
     * @param completedCount   Tổng số task cá nhân đã hoàn thành
     * @param dueDateMillis    Deadline của task (null nếu không có)
     * @param profile          Profile hiện tại của user
     * @param isGroupTask      Task này thuộc nhóm hay không
     * @param isOwner          User có phải trưởng nhóm không (chỉ có nghĩa khi isGroupTask=true)
     * @param onUnlocked       Callback trả về achievementId khi đủ điều kiện
     */
    fun checkAndUnlock(
        completedCount: Int,
        dueDateMillis: Long?,
        profile: UserProfile,
        isGroupTask: Boolean = false,
        isOwner: Boolean = false,
        onUnlocked: (achievementId: String) -> Unit
    ) {
        val unlocked = profile.unlockedAchievements

        // ── Huy hiệu cá nhân ──────────────────────────────────────

        // 1. Khởi đầu tốt — hoàn thành task đầu tiên
        if (completedCount >= 1 && ROOKIE_BADGE !in unlocked) {
            onUnlocked(ROOKIE_BADGE)
        }

        // 2. Siêng năng — hoàn thành 10 task
        if (completedCount >= 10 && DILIGENT_BADGE !in unlocked) {
            onUnlocked(DILIGENT_BADGE)
        }

        // 3. Chiến binh — hoàn thành 50 task
        if (completedCount >= 50 && WARRIOR_BADGE !in unlocked) {
            onUnlocked(WARRIOR_BADGE)
        }

        // 4. Huyền thoại — hoàn thành 200 task
        if (completedCount >= 200 && LEGEND_BADGE !in unlocked) {
            onUnlocked(LEGEND_BADGE)
        }

        // 5. Cú đêm — hoàn thành task lúc 1:00–5:00 sáng
        if (NIGHT_OWL_BADGE !in unlocked) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour in 1..4) onUnlocked(NIGHT_OWL_BADGE)
        }

        // 6. Đúng hạn — hoàn thành trước deadline
        if (ON_TIME_BADGE !in unlocked && dueDateMillis != null) {
            if (System.currentTimeMillis() < dueDateMillis) {
                onUnlocked(ON_TIME_BADGE)
            }
        }

        // ── Huy hiệu nhóm ─────────────────────────────────────────

        if (isGroupTask) {
            // groupTaskCount + 1 vì Firebase chưa cập nhật tại thời điểm check
            val newGroupCount = profile.groupTaskCount + 1

            // 7. Team Player — hoàn thành 5 task nhóm
            if (newGroupCount >= 5 && TEAM_PLAYER_BADGE !in unlocked) {
                onUnlocked(TEAM_PLAYER_BADGE)
            }

            // 8. Captain — trưởng nhóm hoàn thành task nhóm
            if (isOwner && CAPTAIN_BADGE !in unlocked) {
                onUnlocked(CAPTAIN_BADGE)
            }
        }
    }
}
