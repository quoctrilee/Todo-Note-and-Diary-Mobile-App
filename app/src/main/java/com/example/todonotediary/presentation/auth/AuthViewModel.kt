package com.example.todonotediary.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    val googleSignInClient: GoogleSignInClient
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = AuthUiState.Unauthenticated
            return
        }

        // Nếu tìm thấy user trên Firebase Auth, phải check xem họ có data trong Firestore chưa
        _uiState.value = AuthUiState.Loading // Hiện loading nhẹ để check
        viewModelScope.launch {
            // Bạn có thể gọi hàm getUserData có sẵn trong Repo (thông qua UseCases)
            authUseCases.getUserDataUseCase(currentUser.uid).fold(
                onSuccess = { data ->
                    if (data.isNotEmpty()) {
                        // Đã có data -> User cũ hợp lệ -> Vào thẳng Main
                        _uiState.value = AuthUiState.Authenticated(currentUser)
                    } else {
                        // Chưa có data -> User dở dang từ luồng Google -> Đá về màn hình đăng ký hoàn thiện!
                        _uiState.value = AuthUiState.NeedRegistration(currentUser.email ?: "")
                    }
                },
                onFailure = {
                    // Lỗi kết nối hoặc không tìm thấy data -> Coi như chưa đăng nhập an toàn
                    _uiState.value = AuthUiState.Unauthenticated
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading // ➔ Bật xoay xoay loading chung
        viewModelScope.launch {
            authUseCases.signInWithGoogle(idToken).fold(
                onSuccess = { (user, userExists) ->
                    _uiState.value = if (userExists) {
                        AuthUiState.Authenticated(user)
                    } else {
                        AuthUiState.NeedRegistration(user.email ?: "")
                    }
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState.Error(exception.message ?: "Đăng nhập thất bại")
                }
            )
        }
    }

    fun cancelGoogleSignIn() {
        viewModelScope.launch {
            authUseCases.signOut()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email và mật khẩu không được để trống")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authUseCases.loginWithEmail(email, password).fold(
                onSuccess = { user -> _uiState.value = AuthUiState.Authenticated(user) },
                onFailure = { exception -> _uiState.value = AuthUiState.Error(exception.message ?: "Đăng nhập thất bại") }
            )
        }
    }

    fun register(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Các trường thông tin không được để trống")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            // Gọi UseCase -> UseCase tự chạy bộ lọc Validate bên trong nó
            authUseCases.saveUserToFirebaseUseCase(email, password, displayName).fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    // Nếu UseCase quăng ra lỗi Validate hoặc Firebase trả về lỗi,
                    // chúng ta chỉ cần hốt cái message đó đưa thẳng lên UI
                    _uiState.value = AuthUiState.Error(exception.message ?: "Đăng ký thất bại")
                }
            )
        }
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    object Unauthenticated : AuthUiState()

    // Đăng nhập/Đăng ký thành công thì nhảy vào đây
    data class Authenticated(val user: FirebaseUser) : AuthUiState()

    // Case đặc biệt của Google: Đăng nhập được nhưng chưa có tài khoản ở DB
    data class NeedRegistration(val email: String) : AuthUiState()

    // Có lỗi xảy ra (bất kể lỗi từ nguồn nào)
    data class Error(val message: String) : AuthUiState()
}