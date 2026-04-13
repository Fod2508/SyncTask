# Project Overview

## App này làm gì?

SyncTask là app quản lý công việc cá nhân và nhóm. Mục tiêu là giúp người dùng:

- Biết việc nào cần làm trước.
- Không quên deadline.
- Theo dõi tiến độ bản thân và nhóm.
- Có động lực làm việc nhờ achievement và phản hồi trực quan.

## Hoạt động thế nào?

App hoạt động theo chuỗi:

1. Người dùng thao tác trên UI.
2. UI gọi ViewModel.
3. ViewModel gọi Repository.
4. Repository lưu/đọc dữ liệu từ Firebase hoặc Room.
5. Dữ liệu trả về theo realtime listener.
6. UI tự cập nhật lại.

## Chức năng chính

- Auth: đăng ký, đăng nhập, verify email, quên mật khẩu.
- Task cá nhân theo ma trận Eisenhower.
- Task nhóm: tạo nhóm, join nhóm, giao việc, hoàn thành việc.
- Notification trong app + push FCM.
- Dashboard thống kê tiến độ.
- Achievement và âm thanh.
- Theme sáng/tối, onboarding.

## Công nghệ

- Kotlin + Jetpack Compose
- Firebase Auth + Realtime Database + Cloud Messaging
- Room Database
- DataStore
- Coroutines + Flow
