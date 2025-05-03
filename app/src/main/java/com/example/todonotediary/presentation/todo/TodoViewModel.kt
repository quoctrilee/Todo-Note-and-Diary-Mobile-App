//package com.example.todonotediary.presentation.todo
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.todonotediary.domain.model.TodoEntity
//import com.example.todonotediary.domain.usecase.todo.TodoUseCases
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import javax.inject.Inject
//
//@HiltViewModel
//class TodoViewModel @Inject constructor(
//    private val todoUseCases: TodoUseCases
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<Map<String, List<TodoEntity>>>(emptyMap())
//    val state: StateFlow<Map<String, List<TodoEntity>>> = _state.asStateFlow()
//
//    fun loadTodos(userId: String) {
//        viewModelScope.launch {
//            todoUseCases.getTodos(userId).collect { todos ->
//                val grouped = todos.groupBy {
//                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.createdAt))
//                }
//                _state.update { grouped }
//            }
//        }
//    }
//
//    fun toggleCompleted(todo: TodoEntity) {
//        viewModelScope.launch {
//            todoUseCases.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
//        }
//    }
//}