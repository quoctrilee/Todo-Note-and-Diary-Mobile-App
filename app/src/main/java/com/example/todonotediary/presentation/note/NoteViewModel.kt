package com.example.todonotediary.presentation.note

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.note.NoteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = mutableStateOf(NotesState())
    val state: State<NotesState> = _state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Danh sách danh mục mặc định và từ Firestore
    private val _categories = mutableListOf("all")
    val categories: List<String> get() = _categories

    private var getNotesJob: Job? = null
    private var searchJob: Job? = null
    private var categoriesJob: Job? = null

    // Lấy ID người dùng hiện tại
    private val userId: String by lazy {
        getCurrentUserId() ?: ""
    }

    fun getCurrentUserId(): String? {
        val currentUser = authUseCases.getCurrentUser()
        return currentUser?.uid
    }

    // Khởi tạo dữ liệu khi màn hình được tạo
    fun init() {
        loadCategories()
        getNotes()
    }

    // Xử lý các sự kiện từ UI
    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
                searchNotes()
            }
            is NotesEvent.CategorySelected -> {
                _selectedCategory.value = event.category
                if (event.category == "all") {
                    getNotes()
                } else {
                    getNotesByCategory(event.category)
                }
            }
            is NotesEvent.DeleteNote -> {
                deleteNote(event.noteId)
            }
            is NotesEvent.RefreshNotes -> {
                loadCategories()
                getNotes()
            }
        }
    }

    // Tải danh sách danh mục từ Firestore
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val firestoreCategories = noteUseCases.getCategoryUseCase(userId)
                _categories.clear()
                _categories.add("all")  // Luôn có danh mục "all"
                _categories.addAll(firestoreCategories)
            } catch (e: Exception) {
                // Nếu có lỗi, đảm bảo luôn có danh mục "all"
                _categories.clear()
                _categories.add("all")
            }
        }
    }

    // Lấy tất cả ghi chú của người dùng
    private fun getNotes() {
        getNotesJob?.cancel()
        getNotesJob = noteUseCases.getNotes(userId)
            .onEach { notes ->
                // Lọc ra những ghi chú chưa bị xóa và sắp xếp theo thời gian tạo mới nhất
                val filteredNotes = notes
                    .filter { !it.isDeleted }
                    .sortedByDescending { it.createdAt }
                    .map { ensureValidBackgroundColor(it) } // Đảm bảo tất cả note đều có màu nền hợp lệ

                _state.value = state.value.copy(
                    notes = filteredNotes,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    // Lấy ghi chú theo danh mục
    private fun getNotesByCategory(category: String) {
        getNotesJob?.cancel()
        getNotesJob = noteUseCases.getNotesByCategory(userId, category)
            .onEach { notes ->
                // Chỉ hiển thị những ghi chú chưa bị xóa
                val filteredNotes = notes
                    .filter { !it.isDeleted }
                    .sortedByDescending { it.createdAt }
                    .map { ensureValidBackgroundColor(it) } // Đảm bảo tất cả note đều có màu nền hợp lệ

                _state.value = state.value.copy(
                    notes = filteredNotes,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    // Tìm kiếm ghi chú theo tiêu đề hoặc nội dung
    private fun searchNotes() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500L) // Debounce 500ms để tránh gọi API liên tục khi người dùng gõ nhanh

            if (_searchQuery.value.isNotEmpty()) {
                noteUseCases.searchNotesByTitleOrContentUseCase(userId, _searchQuery.value)
                    .onEach { notes ->
                        // Chỉ hiển thị những ghi chú chưa bị xóa
                        val filteredNotes = notes
                            .filter { !it.isDeleted }
                            .sortedByDescending { it.createdAt }
                            .map { ensureValidBackgroundColor(it) } // Đảm bảo tất cả note đều có màu nền hợp lệ

                        _state.value = state.value.copy(
                            notes = filteredNotes,
                            isLoading = false
                        )
                    }
                    .launchIn(this)
            } else {
                // Nếu tìm kiếm trống, quay lại hiển thị theo danh mục đã chọn
                if (_selectedCategory.value == "all") {
                    getNotes()
                } else {
                    getNotesByCategory(_selectedCategory.value)
                }
            }
        }
    }

    // Hàm đảm bảo note có màu nền hợp lệ
    private fun ensureValidBackgroundColor(note: NoteEntity): NoteEntity {
        // Nếu màu nền trống, gán màu mặc định
        if (note.background_color.isBlank()) {
            val defaultColors = listOf(
                "#FFECB3", // Vàng nhạt
                "#C8E6C9", // Xanh lá nhạt
                "#BBDEFB", // Xanh dương nhạt
                "#D1C4E9", // Tím nhạt
                "#FFCCBC", // Cam nhạt
                "#F0F4C3", // Vàng xanh nhạt
                "#B2DFDB", // Xanh lá đậm hơn
                "#CFD8DC"  // Xám nhạt
            )

            // Chọn màu dựa trên hash của id ghi chú để đảm bảo màu ổn định cho mỗi ghi chú
            val colorIndex = note.id.hashCode().rem(defaultColors.size).let {
                if (it < 0) it + defaultColors.size else it
            }

            return note.copy(background_color = defaultColors[colorIndex])
        }
        return note
    }

    // Xóa ghi chú (thực tế là đánh dấu đã xóa)
    private fun deleteNote(noteId: String) {
        viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)

            val result = noteUseCases.deleteNote(noteId)

            if (result.isSuccess) {
                // Làm mới danh sách ghi chú sau khi xóa
                if (_selectedCategory.value == "all") {
                    getNotes()
                } else {
                    getNotesByCategory(_selectedCategory.value)
                }
            } else {
                // Xử lý lỗi nếu cần
                _state.value = state.value.copy(
                    error = "Không thể xóa ghi chú",
                    isLoading = false
                )
            }
        }
    }

    // Hủy các job khi ViewModel bị hủy
    override fun onCleared() {
        getNotesJob?.cancel()
        searchJob?.cancel()
        categoriesJob?.cancel()
        super.onCleared()
    }
}

// State của màn hình hiển thị ghi chú
data class NotesState(
    val notes: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

// Các sự kiện từ UI
sealed class NotesEvent {
    data class SearchQueryChanged(val query: String) : NotesEvent()
    data class CategorySelected(val category: String) : NotesEvent()
    data class DeleteNote(val noteId: String) : NotesEvent()
    object RefreshNotes : NotesEvent()
}