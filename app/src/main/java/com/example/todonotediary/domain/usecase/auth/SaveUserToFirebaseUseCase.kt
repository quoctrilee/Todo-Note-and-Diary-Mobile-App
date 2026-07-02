package com.example.todonotediary.domain.usecase.auth

import com.example.todonotediary.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class SaveUserToFirebaseUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // Thêm từ khóa operator fun invoke để gọi UseCase ngắn gọn như một hàm
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {

        // 🌟 RULE 1: Kiểm tra định dạng Email chuẩn (dùng Regex của Android)
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        if (!email.matches(emailRegex)) {
            return Result.failure(Exception("Định dạng Email không hợp lệ"))
        }

        // 🌟 RULE 2: Kiểm tra độ dài mật khẩu
        if (password.length < 6) {
            return Result.failure(Exception("Mật khẩu bắt buộc phải từ 6 ký tự trở lên"))
        }

        // 🌟 RULE 3: Kiểm tra tên hiển thị không quá ngắn hoặc quá dài
        if (displayName.trim().length < 2) {
            return Result.failure(Exception("Tên hiển thị phải có ít nhất 2 ký tự"))
        }

        // ➔ Vượt qua tất cả vòng kiểm duyệt mới cho phép gọi xuống Repo để ghi vào Firebase
        return authRepository.saveUserToFirebase(email, password, displayName)
    }
}