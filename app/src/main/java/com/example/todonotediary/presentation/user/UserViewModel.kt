package com.example.todonotediary.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val currentUser = authUseCases.getCurrentUser()
            if (currentUser != null) {
                _state.update { it.copy(isLoading = true) }

                authUseCases.getUserDataUseCase(currentUser.uid).fold(
                    onSuccess = { userData ->
                        _state.update {
                            it.copy(
                                displayName = userData["displayName"] as? String ?: "",
                                avatarUrl = userData["avatar_url"] as? String ?: "R.drawable.avt_default",
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                error = error.message ?: "Failed to load user data",
                                isLoading = false
                            )
                        }
                    }
                )
            }
        }
    }

    fun updateAvatar(avatarName: String) {
        viewModelScope.launch {
            val currentUser = authUseCases.getCurrentUser()
            if (currentUser != null) {
                _state.update { it.copy(isLoading = true) }

                authUseCases.updateUserAvatarUseCase(currentUser.uid, avatarName).fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                avatarUrl = avatarName,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                error = error.message ?: "Failed to update avatar",
                                isLoading = false
                            )
                        }
                    }
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut()
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}


data class UserState(
    val displayName: String = "",
    val avatarUrl: String = "R.drawable.avt_default",
    val isLoading: Boolean = false,
    val error: String? = null
)