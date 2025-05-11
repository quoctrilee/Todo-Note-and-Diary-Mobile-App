package com.example.todonotediary.presentation.note

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.usecase.note.AddNoteUseCase
import com.example.todonotediary.domain.usecase.note.GetCategoryUseCase
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNoteViewModel @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase,
    private val getCategoryUseCase: GetCategoryUseCase,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    // Định nghĩa các trạng thái của ViewModel
    data class AddNoteState(
        val title: String = "",
        val content: String = "",
        val category: String = "",  // Không set giá trị mặc định, sẽ được cập nhật sau khi tải danh mục
        val backgroundColor: Color = Color.White,
        val categories: List<String> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isSuccess: Boolean = false,
        val showCategoryDialog: Boolean = false,
        val newCategoryName: String = ""
    )

    private val _state = MutableStateFlow(AddNoteState())
    val state: StateFlow<AddNoteState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AddNoteUiEvent>()
    val uiEvent: SharedFlow<AddNoteUiEvent> = _uiEvent

    // Các màu nền có sẵn cho ghi chú
    val availableColors = listOf(
        Color.White,
        Color(0xFFFFF8E1), // Vàng nhạt
        Color(0xFFE1F5FE), // Xanh dương nhạt
        Color(0xFFE8F5E9), // Xanh lá nhạt
        Color(0xFFF3E5F5), // Tím nhạt
        Color(0xFFFFEBEE), // Hồng nhạt
        Color(0xFFFBE9E7), // Cam nhạt
        Color(0xFFF1F8E9), // Xanh lá cây nhạt
        Color(0xFFE0F7FA)  // Xanh lơ nhạt
    )

    init {
        loadCategories()
    }

    // Tải danh sách category từ repository
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                // Lấy userId từ AuthUseCases
                val currentUser = authUseCases.getCurrentUser() ?: run {
                    _uiEvent.emit(AddNoteUiEvent.ShowError("Bạn cần đăng nhập để thêm ghi chú"))
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

                val userId = currentUser.uid
                val categories = getCategoryUseCase(userId)

                // Thiết lập danh mục mặc định nếu danh sách rỗng hoặc không có "General"
                val defaultCategory = "General"
                val allCategories = if (categories.isEmpty()) {
                    listOf(defaultCategory)
                } else {
                    categories
                }

                _state.update { it.copy(
                    isLoading = false,
                    categories = allCategories,
                    // Chỉ đặt category mặc định nếu chưa có giá trị và có danh mục có sẵn
                    category = if (it.category.isEmpty() && allCategories.isNotEmpty()) allCategories[0] else it.category
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    errorMessage = "Không thể tải danh sách danh mục: ${e.message}"
                ) }
            }
        }
    }

    // Cập nhật title
    fun onTitleChanged(newTitle: String) {
        _state.update { it.copy(title = newTitle) }
    }

    // Cập nhật content
    fun onContentChanged(newContent: String) {
        _state.update { it.copy(content = newContent) }
    }

    // Cập nhật category
    fun onCategoryChanged(newCategory: String) {
        _state.update { it.copy(category = newCategory) }
    }

    // Cập nhật background color
    fun onBackgroundColorChanged(newColor: Color) {
        _state.update { it.copy(backgroundColor = newColor) }
    }

    // Hiển thị dialog thêm category mới
    fun onShowAddCategoryDialog() {
        _state.update { it.copy(showCategoryDialog = true) }
    }

    // Ẩn dialog thêm category mới
    fun onHideAddCategoryDialog() {
        _state.update { it.copy(showCategoryDialog = false, newCategoryName = "") }
    }

    // Cập nhật tên category mới
    fun onNewCategoryNameChanged(name: String) {
        _state.update { it.copy(newCategoryName = name) }
    }

    // Thêm category mới vào danh sách
    fun onAddNewCategory() {
        val newCategory = _state.value.newCategoryName.trim()
        if (newCategory.isNotEmpty() && !_state.value.categories.contains(newCategory)) {
            val updatedCategories = _state.value.categories + newCategory
            _state.update { it.copy(
                categories = updatedCategories,
                category = newCategory,
                showCategoryDialog = false,
                newCategoryName = ""
            ) }
        } else {
            _state.update { it.copy(
                errorMessage = "Tên danh mục không hợp lệ hoặc đã tồn tại",
                showCategoryDialog = false,
                newCategoryName = ""
            ) }
        }
    }

    // Xóa thông báo lỗi
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    // Lưu ghi chú
    fun saveNote() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                // Lấy userId từ AuthUseCases
                val currentUser = authUseCases.getCurrentUser() ?: run {
                    _uiEvent.emit(AddNoteUiEvent.ShowError("Bạn cần đăng nhập để thêm ghi chú"))
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

                val userId = currentUser.uid
                val currentState = _state.value

                // Kiểm tra trường bắt buộc
                if (currentState.title.isBlank()) {
                    _state.update { it.copy(
                        isLoading = false,
                        errorMessage = "Tiêu đề không được để trống"
                    ) }
                    return@launch
                }

                // Chuyển đổi màu thành chuỗi hex để lưu trữ
                val backgroundColorHex = String.format("#%06X", 0xFFFFFF and currentState.backgroundColor.toArgb())

                // Gọi use case để thêm ghi chú với cả thông tin màu nền
                val result = addNoteUseCase(
                    title = currentState.title,
                    content = currentState.content,
                    userId = userId,
                    category = currentState.category,
                    backgroundColor = backgroundColorHex
                )

                if (result.isSuccess) {
                    _state.update { it.copy(
                        isLoading = false,
                        isSuccess = true
                    ) }
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        errorMessage = "Không thể lưu ghi chú: ${result.exceptionOrNull()?.message}"
                    ) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    errorMessage = "Đã xảy ra lỗi: ${e.message}"
                ) }
            }
        }
    }
}

sealed class AddNoteUiEvent {
    data class ShowError(val message: String) : AddNoteUiEvent()
    object NavigateBack : AddNoteUiEvent()
}