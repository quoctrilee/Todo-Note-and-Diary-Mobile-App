package com.example.todonotediary.presentation.diary

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.data.local.preferences.DiaryPreferences
import com.example.todonotediary.domain.model.DiaryResponse
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
    private val authUseCases: AuthUseCases,
    private val diaryPreferences: DiaryPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "AddDiaryViewModel"
    }

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
                        // Check if sentiment analysis is enabled BEFORE emitting success
                        val shouldAnalyzeSentiment = diaryPreferences.isSentimentAnalysisEnabled()
                        
                        if (shouldAnalyzeSentiment) {
                            // Set analyzing state BEFORE analyzing
                            state = state.copy(isAnalyzingSentiment = true)
                            _uiEvent.emit(UiEvent.SaveDiarySuccess)
                            analyzeSentimentAndGenerateResponse()
                        } else {
                            // No sentiment analysis, emit success and user can navigate back
                            _uiEvent.emit(UiEvent.SaveDiarySuccess)
                        }
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
    
    /**
     * Analyze diary sentiment and generate AI response
     */
    private fun analyzeSentimentAndGenerateResponse() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 Starting sentiment analysis...")
                // State already set to isAnalyzingSentiment = true in saveDiary()
                
                // Step 1: Analyze sentiment
                val sentimentResult = diaryUseCases.analyzeDiarySentiment(state.content)
                
                sentimentResult.fold(
                    onSuccess = { sentiment ->
                        Log.d(TAG, "✅ Sentiment: ${sentiment.label} (${sentiment.score})")
                        
                        // Step 2: Generate AI response based on sentiment
                        diaryUseCases.generateDiaryResponse(state.content, sentiment).fold(
                            onSuccess = { diaryResponse ->
                                Log.d(TAG, "✅ AI Response generated")
                                _uiEvent.emit(UiEvent.ShowDiaryResponse(diaryResponse))
                            },
                            onFailure = { error ->
                                Log.e(TAG, "❌ Failed to generate response: ${error.message}")
                                // Don't show error to user, just log it
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Sentiment analysis failed: ${error.message}")
                        // Don't show error to user, just log it
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in sentiment analysis: ${e.message}", e)
            } finally {
                state = state.copy(isAnalyzingSentiment = false)
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
    val isLoading: Boolean = false,
    val isAnalyzingSentiment: Boolean = false
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
    data class ShowDiaryResponse(val response: DiaryResponse) : UiEvent()
}