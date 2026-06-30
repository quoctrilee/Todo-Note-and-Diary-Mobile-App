package com.example.todonotediary.presentation.navigation

import kotlinx.serialization.Serializable

// ── Top-level routes ──────────────────────────────────────────────────────────

@Serializable
object SplashRoute

@Serializable
object AuthRoute

/** Truyền email nullable khi đến từ Google Sign-In */
@Serializable
data class RegisterRoute(val email: String? = null)

@Serializable
object MainScreenRoute

@Serializable
object AddTodoRoute

@Serializable
object AddNoteRoute

@Serializable
object AddDiaryRoute

@Serializable
object UserRoute

// ── Inner (Bottom Nav) routes ─────────────────────────────────────────────────

@Serializable
object TodoRoute

@Serializable
object NoteRoute

@Serializable
object DiaryRoute