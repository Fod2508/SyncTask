package com.phuc.synctask.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.phuc.synctask.utils.AchievementManager

// ─── Model ───────────────────────────────────────────────────────
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: androidx.compose.ui.graphics.Color,
    val isUnlocked: Boolean
)

// ─── Dummy Data ───────────────────────────────────────────────────
private val personalAchievements = listOf(
    Achievement(AchievementManager.ROOKIE_BADGE,    "Khởi đầu tốt",    "Hoàn thành task đầu tiên",           Icons.Filled.Star,            androidx.compose.ui.graphics.Color(0xFFFFC107), false),
    Achievement(AchievementManager.DILIGENT_BADGE,  "Siêng năng",       "Hoàn thành 10 task trong 1 tuần",    Icons.Filled.Bolt,            androidx.compose.ui.graphics.Color(0xFF3B82F6), false),
    Achievement(AchievementManager.ON_TIME_BADGE,   "Đúng hạn",         "Hoàn thành task trước deadline",     Icons.Filled.Timer,           androidx.compose.ui.graphics.Color(0xFF22C55E), false),
    Achievement(AchievementManager.WARRIOR_BADGE,   "Chiến binh",       "Hoàn thành 50 task",                 Icons.Filled.MilitaryTech,    androidx.compose.ui.graphics.Color(0xFFEF4444), false),
    Achievement(AchievementManager.LEGEND_BADGE,    "Huyền thoại",      "Hoàn thành 200 task",                Icons.Filled.EmojiEvents,     androidx.compose.ui.graphics.Color(0xFF9333EA), false),
    Achievement(AchievementManager.NIGHT_OWL_BADGE, "Cú đêm",           "Hoàn thành task lúc 1–5 giờ sáng",  Icons.Filled.AutoAwesome,     androidx.compose.ui.graphics.Color(0xFF6366F1), false),
)

private val groupAchievements = listOf(
    Achievement("group_join",                    "Đồng đội",       "Tham gia nhóm đầu tiên",                    Icons.Filled.Group,           androidx.compose.ui.graphics.Color(0xFF3B82F6), false),
    Achievement(AchievementManager.TEAM_PLAYER_BADGE, "Team Player", "Hoàn thành 5 task nhóm",                 Icons.Filled.Verified,        androidx.compose.ui.graphics.Color(0xFF22C55E), false),
    Achievement(AchievementManager.CAPTAIN_BADGE,     "Captain",    "Trưởng nhóm hoàn thành task nhóm",        Icons.Filled.WorkspacePremium,androidx.compose.ui.graphics.Color(0xFFF97316), false),
    Achievement("group_star",                    "Siêu sao nhóm",  "Được giao nhiều task nhất trong nhóm",      Icons.Filled.Rocket,          androidx.compose.ui.graphics.Color(0xFF9333EA), false),
    Achievement("group_30days",                  "Gắn kết",        "Hoạt động trong nhóm 30 ngày liên tiếp",    Icons.Filled.Favorite,        androidx.compose.ui.graphics.Color(0xFFEC4899), false),
    Achievement("group_perfect",                 "Nhà vô địch",    "Hoàn thành 100% task nhóm trong 1 tuần",    Icons.Filled.EmojiEvents,     androidx.compose.ui.graphics.Color(0xFFFFC107), false),
)

private val specialAchievements = listOf(
    Achievement("pioneer",       "Người tiên phong","Đăng ký tài khoản sớm nhất",               Icons.Filled.AutoAwesome,     androidx.compose.ui.graphics.Color(0xFFFFC107), false),
    Achievement("versatile",     "Đa năng",         "Dùng cả 4 ô Eisenhower trong 1 tuần",       Icons.Filled.Star,            androidx.compose.ui.graphics.Color(0xFF3B82F6), false),
    Achievement("perfectmonth",  "Hoàn hảo",        "Đạt 100% hoàn thành trong 1 tháng",         Icons.Filled.MilitaryTech,    androidx.compose.ui.graphics.Color(0xFF22C55E), false),
    Achievement("earlybird",     "Chim sớm",        "Hoàn thành task trước 7 giờ sáng",          Icons.Filled.Bolt,            androidx.compose.ui.graphics.Color(0xFFF97316), false),
)

// ─── Screen ───────────────────────────────────────────────────────
@Composable
fun AchievementScreen() {
    val tabs = listOf("Cá nhân", "Nhóm", "Đặc biệt")
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tải danh sách thành tựu đã mở từ Firebase
    val unlockedIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .reference.child("users").child(uid).child("unlockedAchievements")
            .get().addOnSuccessListener { snapshot ->
                @Suppress("UNCHECKED_CAST")
                val list = (snapshot.value as? List<String>) ?: emptyList()
                unlockedIds.value = list.toSet()
            }
    }

    fun List<Achievement>.withUnlocked() = map { it.copy(isUnlocked = it.id in unlockedIds.value) }

    val currentList = when (selectedTab) {
        0    -> personalAchievements.withUnlocked()
        1    -> groupAchievements.withUnlocked()
        else -> specialAchievements.withUnlocked()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ─── Tab Row ───
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ─── Grid ───
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentList) { achievement ->
                AchievementItem(achievement = achievement)
            }
        }
    }
}

// ─── Item Card ────────────────────────────────────────────────────
@Composable
private fun AchievementItem(achievement: Achievement) {
    // Grayscale ColorMatrix cho trạng thái khóa
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (achievement.isUnlocked) 4.dp else 1.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .alpha(if (achievement.isUnlocked) 1f else 0.5f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon huy hiệu
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (achievement.isUnlocked)
                                achievement.accentColor.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = achievement.icon,
                        contentDescription = achievement.title,
                        modifier = Modifier.size(40.dp),
                        tint = if (achievement.isUnlocked)
                            achievement.accentColor
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            // Icon ổ khóa góc trên phải khi chưa mở
            if (!achievement.isUnlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(22.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Chưa mở khóa",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
