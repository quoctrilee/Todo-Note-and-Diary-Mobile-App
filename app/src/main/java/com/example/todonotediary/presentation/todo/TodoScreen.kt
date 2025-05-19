package com.example.todonotediary.presentation.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todonotediary.domain.model.TodoEntity
import java.text.SimpleDateFormat
import java.util.*

// Light theme colors
private val BackgroundColor = Color(0xFFFFFFFF)
private val SurfaceColor = Color.White
private val PrimaryColor = Color(0xFF5B8EFF)
private val PrimaryVariantColor = Color(0xFF4A7AEC)
private val SecondaryColor = Color(0xFFFFFFFF)
private val TextPrimaryColor = Color(0xFF2C3E50)
private val TextSecondaryColor = Color(0xFF718096)
private val CompletedColor = Color(0xFF4CAF50)
private val CompletedVariantColor = Color(0xFF3DA641)
private val OverdueColor = Color(0xFFFF7675)
private val OverdueVariantColor = Color(0xFFE56565)
private val CardBorderWidth = 1.dp

@Composable
fun TodoScreen(
    onNavigateToAddTodo: () -> Unit = {},
    onNavigateToEditTodo: (String) -> Unit = {},
    viewModel: TodoViewModel = hiltViewModel()
) {
    val todoState by viewModel.todoState.collectAsState()
    val weekDays = remember { viewModel.generateWeekDays() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header and tab section
            TaskHeader(
                selectedTab = todoState.selectedTab,
                onTabSelected = { tab ->
                    viewModel.switchTab(tab)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date selector section
            DateSelector(
                days = weekDays,
                selectedDayOfWeek = todoState.selectedDayOfWeek,
                selectedDayNumber = todoState.selectedDayNumber,
                onDaySelected = { dayItem ->
                    viewModel.selectDate(
                        dayItem.dayOfWeek,
                        dayItem.dayNumber,
                        dayItem.date
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Todo list section
            TodoList(
                todos = todoState.todoList,
                isLoading = todoState.isLoading,
                selectedTab = todoState.selectedTab,
                onTodoClick = onNavigateToEditTodo,
                onTodoCompletionToggle = { todoId ->
                    viewModel.toggleTodoCompletion(todoId)
                },
                onTodoDelete = { todoId ->
                    viewModel.deleteTodo(todoId)
                }
            )
        }
    }
}

@Composable
fun TaskHeader(
    selectedTab: TodoTab,
    onTabSelected: (TodoTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My tasks",
            color = TextPrimaryColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Row {
            FilterTab(
                text = "Upcoming",
                isSelected = selectedTab == TodoTab.UPCOMING,
                onClick = { onTabSelected(TodoTab.UPCOMING) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterTab(
                text = "Past",
                isSelected = selectedTab == TodoTab.LAST,
                onClick = { onTabSelected(TodoTab.LAST) }
            )
        }
    }
}

@Composable
fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (isSelected) PrimaryColor else TextSecondaryColor,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(3.dp)
                .background(
                    if (isSelected) PrimaryColor else Color.Transparent,
                    RoundedCornerShape(1.5.dp)
                )
        )
    }
}

@Composable
fun DateSelector(
    days: List<DayItem>,
    selectedDayOfWeek: String,
    selectedDayNumber: String,
    onDaySelected: (DayItem) -> Unit
) {
    val listState = rememberLazyListState()

    // Find selected index to center it
    val selectedIndex = days.indexOfFirst {
        it.dayOfWeek == selectedDayOfWeek && it.dayNumber == selectedDayNumber
    }.coerceAtLeast(0)

    // Center the selected date
    LaunchedEffect(selectedDayOfWeek, selectedDayNumber) {
        listState.animateScrollToItem(
            index = maxOf(0, selectedIndex - 3),  // Center the item by subtracting half the visible items
            scrollOffset = 0
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days) { dayItem ->
            val isSelected = dayItem.dayOfWeek == selectedDayOfWeek &&
                    dayItem.dayNumber == selectedDayNumber

            DayItem(
                dayOfWeek = dayItem.dayOfWeek,
                dayNumber = dayItem.dayNumber,
                isSelected = isSelected,
                onClick = { onDaySelected(dayItem) }
            )

            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
fun DayItem(
    dayOfWeek: String,
    dayNumber: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(45.dp)
                .background(
                    color = if (isSelected) PrimaryColor else SecondaryColor,
                    shape = CircleShape
                )
        ) {
            Text(
                text = dayNumber,
                color = if (isSelected) Color.White else TextPrimaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = dayOfWeek,
            color = if (isSelected) TextPrimaryColor else TextSecondaryColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun TodoList(
    todos: List<TodoEntity>,
    isLoading: Boolean,
    selectedTab: TodoTab,
    onTodoClick: (String) -> Unit,
    onTodoCompletionToggle: (String) -> Unit,
    onTodoDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryColor)
        }
    } else if (todos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tasks found",
                color = TextSecondaryColor,
                fontSize = 18.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(todos) { todo ->
                TodoItem(
                    todo = todo,
                    selectedTab = selectedTab,
                    onClick = { onTodoClick(todo.id) },
                    onCompletionToggle = { onTodoCompletionToggle(todo.id) },
                    onDelete = { onTodoDelete(todo.id) }
                )
            }
            // Add space at the end to avoid FAB overlap
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TodoItem(
    todo: TodoEntity,
    selectedTab: TodoTab,
    onClick: () -> Unit,
    onCompletionToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Time formatters
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    val dateYearFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Check if start and end dates are on the same day
    val isSameDay = todo.startAt?.let { startTime ->
        todo.deadline?.let { endTime ->
            val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
            val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

            startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                    startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)
        }
    } ?: true // Default to true if either start or end time is null

    // Set background color based on task status and tab
    val cardColor = when {
        todo.isCompleted -> CompletedColor
        selectedTab == TodoTab.LAST && !todo.isCompleted -> OverdueColor
        else -> PrimaryColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                todo.isCompleted -> CompletedColor.copy(alpha = 0.15f)
                selectedTab == TodoTab.LAST && !todo.isCompleted -> OverdueColor.copy(alpha = 0.15f)
                else -> PrimaryColor.copy(alpha = 0.15f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with border
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (todo.isCompleted) CompletedColor else cardColor,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onCompletionToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (todo.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(CompletedColor, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Task content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (todo.isCompleted) 0.7f else 1f)
            ) {
                Text(
                    text = todo.title,
                    color = TextPrimaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (todo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todo.description,
                        color = TextSecondaryColor,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                // Time section with enhanced styling
                Spacer(modifier = Modifier.height(8.dp))

                // Only display time section if at least one of startAt or deadline is not null
                if (todo.startAt != null || todo.deadline != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Display different formats based on whether it's same day or multiday
                            if (isSameDay) {
                                // Same day format: just show times
                                todo.startAt?.let { startTime ->
                                    Text(
                                        text = timeFormatter.format(Date(startTime)),
                                        color = cardColor,
                                        fontSize = 14.sp
                                    )

                                    if (todo.deadline != null) {
                                        Text(
                                            text = " - ",
                                            color = cardColor.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                todo.deadline?.let { endTime ->
                                    Text(
                                        text = timeFormatter.format(Date(endTime)),
                                        color = cardColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                // Different days format: show dates and times
                                todo.startAt?.let { startTime ->
                                    Text(
                                        text = dateFormatter.format(Date(startTime)) + " " + timeFormatter.format(Date(startTime)),
                                        color = cardColor,
                                        fontSize = 14.sp
                                    )

                                    if (todo.deadline != null) {
                                        Text(
                                            text = " - ",
                                            color = cardColor.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                todo.deadline?.let { endTime ->
                                    Text(
                                        text = dateFormatter.format(Date(endTime)) + " " + timeFormatter.format(Date(endTime)),
                                        color = cardColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = TextSecondaryColor
                )
            }
        }
    }
}