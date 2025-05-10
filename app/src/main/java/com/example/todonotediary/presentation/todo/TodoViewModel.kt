package com.example.todonotediary.presentation.todo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoUseCases: TodoUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    // State for UI
    private val _todoState = MutableStateFlow(TodoState())
    val todoState: StateFlow<TodoState> = _todoState.asStateFlow()

    fun getCurrentUserId(): String? {
        val currentUser = authUseCases.getCurrentUser()
        return currentUser?.uid
    }
    // User Id (in reality, would be fetched from AuthRepository)
    private val userId: String = getCurrentUserId()!!

    // Default value is the current date
    private val calendar = Calendar.getInstance()
    private var currentDate = calendar.timeInMillis

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))


    init {
        // Initialize with "Upcoming" tab selected
        switchTab(TodoTab.UPCOMING)
        // Get current date and set selected date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        selectDate(getDayName(dayOfWeek), dayOfMonth.toString(), currentDate)
    }

    /**
     * Switch between Upcoming and Past tabs
     */
    fun switchTab(tab: TodoTab) {
        viewModelScope.launch {
            _todoState.value = _todoState.value.copy(
                selectedTab = tab,
                isLoading = true
            )

            val selectedDate = _todoState.value.selectedDate

            when (tab) {
                TodoTab.UPCOMING -> {
                    todoUseCases.getTodoUpcoming(userId, selectedDate).collectLatest { todos ->
                        // Sort todos: first by start time, then by deadline
                        val sortedTodos = todos.sortedWith(compareBy(
                            { it.startAt ?: Long.MAX_VALUE },  // Sort by start time first
                            { it.deadline ?: Long.MAX_VALUE }  // Then by deadline
                        ))

                        _todoState.value = _todoState.value.copy(
                            todoList = sortedTodos,
                            isLoading = false
                        )
                    }
                }
                TodoTab.LAST -> {
                    todoUseCases.getTodoPast(userId, selectedDate).collectLatest { todos ->
                        // For past tasks, show completed items first, then sort by deadline
                        val sortedTodos = todos.sortedWith(
                            compareByDescending<TodoEntity> { it.isCompleted }
                                .thenBy { it.deadline ?: Long.MAX_VALUE }
                        )

                        _todoState.value = _todoState.value.copy(
                            todoList = sortedTodos,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Select a specific date to display todos
     * Enhanced to handle multi-day tasks
     */
    fun selectDate(dayOfWeek: String, dayNumber: String, date: Long) {
        viewModelScope.launch {
            _todoState.value = _todoState.value.copy(
                selectedDayOfWeek = dayOfWeek,
                selectedDayNumber = dayNumber,
                selectedDate = date,
                isLoading = true
            )

            // Call switchTab again to get the list corresponding to the selected tab
            switchTab(_todoState.value.selectedTab)
        }
    }

    /**
     * Update the completion status of a task
     */
    fun toggleTodoCompletion(todoId: String) {
        viewModelScope.launch {
            val result = todoUseCases.toggleTodoCompletion(todoId)

            // If update successful, reload task list
            if (result.isSuccess) {
                refreshTodoList()
            }
        }
    }

    /**
     * Delete a task
     */
    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            val result = todoUseCases.deleteTodo(todoId)

            // If deletion successful, reload task list
            if (result.isSuccess) {
                refreshTodoList()
            }
        }
    }

    /**
     * Reload task list based on current tab
     */
    fun refreshTodoList() {
        when (_todoState.value.selectedTab) {
            TodoTab.UPCOMING -> switchTab(TodoTab.UPCOMING)
            TodoTab.LAST -> switchTab(TodoTab.LAST)
        }
    }

    /**
     * Create list of days in the week
     * Extended to show 14 days instead of 7
     */
    fun generateWeekDays(): List<DayItem> {
        val days = mutableListOf<DayItem>()
        val today = Calendar.getInstance()

        // Go back to get first day of week (Monday)
        val daysToShow = 14 // Show 2 weeks of days
        val startDay = Calendar.getInstance()
        startDay.add(Calendar.DAY_OF_MONTH, -7) // Start 1 week before current day

        for (i in 0 until daysToShow) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDay.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, i)

            val dayOfWeek = getDayName(cal.get(Calendar.DAY_OF_WEEK))
            val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
            val isToday = cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)

            days.add(
                DayItem(
                    dayOfWeek = dayOfWeek,
                    dayNumber = dayNumber,
                    date = cal.timeInMillis,
                    isSelected = isToday
                )
            )
        }

        return days
    }

    /**
     * Convert day of week number to abbreviated name
     */
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
    }

    /**
     * Utility function to check if two timestamps are on the same day
     */
    fun areSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * State for Todo screen
 */
data class TodoState(
    val todoList: List<TodoEntity> = emptyList(),
    val selectedTab: TodoTab = TodoTab.UPCOMING,
    val selectedDayOfWeek: String = "",
    val selectedDayNumber: String = "",
    val selectedDate: Long = 0L,
    val isLoading: Boolean = false
)

/**
 * Enum for tabs
 */
enum class TodoTab {
    UPCOMING, LAST
}

/**
 * Data class for day item
 */
data class DayItem(
    val dayOfWeek: String,
    val dayNumber: String,
    val date: Long,
    val isSelected: Boolean = false
)