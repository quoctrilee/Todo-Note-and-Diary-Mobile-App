package com.example.todonotediary.presentation.note

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddNoteScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Xử lý sự kiện UI
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is AddNoteUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AddNoteUiEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    // Xử lý thông báo lỗi
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Xử lý thành công
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Ghi chú đã được lưu", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Hiển thị dialog thêm danh mục mới
    if (state.showCategoryDialog) {
        AddCategoryDialog(
            categoryName = state.newCategoryName,
            onCategoryNameChanged = viewModel::onNewCategoryNameChanged,
            onDismiss = viewModel::onHideAddCategoryDialog,
            onConfirm = viewModel::onAddNewCategory
        )
    }

    Scaffold(
        topBar = {
            AddNoteTopBar(
                onBackClick = onNavigateBack,
                onSaveClick = {
                    // Sử dụng hàm saveNote không cần truyền userId
                    viewModel.saveNote()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = state.backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tiêu đề
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                placeholder = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Danh mục - Chỉ hiển thị nếu có danh mục
            if (state.categories.isNotEmpty()) {
                CategorySelector(
                    selectedCategory = state.category,
                    categories = state.categories,
                    onCategorySelected = viewModel::onCategoryChanged,
                    onAddNewCategory = viewModel::onShowAddCategoryDialog
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Chọn màu nền
            Text(
                text = "Màu nền",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ColorSelector(
                colors = viewModel.availableColors,
                selectedColor = state.backgroundColor,
                onColorSelected = viewModel::onBackgroundColorChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nội dung
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChanged,
                placeholder = { Text("Nội dung ghi chú...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )

            // Hiển thị loading
            AnimatedVisibility(visible = state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("Thêm ghi chú mới") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
        },
        actions = {
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Default.Check, contentDescription = "Lưu")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    onAddNewCategory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Danh mục",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                AddCategoryChip(
                    title = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }

            // Nút thêm danh mục mới
            OutlinedButton(
                onClick = onAddNewCategory,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("+ Thêm mới", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AddCategoryChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier
            .height(32.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
fun ColorSelector(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { color ->
            ColorItem(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    categoryName: String,
    onCategoryNameChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thêm danh mục mới",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChanged,
                    label = { Text("Tên danh mục") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Thêm")
                }
            }
        }
    }
}