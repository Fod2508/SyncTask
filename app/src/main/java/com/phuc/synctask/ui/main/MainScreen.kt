package com.phuc.synctask.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.ui.achievement.AchievementScreen
import com.phuc.synctask.ui.dashboard.DashboardScreen
import com.phuc.synctask.ui.group.GroupListScreen
import com.phuc.synctask.ui.group.GroupTaskScreen
import com.phuc.synctask.ui.navigation.Screen
import com.phuc.synctask.ui.onboarding.SpotlightOverlay
import com.phuc.synctask.ui.onboarding.TUTORIAL_STEPS
import com.phuc.synctask.ui.personal.PersonalTaskScreen
import com.phuc.synctask.viewmodel.HomeViewModel
import com.phuc.synctask.viewmodel.SoundSettingsViewModel
import com.phuc.synctask.viewmodel.ThemeViewModel
import com.phuc.synctask.model.Quadrant
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeViewModel: ThemeViewModel,
    showTutorial: Boolean = false,
    onTutorialFinished: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val isDark by themeViewModel.isDarkTheme.collectAsState()
    val notificationViewModel: com.phuc.synctask.viewmodel.NotificationViewModel = viewModel()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val soundSettingsViewModel: SoundSettingsViewModel = viewModel()
    val soundSettings by soundSettingsViewModel.state.collectAsState()
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val screens = listOf(
        Screen.Personal,
        Screen.Group,
        Screen.Achievement,
        Screen.Dashboard
    )

    LaunchedEffect(Unit) {
        homeViewModel.soundEvent.collectLatest { effect ->
            com.phuc.synctask.utils.AppSoundPlayer.play(effect)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Tutorial state — khởi tạo từ tham số navigation
    var tutorialStep by remember { mutableStateOf(if (showTutorial) 0 else -1) }
    val totalTutorialSteps = TUTORIAL_STEPS.size
    val showSpotlight = tutorialStep in 0 until totalTutorialSteps

    // Tọa độ thực từ onGloballyPositioned (dùng Rect boundsInRoot)
    var matrixBoundsRect     by remember { mutableStateOf<Rect?>(null) }
    var personalTabBoundsRect by remember { mutableStateOf<Rect?>(null) }
    var groupTabBoundsRect   by remember { mutableStateOf<Rect?>(null) }
    var achieveTabBoundsRect by remember { mutableStateOf<Rect?>(null) }
    var dashTabBoundsRect    by remember { mutableStateOf<Rect?>(null) }

    val isDetailRoute = currentRoute == "quadrant_detail/{quadrant}" ||
            currentRoute == "group_detail/{groupId}"

    var showAddSheet by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!isDetailRoute) {
                TopAppBar(
                    title = {
                        val user = FirebaseAuth.getInstance().currentUser
                        val name = user?.displayName?.takeIf { it.isNotBlank() }
                            ?: user?.email?.substringBefore("@")
                            ?: "Bạn"
                        val initials = getInitials(name)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials, 
                                    color = MaterialTheme.colorScheme.onPrimary, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Chào, $name", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.NOTIFICATION)
                            tutorialStep = 0 
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = "Xem lại hướng dẫn"
                            )
                        }
                        IconButton(onClick = { 
                            com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.TASK_CREATED)
                            showNotificationSheet = true 
                        }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Thông báo")
                            }
                        }
                        IconButton(onClick = { 
                            com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.TASK_CREATED)
                            showSoundDialog = true 
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Âm lượng"
                            )
                        }
                        IconButton(onClick = {
                            com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.TASK_CREATED)
                            themeViewModel.toggleTheme()
                        }) {
                            Icon(    imageVector = if (isDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = if (isDark) "Chuyển sang sáng" else "Chuyển sang tối"
                            )
                        }
                        IconButton(onClick = {
                            com.phuc.synctask.utils.AppSoundPlayer.play(com.phuc.synctask.utils.AppSoundEffect.TASK_CREATED)
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Đăng xuất")
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (currentRoute == Screen.Personal.route) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Thêm công việc")
                }
            }
        },
        bottomBar = {
            if (!isDetailRoute) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    val rect = coords.boundsInRoot()
                                    when (screen) {
                                        Screen.Personal    -> personalTabBoundsRect = rect
                                        Screen.Group       -> groupTabBoundsRect   = rect
                                        Screen.Achievement -> achieveTabBoundsRect = rect
                                        Screen.Dashboard   -> dashTabBoundsRect    = rect
                                        else               -> Unit
                                    }
                                }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Personal.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Personal.route) {
                PersonalTaskScreen(
                    viewModel = homeViewModel,
                    onNavigateToQuadrant = { q -> navController.navigate("quadrant_detail/${q.name}") },
                    onMatrixPositioned = { offset, size ->
                        matrixBoundsRect = Rect(
                            left   = offset.x,
                            top    = offset.y,
                            right  = offset.x + size.width,
                            bottom = offset.y + size.height
                        )
                    }
                )
            }
            composable(Screen.Group.route) {
                GroupListScreen(
                    onNavigateToGroup = { groupId -> navController.navigate("group_detail/$groupId") }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Achievement.route) {
                AchievementScreen()
            }
            composable("group_detail/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupTaskScreen(groupId = groupId, onBack = { navController.popBackStack() })
            }
            composable("quadrant_detail/{quadrant}") { backStackEntry ->
                val quadrantName = backStackEntry.arguments?.getString("quadrant")
                val quadrant = Quadrant.valueOf(quadrantName ?: Quadrant.DO_NOW.name)
                com.phuc.synctask.ui.personal.QuadrantDetailScreen(
                    quadrant = quadrant,
                    viewModel = homeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    // AddTaskBottomSheet
    if (showAddSheet) {
        AddTaskBottomSheet(
            onDismiss = { showAddSheet = false },
            onSave = { task ->
                homeViewModel.addTask(
                    title = task.title,
                    description = task.description,
                    isUrgent = task.isUrgent,
                    isImportant = task.isImportant,
                    dueDate = task.dueDate
                )
                showAddSheet = false
            }
        )
    }

    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text("Âm thanh hiệu ứng") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bật hiệu ứng âm thanh")
                        Switch(
                            checked = soundSettings.isEnabled,
                            onCheckedChange = { soundSettingsViewModel.setEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Âm lượng: ${soundSettings.volumePercent}%")
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = soundSettings.volumePercent.toFloat(),
                        onValueChange = { soundSettingsViewModel.setVolumePercent(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = soundSettings.isEnabled
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showNotificationSheet) {
        NotificationBottomSheet(
            viewModel = notificationViewModel,
            onDismiss = { showNotificationSheet = false }
        )
    }

    // ── Spotlight Tutorial Overlay ──
    if (showSpotlight) {
        val density = LocalDensity.current
        val config  = LocalConfiguration.current
        val screenW = with(density) { config.screenWidthDp.dp.toPx() }
        val screenH = with(density) { config.screenHeightDp.dp.toPx() }
        val screenSize = Size(screenW, screenH)

        // Chọn Rect mục tiêu theo bước tutorial:
        // Bước 0 → cá nhân, 1 → nhóm, 2 → thành tựu, 3 → dashboard
        val currentTargetBounds: Rect? = when (tutorialStep) {
            0 -> personalTabBoundsRect
            1 -> groupTabBoundsRect
            2 -> achieveTabBoundsRect
            3 -> dashTabBoundsRect
            else -> null
        }

        SpotlightOverlay(
            currentTargetBounds = currentTargetBounds,
            currentStep         = tutorialStep,
            totalSteps          = totalTutorialSteps,
            screenSize          = screenSize,
            onNext = {
                if (tutorialStep < totalTutorialSteps - 1) {
                    tutorialStep++
                } else {
                    onTutorialFinished()
                    tutorialStep = -1
                }
            },
            onSkip = {
                onTutorialFinished()
                tutorialStep = -1
            }
        )
    }
}

/**
 * Trích xuất chữ cái đầu từ tên.
 * "Nguyễn Phúc" -> "NP", "Huy" -> "H", "" -> "?"
 */
fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].first().uppercase()
        else -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
    }
}

/**
 * Danh sách màu pastel đẹp mắt cho Avatar.
 */
private val avatarColors = listOf(
    Color(0xFF42A5F5), // Blue
    Color(0xFF66BB6A), // Green
    Color(0xFFEF5350), // Red
    Color(0xFFAB47BC), // Purple
    Color(0xFFFFA726), // Orange
    Color(0xFF26A69A), // Teal
    Color(0xFFEC407A), // Pink
    Color(0xFF5C6BC0)  // Indigo
)

/**
 * Lấy màu avatar cố định cho một tên dựa trên HashCode.
 */
fun getAvatarColor(name: String): Color {
    val hash = kotlin.math.abs(name.hashCode())
    return avatarColors[hash % avatarColors.size]
}
