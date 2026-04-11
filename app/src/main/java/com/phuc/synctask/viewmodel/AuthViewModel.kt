package com.phuc.synctask.viewmodel


import androidx.lifecycle.ViewModel
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
        
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let { uid ->
                        saveUserToDatabase(uid, trimmedEmail)
                    }
                    _authState.value = AuthState.Success
                } else {
                    val errorMsg = mapAuthError(task.exception)
                    _authState.value = AuthState.Error(errorMsg)
                }
            }
    }

    fun registerWithEmail(name: String, email: String, pass: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        
        if (trimmedName.isBlank()) {
            _authState.value = AuthState.Error("Vui lòng nhập Họ và Tên!")
            return
        }
        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _authState.value = AuthState.Error("Email và mật khẩu không được để trống!")
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
                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            // Lưu profile xuống Realtime Database
                            saveUserToDatabase(user.uid, trimmedEmail, trimmedName)
                            _authState.value = AuthState.Success
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

    // Google Sign-In framework (Placeholder for Web Client ID injection later)
    // In Android Compose, usually we launch an ActivityResultContract from UI
    // and pass the obtained GoogleSignInAccount ID Token to Firebase here.
    fun signInWithGoogle(idToken: String) {
        // ... (This function will be completed when you have the Web Client ID and Token)
        // Usually: auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
    }

    private fun saveUserToDatabase(uid: String, email: String, displayName: String? = null) {
        val name = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        val userMap = mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to name
        )
        database.reference.child("users").child(uid).setValue(userMap)
    }
}
