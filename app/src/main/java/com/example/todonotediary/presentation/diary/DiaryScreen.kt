package com.example.todonotediary.presentation.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todonotediary.domain.model.DiaryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel = hiltViewModel(),
    onNavigateToDiaryDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling with snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss"
            )
            viewModel.clearError()
        }
    }

    // Initialize data on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeData()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            DiaryHeader(
                hasDateFilter = uiState.selectedDate != null,
                onClearDateFilter = { viewModel.clearDateFilter() },
                onCalendarClick = { viewModel.toggleCalendarVisibility() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CustomSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Only show DateSelector if calendar is not visible
                if (!uiState.isCalendarVisible) {
                    // Date Selector
                    DateSelector(
                        dates = viewModel.generateDateRange(),
                        selectedDate = uiState.selectedDate,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }

                // Calendar Picker when visible
                AnimatedVisibility(visible = uiState.isCalendarVisible) {
                    CalendarPicker(
                        initialDate = uiState.selectedDate ?: Calendar.getInstance().timeInMillis,
                        onDateSelected = { viewModel.selectDate(it) },
                        onDismiss = { viewModel.hideCalendar() }
                    )
                }

                // Diary List
                DiaryList(
                    diaries = uiState.filteredDiaries,
                    onDiaryClick = { onNavigateToDiaryDetail(it.id) },
                    onDeleteClick = { viewModel.deleteDiary(it.id) }
                )

                // Empty state
                if (uiState.filteredDiaries.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) {
                                "No diaries found for '${uiState.searchQuery}'"
                            } else if (uiState.selectedDate != null) {
                                "No diaries for selected date"
                            } else {
                                "No diaries available"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Loading state
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DiaryHeader(
    hasDateFilter: Boolean = false,
    onClearDateFilter: () -> Unit = {},
    onCalendarClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "My Diaries",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasDateFilter) {
                IconButton(onClick = onClearDateFilter) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear date filter"
                    )
                }
            }

            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Calendar"
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
                contentDescription = "Search icon",
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
                                text = "Search diaries",
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
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    dates: List<Long>,
    selectedDate: Long? = null,
    onDateSelected: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(dates) { date ->
            val isSelected = selectedDate?.let { selected ->
                // Compare by day, month, year (ignore time)
                val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selected }
                val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }

                selectedCalendar.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
                        selectedCalendar.get(Calendar.MONTH) == dateCalendar.get(Calendar.MONTH) &&
                        selectedCalendar.get(Calendar.DAY_OF_MONTH) == dateCalendar.get(Calendar.DAY_OF_MONTH)
            } ?: false

            DateItem(
                date = date,
                isSelected = isSelected,
                onClick = { onDateSelected(date) }
            )
        }
    }
}

@Composable
fun DateItem(
    date: Long,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val weekdayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val day = dayFormat.format(Date(date))
    val month = monthFormat.format(Date(date))
    val weekday = weekdayFormat.format(Date(date))

    Card(
        modifier = Modifier
            .width(70.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color(0xFF2196F3) // màu xanh lam (Blue 500)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )


    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = day,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = weekday,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DiaryList(
    diaries: List<DiaryEntity>,
    onDiaryClick: (DiaryEntity) -> Unit = {},
    onDeleteClick: (DiaryEntity) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(diaries) { diary ->
            DiaryCard(
                diary = diary,
                onClick = { onDiaryClick(diary) },
                onDeleteClick = { onDeleteClick(diary) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryCard(
    diary: DiaryEntity,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val moodColor = getMoodColor(diary.mood)

    // Determine appropriate text color based on background color
    val textColor = if (diary.mood.isNotEmpty()) {
        // For dark backgrounds, use white text; for light backgrounds, use dark text
        if (isColorDark(moodColor)) Color.White else Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (diary.mood.isNotEmpty()) moodColor
            else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = textColor
                )

                Box {
                    IconButton(
                        onClick = { showOptions = !showOptions },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = if (diary.mood.isNotEmpty() && isColorDark(moodColor))
                                Color.White.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = "Delete") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete"
                                )
                            },
                            onClick = {
                                showOptions = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = diary.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(diary.date)),
                style = MaterialTheme.typography.bodySmall,
                color = if (diary.mood.isNotEmpty() && isColorDark(moodColor))
                    Color.White.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Diary") },
            text = { Text("Are you sure you want to delete this diary entry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun getMoodColor(mood: String): Color {
    // Enhanced color palette with more subtle pastel colors
    return when (mood.lowercase()) {
        "happy" -> Color(0xFFB5E8B5)      // Light Pastel Green
        "sad" -> Color(0xFFB5D8E8)        // Light Pastel Blue
        "excited" -> Color(0xFFFFD6A5)    // Light Pastel Orange/Peach
        "calm" -> Color(0xFFD4E2D4)       // Mint Green / Sage
        "frustrated" -> Color(0xFFFFCCCC) // Light Pastel Red/Pink
        "angry" -> Color(0xFFFFDAD6)      // Lighter Red
        "anxious" -> Color(0xFFFFECB3)    // Light Pastel Yellow
        "neutral" -> Color(0xFFE0E0E0)    // Light Gray
        else -> MaterialTheme.colorScheme.surface
    }
}

// Helper function to determine if a color is dark
fun isColorDark(color: Color): Boolean {
    // Color brightness formula (ITU-R BT.709)
    val brightness = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return brightness < 0.5
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPicker(
    initialDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Calendar"
                    )
                }
            }

            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(it)
                        }
                        onDismiss()
                    }
                ) {
                    Text("Select")
                }
            }
        }
    }
}