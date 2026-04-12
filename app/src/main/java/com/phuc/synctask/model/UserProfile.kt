package com.phuc.synctask.model

/**
 * Thông tin profile người dùng lưu trên Realtime Database.
 * Đường dẫn: /users/{uid}/
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val unlockedAchievements: List<String> = emptyList(),
    val groupTaskCount: Int = 0          // Tổng số task nhóm đã hoàn thành
) {
    constructor() : this("", "", "", emptyList(), 0)
}
