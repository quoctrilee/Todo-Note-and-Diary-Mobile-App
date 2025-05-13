package com.example.todonotediary.presentation.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.diary.DiaryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddDiaryViewModel @Inject constructor(
    private val diaryUseCases: DiaryUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    // Sử dụng data class để quản lý state
    var state by mutableStateOf(AddDiaryState())
        private set

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Event handlers for UI events
    fun onEvent(event: AddDiaryEvent) {
        when (event) {
            is AddDiaryEvent.OnTitleChanged -> {
                state = state.copy(title = event.title)
            }
            is AddDiaryEvent.OnContentChanged -> {
                state = state.copy(content = event.content)
            }
            is AddDiaryEvent.OnMoodChanged -> {
                state = state.copy(mood = event.mood)
            }
            is AddDiaryEvent.OnDateSelected -> {
                state = state.copy(selectedDate = event.timestamp)
            }
            is AddDiaryEvent.OnSaveDiary -> {
                saveDiary()
            }
        }
    }

    private fun saveDiary() {
        viewModelScope.launch {
            // Start loading
            state = state.copy(isLoading = true)

            try {
                // Validate form inputs
                if (state.title.isBlank()) {
                    _uiEvent.emit(UiEvent.ShowError("Title cannot be empty"))
                    state = state.copy(isLoading = false)
                    return@launch
                }

                if (state.content.isBlank()) {
                    _uiEvent.emit(UiEvent.ShowError("Content cannot be empty"))
                    state = state.copy(isLoading = false)
                    return@launch
                }

                // Kiểm tra người dùng đã đăng nhập chưa - pattern từ TodoViewModel
                val currentUser = authUseCases.getCurrentUser() ?: run {
                    _uiEvent.emit(UiEvent.ShowError("You need to login to add a diary"))
                    state = state.copy(isLoading = false)
                    return@launch
                }

                // Tiếp tục lưu nhật ký nếu người dùng đã đăng nhập
                diaryUseCases.addDiary(
                    date = state.selectedDate,
                    mood = state.mood,
                    title = state.title,
                    content = state.content,
                    userId = currentUser.uid // Sử dụng uid từ currentUser
                ).fold(
                    onSuccess = { diary ->
                        _uiEvent.emit(UiEvent.SaveDiarySuccess)
                    },
                    onFailure = { error ->
                        _uiEvent.emit(UiEvent.ShowError(error.message ?: "Failed to save diary"))
                    }
                )
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("An error occurred: ${e.message}"))
            } finally {
                // End loading
                state = state.copy(isLoading = false)
            }
        }
    }
}

// State class to manage all state in one place
data class AddDiaryState(
    val title: String = "",
    val content: String = "",
    val mood: String = "happy",
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

// Event sealed class for UI events
sealed class AddDiaryEvent {
    data class OnTitleChanged(val title: String) : AddDiaryEvent()
    data class OnContentChanged(val content: String) : AddDiaryEvent()
    data class OnMoodChanged(val mood: String) : AddDiaryEvent()
    data class OnDateSelected(val timestamp: Long) : AddDiaryEvent()
    object OnSaveDiary : AddDiaryEvent()
}

// UI event sealed class for one-time events
sealed class UiEvent {
    object SaveDiarySuccess : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}