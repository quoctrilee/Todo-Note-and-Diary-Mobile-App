package com.example.todonotediary.presentation.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.todonotediary.presentation.todo.add.AddTodoEvent
import com.example.todonotediary.presentation.todo.add.AddTodoUiEvent
import com.example.todonotediary.presentation.todo.add.AddTodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: AddTodoViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AddTodoUiEvent.TodoSaved -> {
                    Toast.makeText(context, "Công việc đã được lưu", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is AddTodoUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                else -> {} // thêm dòng này
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm công việc mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (state.isFormValid && !state.isLoading) {
                        viewModel.onEvent(AddTodoEvent.OnSaveTodo)
                    } else if (!state.isLoading) {
                        // Show validation warnings if user tries to save an invalid form
                        if (state.title.isEmpty()) {
                            Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show()
                        } else if (state.startAt == null) {
                            Toast.makeText(context, "Vui lòng chọn thời gian bắt đầu", Toast.LENGTH_SHORT).show()
                        } else if (state.deadline == null) {
                            Toast.makeText(context, "Vui lòng chọn thời hạn hoàn thành", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                containerColor = if (state.isFormValid && !state.isLoading)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), // làm mờ
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Icons.Default.Done, contentDescription = "Lưu")
                }
            }
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onEvent(AddTodoEvent.OnTitleChanged(it)) },
                label = { Text("Tiêu đề *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.showTitleError && state.title.isEmpty(),
                supportingText = {
                    if (state.showTitleError && state.title.isEmpty()) {
                        Text(
                            text = "Tiêu đề không được để trống",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            // Description field
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onEvent(AddTodoEvent.OnDescriptionChanged(it)) },
                label = { Text("Mô tả") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // Time settings card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lịch trình",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Start time - Required
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDateTimePicker(context, true) { startTime ->
                                viewModel.onEvent(AddTodoEvent.OnStartTimeChanged(startTime))
                            }}
                            .background(
                                if (state.showStartTimeError && state.startAt == null)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Thời gian bắt đầu",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Thời gian bắt đầu *",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = state.startAt?.let { formatDateTime(it) } ?: "Chọn thời gian",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.startAt != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            if (state.showStartTimeError && state.startAt == null) {
                                Text(
                                    text = "Thời gian bắt đầu là bắt buộc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Remove the ability to clear required startAt field
                        if (state.startAt != null) {
                            IconButton(onClick = {
                                // Optionally display a toast explaining it's required
                                Toast.makeText(context, "Thời gian bắt đầu là bắt buộc", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Không thể xóa thời gian bắt đầu",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Deadline - Required
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDateTimePicker(context, false) { deadline ->
                                viewModel.onEvent(AddTodoEvent.OnDeadlineChanged(deadline))
                            }}
                            .background(
                                if (state.showDeadlineError && state.deadline == null)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Thời hạn",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Thời hạn hoàn thành *",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = state.deadline?.let { formatDateTime(it) } ?: "Chọn thời hạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.deadline != null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            if (state.showDeadlineError && state.deadline == null) {
                                Text(
                                    text = "Thời hạn hoàn thành là bắt buộc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Remove the ability to clear required deadline field
                        if (state.deadline != null) {
                            IconButton(onClick = {
                                // Optionally display a toast explaining it's required
                                Toast.makeText(context, "Thời hạn hoàn thành là bắt buộc", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Không thể xóa thời hạn",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Add warning text if deadline is before startAt
                    if (state.startAt != null && state.deadline != null && state.deadline < state.startAt) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lưu ý: Thời hạn hoàn thành đang sớm hơn thời gian bắt đầu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }

            // Hướng dẫn
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mẹo tạo công việc hiệu quả",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Đặt tiêu đề ngắn gọn và rõ ràng\n• Thêm mô tả chi tiết khi cần thiết\n• Đặt thời gian bắt đầu để biết khi nào bắt đầu công việc\n• Đặt thời hạn hợp lý để đảm bảo hoàn thành đúng thời gian",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Required fields note
            Text(
                text = "* Các trường bắt buộc",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    isStartTime: Boolean,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // Show time picker after date is selected
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    onDateTimeSelected(calendar.timeInMillis)
                    val selectedTime = calendar.timeInMillis
                    Log.d("DEBUG_TIME_PICKER FROM SCREEN", "Selected time: $selectedTime (${Date(selectedTime)})")
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)

    )

    datePickerDialog.show()
}

private fun formatDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
    return dateFormat.format(Date(timestamp))
}