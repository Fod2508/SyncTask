package com.phuc.synctask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.ui.auth.LoginScreen
import com.phuc.synctask.ui.auth.RegisterScreen
import com.phuc.synctask.ui.main.MainScreen
import com.phuc.synctask.ui.theme.SyncTaskTheme
import com.phuc.synctask.viewmodel.AuthViewModel

/**
 * Activity chính (Dashboard) của ứng dụng SyncTask.
 * Quản lý luồng Auth ↔ Main bằng NavController cấp cao nhất.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyncTaskTheme {
                val rootNavController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                // Lắng nghe Firebase Auth state thay đổi toàn cục
                var currentUser by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser)
                }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                    }
                    FirebaseAuth.getInstance().addAuthStateListener(listener)
                    onDispose {
                        FirebaseAuth.getInstance().removeAuthStateListener(listener)
                    }
                }

                val startRoute = if (currentUser != null) "main" else "login"

                NavHost(
                    navController = rootNavController,
                    startDestination = startRoute
                ) {
                    composable("login") {
                        authViewModel.resetState()
                        LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToRegister = {
                                authViewModel.resetState()
                                rootNavController.navigate("register")
                            },
                            onLoginSuccess = {
                                rootNavController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("register") {
                        authViewModel.resetState()
                        RegisterScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = {
                                authViewModel.resetState()
                                rootNavController.popBackStack()
                            },
                            onRegisterSuccess = {
                                rootNavController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            onLogout = {
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
}
