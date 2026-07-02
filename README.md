# 📝 Todo Note Diary

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-1.5+-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger_Hilt-DI-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/training/dependency-injection/hilt-android)
[![Firebase](https://img.shields.io/badge/Firebase-Sync-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)

**Todo Note Diary** là ứng dụng Android hiện đại giúp người dùng quản lý công việc hàng ngày (Todo), ghi chú nhanh (Note) và viết nhật ký cá nhân (Diary) trên cùng một giao diện trực quan. Ứng dụng được thiết kế theo mô hình **Offline-first**, tích hợp AI để tối ưu hóa trải nghiệm người dùng.

---

## 🚀 Điểm Nhấn Kỹ Thuật 

Dự án được xây dựng dựa trên các tiêu chuẩn phát triển Android hiện đại, tập trung vào tính đóng gói, dễ bảo trì và khả năng mở rộng:

*   **Kiến trúc sạch (Clean Architecture)**: Phân tách rõ ràng thành 3 lớp độc lập:
    *   `presentation`: UI xây dựng 100% bằng **Jetpack Compose** theo mô hình **MVVM (Model-View-ViewModel)**.
    *   `domain`: Chứa logic nghiệp vụ cốt lõi (Business Logic), Use Cases độc lập với UI và Data Source.
    *   `data`: Triển khai Repository, xử lý dữ liệu local (**Room DB**) và remote (**Firebase Firestore** / **Retrofit**).
*   **Offline-first Architecture**: Dữ liệu được lưu trữ trực tiếp dưới Local (Room DB) đóng vai trò là *Single Source of Truth*, giúp app chạy mượt mà ngay cả khi không có mạng, và tự động đồng bộ lên Firebase khi trực tuyến qua **WorkManager**.
*   **Dependency Injection**: Sử dụng **Dagger-Hilt** quản lý vòng đời và tiêm phụ thuộc (Dependency Injection) toàn cục.
*   **Reactive Programming**: Sử dụng **Kotlin Coroutines** và **Flow** để xử lý các tác vụ bất đồng bộ, cập nhật UI thời gian thực và quản lý tài nguyên hiệu quả.
*   **AI Integration**: Tích hợp **Groq LLM** thông qua **Retrofit** để phân tích, tóm tắt và hỗ trợ viết nhật ký/ghi chú thông minh.

---

## 🛠️ Tech Stack & Thư Viện Sử Dụng

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Navigation, Material 3)
*   **Local Database**: Room DB
*   **Cloud Backend**: Firebase Authentication (Google Sign-In), Cloud Firestore
*   **Networking**: Retrofit, OkHttp, Gson
*   **Dependency Injection**: Dagger Hilt
*   **Background Tasks**: WorkManager
*   **AI Engine**: Groq API (LLM)

---

## 📁 Cấu Trúc Thư Mục Chính

```text
app/src/main/java/com/example/todonotediary/
├── data/          # Cấu hình Room Database, Firebase API, Repositories Implementation
├── di/            # Cấu hình Dagger Hilt Dependency Injection Modules
├── domain/        # Models, Repository Interfaces, và Business Use Cases
├── presentation/  # Jetpack Compose UI (Screens, Components) & ViewModels
│   ├── ai/        # Giao diện/Xử lý hỗ trợ AI với Groq LLM
│   ├── auth/      # Đăng nhập Google Sign-In & Đăng ký
│   ├── diary/     # Quản lý nhật ký cá nhân
│   ├── note/      # Quản lý ghi chú nhanh
│   ├── todo/      # Quản lý công việc hàng ngày
│   └── navigation/# Điều hướng trong ứng dụng (Compose Navigation)
└── worker/        # Tác vụ đồng bộ ngầm chạy với WorkManager
```

---

## ⚙️ Cấu Hình Nhanh Để Chạy Dự Án

### 1. Yêu cầu hệ thống
*   Android Studio mới nhất.
*   JDK 17.
*   Thiết bị/Emulator chạy Android SDK 24 (`minSdk`) trở lên.

### 2. Cấu hình Firebase & API Key
1. Tạo một dự án trên [Firebase Console](https://console.firebase.google.com/).
2. Thêm ứng dụng Android với package name: `com.example.todonotediary`.
3. Tải file `google-services.json` đặt vào thư mục `/app`.
4. Bật **Authentication** (Google Sign-In) và **Cloud Firestore** trên Firebase Console.
5. Thêm API Key của Groq vào file `local.properties` tại thư mục gốc dự án:
   ```properties
   groq.api.key=YOUR_GROQ_API_KEY
   ```

### 3. Build & Run
Mở dự án bằng Android Studio, đợi đồng bộ Gradle hoàn chỉnh và nhấn **Run** trên thiết bị hoặc chạy lệnh:
```bash
./gradlew assembleDebug
```
