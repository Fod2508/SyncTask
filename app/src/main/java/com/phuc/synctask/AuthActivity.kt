package com.phuc.synctask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.phuc.synctask.ui.auth.LoginScreen
import com.phuc.synctask.ui.auth.RegisterScreen
import com.phuc.synctask.ui.theme.SyncTaskTheme
import com.phuc.synctask.viewmodel.AuthViewModel

/**
 * Activity đăng nhập / đăng ký.
 * Đặt làm LAUNCHER trong AndroidManifest.
 * Nếu user đã đăng nhập → chuyển thẳng sang MainActivity (Dashboard).
 */
class AuthActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra nếu đã đăng nhập → chuyển thẳng sang Dashboard
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navigateToDashboard()
            return
        }

        setContent {
            SyncTaskTheme(useDarkTheme = false) {
                var showLogin by remember { mutableStateOf(true) }

                if (showLogin) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onNavigateToRegister = { showLogin = false }
                    )
                } else {
                    RegisterScreen(
                        viewModel = authViewModel,
                        onNavigateToLogin = { showLogin = true }
                    )
                }
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
