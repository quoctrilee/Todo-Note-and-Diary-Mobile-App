package com.example.todonotediary.presentation.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.diary.DiaryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryUseCases: DiaryUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState = _uiState.asStateFlow()

    // Get userId only when needed to avoid initialization issues
    private val userId: String
        get() = getCurrentUserId() ?: ""

    fun getCurrentUserId(): String? {
        return authUseCases.getCurrentUser()?.uid
    }

    fun initializeData() {
        if (userId.isNotEmpty()) {
            // Initialize with current date selected
            val currentDate = Calendar.getInstance().timeInMillis
            val normalizedDate = normalizeDate(currentDate)
            _uiState.update { it.copy(selectedDate = normalizedDate) }
            // Get diaries for the current date
            getDiariesByDate(userId, normalizedDate)
        } else {
            _uiState.update { it.copy(error = "User not authenticated") }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotEmpty()) {
            searchDiaries(query)
        } else {
            // If query is empty, show diaries for selected date or all diaries
            uiState.value.selectedDate?.let {
                getDiariesByDate(userId, it)
            } ?: getAllDiaries(userId)
        }
    }

    fun getAllDiaries(userId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                diaryUseCases.getDiaries(userId).collect { diaries ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            diaries = diaries,
                            filteredDiaries = diaries,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Failed to load diaries",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchDiaries(query: String) {
        if (userId.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                diaryUseCases.getDiariesByTitleAndContentUseCase(userId, query).collect { diaries ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            filteredDiaries = diaries,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Search failed",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectDate(selectedDate: Long) {
        val normalizedDate = normalizeDate(selectedDate)
        _uiState.update { it.copy(selectedDate = normalizedDate, isCalendarVisible = false) }
        getDiariesByDate(userId, normalizedDate)
    }

    private fun getDiariesByDate(userId: String, date: Long) {
        if (userId.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                diaryUseCases.getDiariesByDateUseCase(userId, date).collect { diaries ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            filteredDiaries = diaries,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message ?: "Failed to get diaries for selected date",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearDateFilter() {
        _uiState.update { it.copy(selectedDate = null) }
        if (_uiState.value.searchQuery.isEmpty()) {
            getAllDiaries(userId)
        } else {
            searchDiaries(_uiState.value.searchQuery)
        }
    }

    fun toggleCalendarVisibility() {
        _uiState.update { it.copy(isCalendarVisible = !it.isCalendarVisible) }
    }

    fun hideCalendar() {
        _uiState.update { it.copy(isCalendarVisible = false) }
    }

    fun deleteDiary(diaryId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            diaryUseCases.deleteDiary(diaryId).fold(
                onSuccess = {
                    // Refresh diaries list after deletion
                    uiState.value.selectedDate?.let {
                        getDiariesByDate(userId, it)
                    } ?: if (uiState.value.searchQuery.isNotEmpty()) {
                        searchDiaries(uiState.value.searchQuery)
                    } else {
                        getAllDiaries(userId)
                    }
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            error = error.message ?: "Error deleting diary",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Helper function to generate date range for date selector
    fun generateDateRange(daysCount: Int = 30): List<Long> {
        val dateList = mutableListOf<Long>()
        val calendar = Calendar.getInstance()

        // Reset time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        for (i in 0 until daysCount) {
            val dayCalendar = calendar.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            dateList.add(dayCalendar.timeInMillis)
        }

        return dateList
    }

    // Function to convert any date to start of day (00:00:00)
    fun normalizeDate(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

data class DiaryUiState(
    val diaries: List<DiaryEntity> = emptyList(),
    val filteredDiaries: List<DiaryEntity> = emptyList(),
    val selectedDate: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCalendarVisible: Boolean = false
)