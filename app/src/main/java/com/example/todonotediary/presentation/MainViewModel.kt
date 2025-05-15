package com.example.todonotediary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user

    private val _userData = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userData: StateFlow<Map<String, Any>> = _userData

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _user.value = authUseCases.getCurrentUser()
            _user.value?.uid?.let { userId ->
                fetchUserData(userId)
            }
        }
    }

    private fun fetchUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authUseCases.getUserDataUseCase(userId)
                .onSuccess { data ->
                    _userData.value = data
                    _displayName.value = data["displayName"] as? String
                    _avatarUrl.value = data["avatar_url"] as? String
                    _error.value = null
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load user data"
                }
            _isLoading.value = false
        }
    }

    fun refreshUserData() {
        _user.value?.uid?.let { userId ->
            fetchUserData(userId)
        }
    }
}