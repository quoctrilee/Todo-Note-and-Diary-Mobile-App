package com.example.todonotediary.presentation.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todonotediary.domain.model.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit = {},
    onAddNoteClick: () -> Unit = {}
) {
    // Khởi tạo ViewModel khi màn hình được tạo
    LaunchedEffect(key1 = Unit) {
        viewModel.init()
    }

    // Thu thập state từ ViewModel
    val state by viewModel.state
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Notes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    )
                },
                modifier = Modifier.offset(y = (-16).dp) // Đẩy TopAppBar lên trên 12.dp
            )
        },
    )
    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Thanh tìm kiếm
            CustomSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onEvent(NotesEvent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            val categories by viewModel.categories.collectAsState()
            // Danh mục
            CategoriesRow(

                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.onEvent(NotesEvent.CategorySelected(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lưới ghi chú
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.notes.isEmpty()) {
                EmptyNotesMessage()
            } else {
                NotesGrid(
                    notes = state.notes,
                    onNoteClick = onNoteClick,
                    onDeleteClick = { viewModel.onEvent(NotesEvent.DeleteNote(it)) }
                )
            }
        }
    }
}

@Composable
fun CustomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Biểu tượng tìm kiếm",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Tìm kiếm ghi chú",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xóa tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(categories) { _, category ->
            CategoryChip(
                category = category.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                },
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun NotesGrid(
    notes: List<NoteEntity>,
    onNoteClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(notes) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDeleteClick = { onDeleteClick(note.id) }
            )
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val backgroundColor = parseColor(
        note.background_color,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Tiêu đề
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    // Điều chỉnh màu chữ dựa trên màu nền để đảm bảo độ tương phản
                    color = getTextColorForBackground(backgroundColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Xem trước nội dung
                Text(
                    text = note.content,
                    fontSize = 14.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // Điều chỉnh màu chữ dựa trên màu nền để đảm bảo độ tương phản
                    color = getTextColorForBackground(backgroundColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ngày tháng
                Text(
                    text = formatDate(note.createdAt),
                    fontSize = 12.sp,
                    // Điều chỉnh màu chữ dựa trên màu nền để đảm bảo độ tương phản
                    color = getTextColorForBackground(backgroundColor).copy(alpha = 0.7f)
                )
            }

            // Menu tùy chọn
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = getTextColorForBackground(backgroundColor),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa ghi chú"
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirmation = true
                        }
                    )
                }
            }
        }
    }

    // Hộp thoại xác nhận xóa
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Xóa ghi chú") },
            text = { Text("Bạn có chắc chắn muốn xóa ghi chú này không?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick()
                    showDeleteConfirmation = false
                }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun EmptyNotesMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Không tìm thấy ghi chú nào",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Thêm ghi chú mới để bắt đầu",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Hàm trợ giúp để định dạng ngày tháng
private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Ngày không xác định"
    }
}

private fun parseColor(colorString: String, defaultColor: Color): Color {
    return try {
        if (colorString.isEmpty()) return defaultColor
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        defaultColor
    }
}


// Hàm tính toán màu văn bản phù hợp dựa trên màu nền
private fun getTextColorForBackground(backgroundColor: Color): Color {
    // Tính toán độ sáng của màu nền (công thức YIQ)
    val brightness = (backgroundColor.red * 299 + backgroundColor.green * 587 + backgroundColor.blue * 114) / 1000

    // Nếu màu nền sáng, sử dụng chữ tối và ngược lại
    return if (brightness > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}