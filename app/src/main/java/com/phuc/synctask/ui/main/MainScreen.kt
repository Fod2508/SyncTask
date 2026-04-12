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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.phuc.synctask.R
import com.phuc.synctask.model.Quadrant
import com.phuc.synctask.ui.achievement.AchievementScreen
import com.phuc.synctask.ui.dashboard.DashboardScreen
import com.phuc.synctask.ui.group.GroupListScreen
import com.phuc.synctask.ui.group.GroupTaskScreen
import com.phuc.synctask.ui.navigation.Screen
import com.phuc.synctask.ui.onboarding.SpotlightOverlay
import com.phuc.synctask.ui.onboarding.TUTORIAL_STEPS
import com.phuc.synctask.ui.personal.PersonalTaskScreen
import com.phuc.synctask.viewmodel.HomeViewModel
import com.phuc.synctask.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay

// Số bước tutorial = số phần tử trong TUTORIAL_STEPS (Single Source of Truth)
private val TOTAL_TUTORIAL_STEPS = TUTORIAL_STEPS.size   // = 4

// Map bước → route cần navigate tới
private fun tutorialRouteForStep(step: Int): String = when (step) {
    0 -> Screen.Personal.route
    1 -> Screen.Group.route
    2 -> Screen.Achievement.route
    3 -> Screen.Dashboard.route
    else -> Screen.Personal.route
}

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

    // ── Tutorial state ──────────────────────────────────────────
    // -1 = ẩn overlay; 0..3 = đang chạy bước tương ứng
    var tutorialStep by remember { mutableStateOf(if (showTutorial) 0 else -1) }
    val showSpotlight = tutorialStep in 0 until TOTAL_TUTORIAL_STEPS

    // Consume onTutorialFinished() đúng một lần (lần đầu chạy app).
    // Replay sau đó KHÔNG ghi DataStore.
    var pendingFirstRunFinish by remember { mutableStateOf(showTutorial) }

    // ── Bounds tuyệt đối (boundsInRoot) ─────────────────────────
    // Tab items trong BottomBar — luôn visible nên bounds ổn định
    var personalTabBounds  by remember { mutableStateOf<Rect?>(null) }
    var groupTabBounds     by remember { mutableStateOf<Rect?>(null) }
    var achieveTabBounds   by remember { mutableStateOf<Rect?>(null) }
    var dashboardTabBounds by remember { mutableStateOf<Rect?>(null) }

    // Ma trận Eisenhower — chỉ có khi PersonalTaskScreen đang compose
    var matrixBounds by remember { mutableStateOf<Rect?>(null) }

    // ── Auto-navigation khi bước thay đổi ───────────────────────
    // Mỗi khi tutorialStep thay đổi → navigate đến tab tương ứng,
    // delay ngắn để UI render xong và bounds được đo trước khi spotlight vẽ.
    LaunchedEffect(tutorialStep) {
        if (tutorialStep < 0) return@LaunchedEffect
        val targetRoute = tutorialRouteForStep(tutorialStep)
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
        // Chờ frame render xong để bounds được cập nhật
        delay(120)
    }

    // ── Chọn bounds cho bước hiện tại ───────────────────────────
    // Bước 0 (Personal): spotlight vào tab Personal trên BottomBar
    // Bước 1 (Group):    spotlight vào tab Group
    // Bước 2 (Achieve):  spotlight vào tab Achievement
    // Bước 3 (Dashboard):spotlight vào tab Dashboard
    val currentSpotlightBounds: Rect? = when (tutorialStep) {
        0 -> personalTabBounds
        1 -> groupTabBounds
        2 -> achieveTabBounds
        3 -> dashboardTabBounds
        else -> null
    }

    val isDetailRoute = currentRoute == "quadrant_detail/{quadrant}" ||
            currentRoute == "group_detail/{groupId}"

    var showAddSheet by remember { mutableStateOf(false) }

    // ── Kích thước màn hình ──────────────────────────────────────
    val density = LocalDensity.current
    val config  = LocalConfiguration.current
    val screenSize = with(density) {
        Size(config.screenWidthDp.dp.toPx(), config.screenHeightDp.dp.toPx())
    }

    Scaffold(
        topBar = {
            if (!isDetailRoute) {
                TopAppBar(
                    title = {
                        val user = FirebaseAuth.getInstance().currentUser
                        val name = user?.displayName?.takeIf { it.isNotBlank() }
                            ?: user?.email?.substringBefore("@")
                            ?: "Bạn"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = getInitials(name),
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text  = "Chào, $name",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Nút replay tutorial — chỉ reset state local, KHÔNG đụng DataStore
                        IconButton(onClick = { tutorialStep = 0 }) {
                            Icon(
                                imageVector        = Icons.Outlined.MenuBook,
                                contentDescription = "Xem lại hướng dẫn"
                            )
                        }
                        IconButton(onClick = { themeViewModel.toggleTheme() }) {
                            Icon(
                                imageVector        = if (isDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
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
            if (currentRoute == Screen.Personal.route && !showSpotlight) {
                FloatingActionButton(
                    onClick   = { showAddSheet = true },
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
                BottomAppBar(containerColor = MaterialTheme.colorScheme.surface) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon     = { Icon(screen.icon, contentDescription = screen.title) },
                            label    = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick  = {
                                if (!showSpotlight) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    // boundsInRoot() — tọa độ pixel tuyệt đối so với root
                                    val b = coords.boundsInRoot()
                                    when (screen) {
                                        Screen.Personal    -> personalTabBounds  = b
                                        Screen.Group       -> groupTabBounds     = b
                                        Screen.Achievement -> achieveTabBounds   = b
                                        Screen.Dashboard   -> dashboardTabBounds = b
                                    }
                                }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Personal.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Personal.route) {
                PersonalTaskScreen(
                    viewModel            = homeViewModel,
                    onNavigateToQuadrant = { q -> navController.navigate("quadrant_detail/${q.name}") },
                    onMatrixPositioned   = { bounds -> matrixBounds = bounds }
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
                val quadrant     = Quadrant.valueOf(quadrantName ?: Quadrant.DO_NOW.name)
                com.phuc.synctask.ui.personal.QuadrantDetailScreen(
                    quadrant  = quadrant,
                    viewModel = homeViewModel,
                    onBack    = { navController.popBackStack() }
                )
            }
        }
    }

    // ── AddTaskBottomSheet ───────────────────────────────────────
    if (showAddSheet) {
        AddTaskBottomSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { task ->
                homeViewModel.addTask(
                    title       = task.title,
                    description = task.description,
                    isUrgent    = task.isUrgent,
                    isImportant = task.isImportant,
                    dueDate     = task.dueDate
                )
                showAddSheet = false
            }
        )
    }

    // ── Spotlight Tutorial Overlay ───────────────────────────────
    if (showSpotlight) {
        SpotlightOverlay(
            currentTargetBounds = currentSpotlightBounds,
            currentStep         = tutorialStep,
            totalSteps          = TOTAL_TUTORIAL_STEPS,
            screenSize          = screenSize,
            spotlightPadding    = 18f,
            onNext = {
                if (tutorialStep < TOTAL_TUTORIAL_STEPS - 1) {
                    tutorialStep++
                } else {
                    // Bước cuối: consume first-run flag nếu còn
                    if (pendingFirstRunFinish) {
                        onTutorialFinished()
                        pendingFirstRunFinish = false
                    }
                    tutorialStep = -1
                }
            },
            onSkip = {
                if (pendingFirstRunFinish) {
                    onTutorialFinished()
                    pendingFirstRunFinish = false
                }
                tutorialStep = -1
            }
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────

fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].first().uppercase()
        else            -> "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
    }
}

private val avatarColors = listOf(
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFEF5350),
    Color(0xFFAB47BC),
    Color(0xFFFFA726),
    Color(0xFF26A69A),
    Color(0xFFEC407A),
    Color(0xFF5C6BC0)
)

fun getAvatarColor(name: String): Color {
    val hash = kotlin.math.abs(name.hashCode())
    return avatarColors[hash % avatarColors.size]
}
