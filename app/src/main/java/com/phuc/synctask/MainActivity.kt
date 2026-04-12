package com.phuc.synctask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.ui.auth.LoginScreen
import com.phuc.synctask.ui.auth.RegisterScreen
import com.phuc.synctask.ui.main.MainScreen
import com.phuc.synctask.ui.onboarding.WelcomeScreen
import com.phuc.synctask.ui.theme.SyncTaskTheme
import com.phuc.synctask.util.LocalSoundManager
import com.phuc.synctask.util.SoundManager
import com.phuc.synctask.viewmodel.AuthViewModel
import com.phuc.synctask.viewmodel.OnboardingViewModel
import com.phuc.synctask.viewmodel.ThemeViewModel

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)
        setContent {
            val isDark by themeViewModel.isDarkTheme.collectAsState()
            val isFirstTime by onboardingViewModel.isFirstTime.collectAsState()

            SyncTaskTheme(useDarkTheme = isDark) {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
                    val rootNavController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()

                    var currentUser by remember {
                        mutableStateOf(FirebaseAuth.getInstance().currentUser)
                    }
                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            currentUser = auth.currentUser
                        }
                        FirebaseAuth.getInstance().addAuthStateListener(listener)
                        onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
                    }

                    if (isFirstTime == null) {
                        // Chờ DataStore load xong — hiển thị màn hình trống
                        Box(modifier = Modifier.fillMaxSize())
                    } else {
                        val startRoute = when {
                            currentUser == null -> "login"
                            isFirstTime == true -> "welcome"
                            else                -> "main/false"
                        }

                        NavHost(
                            navController = rootNavController,
                            startDestination = startRoute
                        ) {

                            // ── Welcome ──────────────────────────────────────────────
                            composable("welcome") {
                                WelcomeScreen(
                                    onGetStarted = {
                                        rootNavController.navigate("main/true") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // ── Login ─────────────────────────────────────────────────
                            composable("login") {
                                authViewModel.resetState()
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRegister = {
                                        authViewModel.resetState()
                                        rootNavController.navigate("register")
                                    },
                                    onLoginSuccess = {
                                        val dest = if (isFirstTime == true) "welcome" else "main/false"
                                        rootNavController.navigate(dest) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // ── Register ──────────────────────────────────────────────
                            composable("register") {
                                authViewModel.resetState()
                                RegisterScreen(
                                    viewModel = authViewModel,
                                    onNavigateToLogin = {
                                        authViewModel.resetState()
                                        rootNavController.popBackStack()
                                    },
                                    onRegisterSuccess = {
                                        rootNavController.navigate("welcome") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // ── Main (nhận showTutorial qua path param) ───────────────
                            composable(
                                "main/{showTutorial}",
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
                                    themeViewModel     = themeViewModel,
                                    showTutorial       = showTutorial,
                                    onTutorialFinished = { onboardingViewModel.markOnboardingDone() },
                                    onLogout           = {
                                        onboardingViewModel.resetOnboarding()
                                        FirebaseAuth.getInstance().signOut()
                                        rootNavController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                } // CompositionLocalProvider
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
