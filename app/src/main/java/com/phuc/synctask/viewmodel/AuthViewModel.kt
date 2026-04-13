package com.phuc.synctask.viewmodel


import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class RegisterSuccess(val message: String) : AuthState()
    data class PasswordResetSent(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun loginWithEmail(email: String, pass: String) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        
        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _authState.value = AuthState.Error("Email và mật khẩu không được để trống!")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Email không đúng định dạng.")
            return
        }
        
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val isPasswordLogin = user?.providerData?.any { it.providerId == "password" } == true

                    if (user != null && isPasswordLogin && !user.isEmailVerified) {
                        user.sendEmailVerification()
                        auth.signOut()
                        _authState.value = AuthState.Error(
                            "Email chưa xác thực. Mình đã gửi lại email xác thực, vui lòng kiểm tra hộp thư."
                        )
                    } else {
                        _authState.value = AuthState.Success
                    }
                } else {
                    val errorMsg = mapAuthError(task.exception)
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
    }

    fun registerWithEmail(name: String, email: String, pass: String, confirmPass: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedConfirmPass = confirmPass.trim()
        
        if (trimmedName.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập Họ và Tên!")
            return
        }
        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _authState.value = AuthState.Error("Email và mật khẩu không được để trống!")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Email không đúng định dạng.")
            return
        }
        if (trimmedPass.length < 6) {
            _authState.value = AuthState.Error("Mật khẩu phải có ít nhất 6 ký tự.")
            return
        }
        if (trimmedPass != trimmedConfirmPass) {
            _authState.value = AuthState.Error("Mật khẩu xác nhận không khớp.")
            return
        }
        
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Cập nhật displayName trên Firebase Auth profile
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                        user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                            if (!profileTask.isSuccessful) {
                                _authState.value = AuthState.Error(
                                    "Tạo tài khoản thành công nhưng cập nhật hồ sơ thất bại. Vui lòng thử lại."
                                )
                                return@addOnCompleteListener
                            }

                            saveUserToDatabase(
                                uid = user.uid,
                                email = trimmedEmail,
                                displayName = trimmedName,
                                onSuccess = {
                                    user.sendEmailVerification()
                                    auth.signOut()
                                    _authState.value = AuthState.RegisterSuccess(
                                        "Đăng ký thành công. Vui lòng xác thực email trước khi đăng nhập."
                                    )
                                },
                                onError = { msg ->
                                    _authState.value = AuthState.Error(msg)
                                }
                            )
                        }
                    } else {
                        _authState.value = AuthState.Success
                    }
                } else {
                    val errorMsg = mapAuthError(task.exception)
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
    }
    
    private fun mapAuthError(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> "Lỗi: Email này đã được đăng ký!"
            is FirebaseAuthInvalidCredentialsException -> "Lỗi: Sai email hoặc mật khẩu!"
            is FirebaseAuthInvalidUserException -> "Lỗi: Tài khoản không tồn tại!"
            is FirebaseNetworkException -> "Lỗi kết nối, vui lòng kiểm tra lại mạng!"
            else -> {
                val message = exception?.message ?: ""
                when {
                    message.contains("badly formatted", ignoreCase = true) -> "Định dạng email sai."
                    message.contains("password should be at least", ignoreCase = true) -> "Mật khẩu phải có ít nhất 6 ký tự."
                    else -> "Lỗi: ${exception?.localizedMessage ?: "Đã có lỗi xảy ra"}"
                }
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }



    fun sendPasswordResetEmail(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập email để khôi phục mật khẩu.")
            return
        }
        if (!isValidEmail(trimmedEmail)) {
            _authState.value = AuthState.Error("Email không đúng định dạng.")
            return
        }

        _authState.value = AuthState.Loading
        auth.sendPasswordResetEmail(trimmedEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.PasswordResetSent(
                        "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra hộp thư."
                    )
                } else {
                    _authState.value = AuthState.Error(mapPasswordResetError(task.exception))
                }
            }
    }

    private fun mapPasswordResetError(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Email này chưa được đăng ký tài khoản."
            is FirebaseAuthInvalidCredentialsException -> "Email không hợp lệ."
            is FirebaseNetworkException -> "Lỗi kết nối, vui lòng kiểm tra lại mạng!"
            else -> {
                val message = exception?.message ?: ""
                when {
                    message.contains("no user record", ignoreCase = true) -> "Email này chưa được đăng ký tài khoản."
                    message.contains("badly formatted", ignoreCase = true) -> "Email không hợp lệ."
                    else -> "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại."
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveUserToDatabase(
        uid: String,
        email: String,
        displayName: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "email" to email
        )

        val name = displayName?.trim().orEmpty()
        if (name.isNotBlank()) {
            userMap["displayName"] = name
        }

        database.reference.child("users").child(uid)
            .updateChildren(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError("Không thể đồng bộ hồ sơ người dùng. Vui lòng thử lại.")
            }
    }
}
