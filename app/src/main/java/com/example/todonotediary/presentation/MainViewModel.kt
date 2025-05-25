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
        getCurrentUser()
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            val currentUser = authUseCases.getCurrentUser()
            _user.value = currentUser
            if (currentUser != null) {
                fetchUserData(currentUser.uid)
            }
        }
    }

    private fun fetchUserData(userId: String) {
        viewModelScope.launch {
            authUseCases.getUserDataUseCase(userId).fold(
                onSuccess = { userData ->
                    _displayName.value = userData["displayName"] as? String
                    _avatarUrl.value = userData["avatar_url"] as? String ?: "R.drawable.avt_default"
                },
                onFailure = { error ->
                    // Handle error silently here or add error state if needed
                }
            )
        }
    }

    // Add this function to refresh user data when returning from UserScreen
    fun refreshUserData() {
        _user.value?.let { user ->
            fetchUserData(user.uid)
        }
    }
}