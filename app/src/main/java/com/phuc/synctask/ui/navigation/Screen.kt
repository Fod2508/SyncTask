package com.phuc.synctask.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Personal    : Screen("personal",     "Cá nhân",   Icons.Filled.Person)
    object Group       : Screen("group",         "Nhóm",      Icons.Filled.Group)
    object Achievement : Screen("achievement",   "Thành tựu", Icons.Filled.EmojiEvents)
    object Dashboard   : Screen("dashboard",     "Thống kê",  Icons.Filled.Analytics)
}
