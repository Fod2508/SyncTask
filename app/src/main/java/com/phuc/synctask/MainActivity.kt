package com.phuc.synctask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.ui.auth.LoginScreen
import com.phuc.synctask.ui.auth.RegisterScreen
import com.phuc.synctask.ui.common.AnimatedLoadingScreen
import com.phuc.synctask.ui.main.MainScreen
import com.phuc.synctask.ui.onboarding.WelcomeScreen
import com.phuc.synctask.ui.theme.SyncTaskTheme
import com.phuc.synctask.utils.AppSoundPlayer
import com.phuc.synctask.viewmodel.AuthViewModel
import com.phuc.synctask.viewmodel.OnboardingViewModel
import com.phuc.synctask.viewmodel.SoundSettingsViewModel
import com.phuc.synctask.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val soundSettingsViewModel: SoundSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSoundPlayer.init(applicationContext)
        setContent {
            val isDark by themeViewModel.isDarkTheme.collectAsState()
            val soundSettings by soundSettingsViewModel.state.collectAsState()
            // null = DataStore chưa load xong
            val isFirstTime by onboardingViewModel.isFirstTime.collectAsState()

            LaunchedEffect(soundSettings.isEnabled, soundSettings.volumePercent) {
                AppSoundPlayer.setEnabled(soundSettings.isEnabled)
                AppSoundPlayer.setVolumePercent(soundSettings.volumePercent)
            }

            SyncTaskTheme(useDarkTheme = isDark) {
                val rootNavController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                // Thay vì dùng AuthStateListener lắng nghe liên tục có thể gây lỗi reset NavHost (do Firebase tự login rất nhanh trước khi check email verify)
                // Ta chỉ cần tính toán route khởi đầu MỘT LẦN khi App khởi động xong DataStore.
                var computedStartRoute by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(isFirstTime) {
                    if (isFirstTime != null && computedStartRoute == null) {
                        val auth = FirebaseAuth.getInstance()
                        val user = auth.currentUser
                        val isPasswordLogin = user?.providerData?.any { it.providerId == "password" } == true
                        val isValidUser = user != null && (!isPasswordLogin || user.isEmailVerified)

                        computedStartRoute = when {
                            !isValidUser -> {
                                if (user != null) auth.signOut() // Đăng xuất phòng hờ user chưa xác thực email (do session cũ)
                                "login"
                            }
                            isFirstTime == true -> "welcome"
                            else -> "main/false"
                        }
                    }
                }

                if (computedStartRoute == null) {
                    AnimatedLoadingScreen(message = "Đang khởi tạo dữ liệu...")
                    return@SyncTaskTheme
                }

                NavHost(
                    navController = rootNavController,
                    startDestination = computedStartRoute!!
                ) {

                    // ── Welcome ──────────────────────────────────────────────
                    composable("welcome") {
                        WelcomeScreen(
                            onGetStarted = {
                                // Chưa lưu DataStore — tutorial chạy xong mới lưu
                                rootNavController.navigate("main/true") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Login ─────────────────────────────────────────────────
                    composable("login") {
                        LaunchedEffect(Unit) {
                            authViewModel.resetState()
                        }
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToRegister = {
                                rootNavController.navigate("register")
                            },
                            onLoginSuccess = {
                                // Người dùng cũ xóa app / lỗi cache → vẫn show welcome
                                val dest = if (isFirstTime == true) "welcome" else "main/false"
                                rootNavController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Register ──────────────────────────────────────────────
                    composable("register") {
                        LaunchedEffect(Unit) {
                            authViewModel.resetState()
                        }
                        RegisterScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                rootNavController.popBackStack()
                            },
                            onRegisterSuccess = {
                                // Đăng ký xong → thẳng welcome, xóa toàn bộ backstack
                                rootNavController.navigate("welcome") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── Main (nhận showTutorial qua path param) ───────────────
                    composable("main/{showTutorial}",
                        arguments = listOf(
                            navArgument("showTutorial") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val showTutorial = backStackEntry.arguments
                            ?.getBoolean("showTutorial") ?: false
                        MainScreen(
                            themeViewModel      = themeViewModel,
                            showTutorial        = showTutorial,
                            onTutorialFinished  = { onboardingViewModel.markOnboardingDone() },
                            onLogout            = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppSoundPlayer.resumeBgm()
    }

    override fun onPause() {
        super.onPause()
        AppSoundPlayer.pauseBgm()
    }
}
