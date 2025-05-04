package com.example.todonotediary.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _googleSignInState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.Initial)
    val googleSignInState: StateFlow<GoogleSignInState> = _googleSignInState.asStateFlow()

    private val _emailSignInState = MutableStateFlow<EmailSignInState>(EmailSignInState.Initial)
    val emailSignInState: StateFlow<EmailSignInState> = _emailSignInState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signInWithGoogle(idToken: String) {
        _googleSignInState.value = GoogleSignInState.Loading
        viewModelScope.launch {
            try {
                val result = authUseCases.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { (user, userExists) ->
                        if (userExists) {
                            // Người dùng đã tồn tại - Đăng nhập thành công
                            _googleSignInState.value = GoogleSignInState.Success(user)
                            _authState.value = AuthState.Authenticated(user)
                        } else {
                            // Người dùng chưa tồn tại - chuyển đến màn hình đăng ký
                            _googleSignInState.value = GoogleSignInState.NeedRegistration(user.email ?: "")
                        }
                    },
                    onFailure = { exception ->
                        _googleSignInState.value = GoogleSignInState.Error(exception.message ?: "Đăng nhập thất bại")
                    }
                )
            } catch (e: Exception) {
                _googleSignInState.value = GoogleSignInState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _emailSignInState.value = EmailSignInState.Error("Email và mật khẩu không được để trống")
            return
        }

        _emailSignInState.value = EmailSignInState.Loading
        viewModelScope.launch {
            try {
                val result = authUseCases.loginWithEmail(email, password)
                result.fold(
                    onSuccess = { user ->
                        _emailSignInState.value = EmailSignInState.Success(user)
                        _authState.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _emailSignInState.value = EmailSignInState.Error(exception.message ?: "Đăng nhập thất bại")
                    }
                )
            } catch (e: Exception) {
                _emailSignInState.value = EmailSignInState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _registerState.value = RegisterState.Error("Các trường thông tin không được để trống")
            return
        }

        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            try {
                val result = authUseCases.saveUserToFirebaseUseCase(email, password, displayName)
                result.fold(
                    onSuccess = { user ->
                        _registerState.value = RegisterState.Success(user)
                        _authState.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _registerState.value = RegisterState.Error(exception.message ?: "Đăng ký thất bại")
                    }
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut()
            _authState.value = AuthState.Unauthenticated
            // Reset các state khác
            _googleSignInState.value = GoogleSignInState.Initial
            _emailSignInState.value = EmailSignInState.Initial
            _registerState.value = RegisterState.Initial
        }
    }

    fun resetStates() {
        _googleSignInState.value = GoogleSignInState.Initial
        _emailSignInState.value = EmailSignInState.Initial
        _registerState.value = RegisterState.Initial
    }
}

// Các sealed class để quản lý state

sealed class AuthState {
    object Initial : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}

sealed class GoogleSignInState {
    object Initial : GoogleSignInState()
    object Loading : GoogleSignInState()
    data class Success(val user: FirebaseUser) : GoogleSignInState()
    data class NeedRegistration(val email: String) : GoogleSignInState()
    data class Error(val message: String) : GoogleSignInState()
}

sealed class EmailSignInState {
    object Initial : EmailSignInState()
    object Loading : EmailSignInState()
    data class Success(val user: FirebaseUser) : EmailSignInState()
    data class Error(val message: String) : EmailSignInState()
}

sealed class RegisterState {
    object Initial : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: FirebaseUser) : RegisterState()
    data class Error(val message: String) : RegisterState()
}