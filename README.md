# Todo Note Diary

Ứng dụng Android cho phép quản lý việc cần làm, ghi chú và nhật ký cá nhân trên cùng một giao diện. Dự án được xây dựng bằng Kotlin, Jetpack Compose, Hilt, Room, Firebase và tích hợp Groq API cho các tính năng AI/liên quan đến xử lý nội dung.

## Tính năng chính

- Quản lý todo, note và diary.
- Đăng nhập và đồng bộ dữ liệu với Firebase.
- Lưu dữ liệu cục bộ bằng Room.
- Gọi Groq API qua Retrofit.
- Hỗ trợ giọng nói, nhắc lại và đồng bộ nền.

## Yêu cầu môi trường

- Android Studio mới nhất.
- JDK 17.
- Android SDK với `minSdk = 24` và `targetSdk = 35`.
- Một dự án Firebase đã được cấu hình.

## Cấu hình trước khi chạy

1. Tạo project trên Firebase Console.
2. Thêm ứng dụng Android với package name `com.example.todonotediary`.
3. Tải `google-services.json` và đặt vào thư mục `app/`.
4. Bật các dịch vụ cần dùng trong Firebase, tối thiểu là Authentication và Firestore.
5. Lấy `web client id` từ Google Sign-In / Firebase Authentication và cấu hình vào màn hình đăng nhập nếu dự án yêu cầu.
6. Thêm Groq API key vào file `local.properties` ở thư mục gốc của dự án:

```properties
groq.api.key=YOUR_GROQ_API_KEY
```

## Chạy dự án

```bash
./gradlew assembleDebug
```

Hoặc mở dự án bằng Android Studio và chạy trực tiếp trên thiết bị/emulator.

## Ghi chú

- Nếu thiếu `google-services.json`, ứng dụng sẽ không khởi tạo được phần Firebase.
- Nếu thiếu `groq.api.key`, các tính năng gọi Groq API sẽ không hoạt động.
- Dự án sử dụng Compose, Hilt, Room và WorkManager, nên lần build đầu tiên có thể mất thêm thời gian.
