package com.example.todonotediary.presentation.todo.add

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTodoViewModel @Inject constructor(
    private val todoUseCases: TodoUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    var state by mutableStateOf(AddTodoState())
        private set

    private val _uiEvent = MutableSharedFlow<AddTodoUiEvent>()
    val uiEvent: SharedFlow<AddTodoUiEvent> = _uiEvent.asSharedFlow()

    fun onEvent(event: AddTodoEvent) {
        when (event) {
            is AddTodoEvent.OnTitleChanged -> {
                state = state.copy(
                    title = event.title,
                    showTitleError = false
                )
                validateForm()
            }
            is AddTodoEvent.OnDescriptionChanged -> {
                state = state.copy(description = event.description)
                validateForm()
            }
            is AddTodoEvent.OnStartTimeChanged -> {
                Log.d("DEBUG_VIEWMODEL", "Start time received: ${event.startTime} (${event.startTime?.let { Date(it) }})")
                state = state.copy(
                    startAt = event.startTime,
                    showStartTimeError = false
                )
                validateForm()
            }
            is AddTodoEvent.OnDeadlineChanged -> {
                Log.d("DEBUG_VIEWMODEL", "Deadline received: ${event.deadline} (${event.deadline?.let { Date(it) }})")
                state = state.copy(
                    deadline = event.deadline,
                    showDeadlineError = false
                )
                validateForm()
            }
            is AddTodoEvent.OnSaveTodo -> {
                if (validateFormBeforeSave()) {
                    saveTodo()
                }
            }
            else -> {}
        }
    }

    private fun validateForm() {
        val isTitleValid = state.title.isNotBlank()
        val isStartTimeValid = state.startAt != null
        val isDeadlineValid = state.deadline != null

        state = state.copy(
            isFormValid = isTitleValid && isStartTimeValid && isDeadlineValid
        )
    }

    private fun validateFormBeforeSave(): Boolean {
        val isTitleValid = state.title.isNotBlank()
        val isStartTimeValid = state.startAt != null
        val isDeadlineValid = state.deadline != null

        // Update state to show appropriate error indicators
        state = state.copy(
            showTitleError = !isTitleValid,
            showStartTimeError = !isStartTimeValid,
            showDeadlineError = !isDeadlineValid,
            isFormValid = isTitleValid && isStartTimeValid && isDeadlineValid
        )

        return state.isFormValid
    }

    private fun saveTodo() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val currentUser = authUseCases.getCurrentUser() ?: run {
                    _uiEvent.emit(AddTodoUiEvent.ShowError("Bạn cần đăng nhập để thêm công việc"))
                    state = state.copy(isLoading = false)
                    return@launch
                }

                val todo = TodoEntity(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.uid,
                    title = state.title,
                    description = state.description,
                    // Các trường dưới đây bây giờ luôn có giá trị vì chúng đã được kiểm tra trong validateForm
                    startAt = state.startAt!!,
                    deadline = state.deadline!!,
                    isCompleted = false,
                    isDeleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastSyncTimestamp = System.currentTimeMillis()
                )

                todoUseCases.addTodo(todo).fold(
                    onSuccess = {
                        _uiEvent.emit(AddTodoUiEvent.TodoSaved)
                    },
                    onFailure = { e ->
                        Log.e("AddTodoViewModel", "Lỗi khi lưu công việc", e)
                        _uiEvent.emit(AddTodoUiEvent.ShowError("Không thể lưu công việc: ${e.message}"))
                    }
                )
            } catch (e: Exception) {
                Log.e("AddTodoViewModel", "Lỗi không xác định", e)
                _uiEvent.emit(AddTodoUiEvent.ShowError("Đã xảy ra lỗi: ${e.message}"))
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }
}

data class AddTodoState(
    val title: String = "",
    val description: String = "",
    val startAt: Long? = null,
    val deadline: Long? = null,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    // Thêm các trạng thái để hiển thị lỗi cho từng trường
    val showTitleError: Boolean = false,
    val showStartTimeError: Boolean = false,
    val showDeadlineError: Boolean = false
)

sealed class AddTodoEvent {
    data class OnTitleChanged(val title: String) : AddTodoEvent()
    data class OnDescriptionChanged(val description: String) : AddTodoEvent()
    data class OnStartTimeChanged(val startTime: Long?) : AddTodoEvent()
    data class OnDeadlineChanged(val deadline: Long?) : AddTodoEvent()
    object OnSaveTodo : AddTodoEvent()
}

sealed class AddTodoUiEvent {
    object TodoSaved : AddTodoUiEvent()
    data class ShowError(val message: String) : AddTodoUiEvent()
}