package com.phuc.synctask.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*

import com.phuc.synctask.R
import com.phuc.synctask.utils.AppSoundEffect
import com.phuc.synctask.utils.AppSoundPlayer
import com.phuc.synctask.viewmodel.AuthState
import com.phuc.synctask.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var isSendingReset by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()



    // Xử lý State của ViewModel
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                AppSoundPlayer.play(AppSoundEffect.AUTH_SUCCESS)
                onLoginSuccess()
            }
            is AuthState.Error -> {
                AppSoundPlayer.play(AppSoundEffect.ERROR)
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                if (isSendingReset) {
                    isSendingReset = false
                }
                viewModel.resetState()
            }
            is AuthState.PasswordResetSent -> {
                AppSoundPlayer.play(AppSoundEffect.AUTH_SUCCESS)
                snackbarHostState.showSnackbar((authState as AuthState.PasswordResetSent).message)
                if (isSendingReset) {
                    isSendingReset = false
                    showForgotPasswordDialog = false
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Lottie Animation
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets3.lottiefiles.com/packages/lf20_jcikwtux.json")
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Lottie Animation
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Đăng Nhập",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    if (authState is AuthState.Error) {
                        viewModel.resetState()
                    }
                },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (authState is AuthState.Error) {
                        viewModel.resetState()
                    }
                },
                label = { Text("Mật khẩu") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val desc = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = desc)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        resetEmail = email
                        showForgotPasswordDialog = true
                    }
                ) {
                    Text("Quên mật khẩu?")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.loginWithEmail(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = authState != AuthState.Loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng nhập bằng Email", fontSize = 16.sp)
                }
            }



            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chưa có tài khoản?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Đăng ký ngay", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingReset) {
                    showForgotPasswordDialog = false
                }
            },
            title = { Text("Khôi phục mật khẩu") },
            text = {
                Column {
                    Text("Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSendingReset,
                    onClick = {
                        isSendingReset = true
                        viewModel.sendPasswordResetEmail(resetEmail)
                    }
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Gửi")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSendingReset,
                    onClick = { showForgotPasswordDialog = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
