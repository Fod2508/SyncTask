package com.phuc.synctask.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.phuc.synctask.ui.onboarding.TutorialStep
import com.phuc.synctask.R
import com.phuc.synctask.ui.personal.PersonalTaskScreen
import com.phuc.synctask.viewmodel.HomeViewModel
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
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val screens = listOf(
        Screen.Personal,
        Screen.Group,
        Screen.Achievement,
        Screen.Dashboard
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Tutorial state — khởi tạo từ tham số navigation
    var tutorialStep by remember { mutableStateOf(if (showTutorial) 0 else -1) }
    val showSpotlight = tutorialStep in 0..3

    // Tọa độ tuyệt đối từ boundsInRoot() — Rect pixel
    var matrixBounds    by remember { mutableStateOf<Rect?>(null) }
    var groupTabBounds  by remember { mutableStateOf<Rect?>(null) }
    var achieveTabBounds by remember { mutableStateOf<Rect?>(null) }
    var dashboardTabBounds by remember { mutableStateOf<Rect?>(null) }

    val isDetailRoute = currentRoute == "quadrant_detail/{quadrant}" ||
            currentRoute == "group_detail/{groupId}"

    var showAddSheet by remember { mutableStateOf(false) }

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
                        // Nút xem lại hướng dẫn — chỉ reset state local, không đụng DataStore
                        IconButton(onClick = { tutorialStep = 0 }) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = "Xem lại hướng dẫn"
                            )
                        }
                        IconButton(onClick = { themeViewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = if (isDark) "Chuyển sang sáng" else "Chuyển sang tối"
                            )
                        }
                        IconButton(onClick = onLogout) {
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
                                    val bounds = coords.boundsInRoot()
                                    when (screen) {
                                        Screen.Group       -> groupTabBounds    = bounds
                                        Screen.Achievement -> achieveTabBounds  = bounds
                                        Screen.Dashboard   -> dashboardTabBounds = bounds
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
                    onMatrixPositioned = { bounds -> matrixBounds = bounds }
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

    // ── Spotlight Tutorial Overlay ──
    if (showSpotlight) {
        val density = LocalDensity.current
        val config  = LocalConfiguration.current
        val screenW = with(density) { config.screenWidthDp.dp.toPx() }
        val screenH = with(density) { config.screenHeightDp.dp.toPx() }
        val screenSize = Size(screenW, screenH)

        val tutorialSteps = listOf(
            // Bước 0: Ma trận Eisenhower
            TutorialStep(
                targetBounds     = matrixBounds,
                padding          = 24f,
                title            = "Ma trận Eisenhower là gì?",
                iconRes          = R.drawable.ic_tutorial_read,
                isEisenhowerStep = true,
                tooltipBelow     = false
            ),
            // Bước 1: Tab Nhóm
            TutorialStep(
                targetBounds = groupTabBounds,
                padding      = 16f,
                title        = "Tăng tốc cùng đồng đội",
                iconRes      = R.drawable.ic_tutorial_rocket,
                tooltipBelow = false
            ),
            // Bước 2: Tab Thành tựu
            TutorialStep(
                targetBounds = achieveTabBounds,
                padding      = 16f,
                title        = "Phóng tới các cột mốc",
                iconRes      = R.drawable.ic_tutorial_rocket,
                tooltipBelow = false
            ),
            // Bước 3: Tab Dashboard
            TutorialStep(
                targetBounds = dashboardTabBounds,
                padding      = 16f,
                title        = "Tổng quan Dashboard",
                iconRes      = R.drawable.ic_welcome_rocket,
                tooltipBelow = false   // tab ở bottom → spotlight thấp → tooltip tự động lên trên
            )
        )

        SpotlightOverlay(
            steps       = tutorialSteps,
            currentStep = tutorialStep,
            screenSize  = screenSize,
            onNext      = {
                if (tutorialStep < tutorialSteps.lastIndex) {
                    tutorialStep++
                } else {
                    // Bước cuối: nếu là lần đầu thì lưu DataStore, replay thì chỉ đóng overlay
                    if (showTutorial) onTutorialFinished()
                    tutorialStep = -1
                }
            },
            onSkip      = {
                if (showTutorial) onTutorialFinished()
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
