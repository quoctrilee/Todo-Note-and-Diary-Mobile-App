package com.example.todonotediary.presentation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Model cho mỗi mục trong Bottom Navigation Bar.
 * [route] là một Serializable route object (type-safe navigation).
 */
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: Any
)