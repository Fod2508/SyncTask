package com.phuc.synctask.viewmodel


import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
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
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun loginWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email và mật khẩu không được để trống!")
            return
        }
        
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Đăng nhập thất bại!")
                }
            }
    }

    fun registerWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email và mật khẩu không được để trống!")
            return
        }
        
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Đăng ký thất bại!")
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
}
