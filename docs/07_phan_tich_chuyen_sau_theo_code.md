# Phân tích chuyên sâu theo code

## Mục tiêu tài liệu

Tài liệu tập trung vào mức **triển khai mã nguồn**, mô tả trực tiếp cách các lớp và hàm phối hợp để tạo ra hành vi của hệ thống.

## Bản đồ lớp cốt lõi

| Tầng | Lớp/Đối tượng | Vai trò |
|---|---|---|
| Activity | `MainActivity` | Khởi tạo app, quyết định route ban đầu |
| UI | `MainScreen`, `PersonalTaskScreen`, `GroupTaskScreen` | Render và phát sinh sự kiện |
| ViewModel | `AuthViewModel`, `HomeViewModel`, `GroupViewModel`, `GroupTaskViewModel`, `NotificationViewModel` | Điều phối nghiệp vụ, quản lý state |
| Repository | `FirebaseHomeTaskRepository`, `FirebaseGroupRepository`, `FirebaseGroupTaskRepository`, `FirebaseNotificationRepository` | Truy cập dữ liệu Firebase |
| Service | `SyncTaskMessagingService` | Nhận push FCM, cập nhật token |
| Utility | `AchievementManager`, `NotificationHelper`, `AppSoundPlayer` | Logic thuần và tiện ích hạ tầng |

## Phân tích các hàm then chốt

## `AuthViewModel.loginWithEmail`

- Chuỗi xử lý:
  1. Trim input.
  2. Validate email/password.
  3. Gọi `signInWithEmailAndPassword`.
  4. Kiểm tra trường hợp provider password nhưng email chưa verify.
  5. Nếu chưa verify: gửi lại email verify, sign out, trả state lỗi có hướng dẫn.
- Giá trị kỹ thuật:
  - Tránh cho user chưa xác thực đi sâu vào app.
  - Thống nhất error message ở tầng ViewModel.

## `HomeViewModel.toggleTaskStatus`

- Chuỗi xử lý:
  1. Tạo `newStatus`.
  2. Gọi repository cập nhật `isCompleted`.
  3. Nếu vừa chuyển sang hoàn thành:
     - So sánh `dueDate` để xác định đúng hạn/trễ hạn.
     - Phát âm thanh theo ngữ cảnh.
     - Ghi thông báo nội bộ.
     - Tính `completedCount` và check thành tựu.
  4. Nếu lỗi: đẩy `HomeUiState.Error`.
- Lưu ý:
  - Hàm đảm nhiệm nhiều trách nhiệm, có thể tách nhỏ thành các hàm private để rõ hơn.

## `GroupTaskViewModel.toggleTaskStatus`

- Chuỗi xử lý:
  1. Gọi `repository.toggleTaskStatus` (transaction ở DB).
  2. Nhận `delta` để biết chiều thay đổi.
  3. Nếu `delta > 0` thì xử lý thông báo và thành tựu.
  4. Gọi `updateGroupTaskCount(uid, delta)` để đồng bộ hồ sơ user.
- Điểm mạnh:
  - Sử dụng `delta` giúp tránh đọc lại toàn bộ list để tính toán.
  - Đồng bộ count theo transaction giảm sai lệch khi nhiều người thao tác đồng thời.

## `FirebaseGroupRepository.joinGroup`

- Chi tiết transaction:
  - Đọc `members` hiện tại.
  - Nếu đã chứa `uid` thì `Transaction.abort`.
  - Nếu chưa có thì add và `Transaction.success`.
- Giá trị:
  - Chống race condition khi nhiều thiết bị cùng join.
  - Trả trạng thái rõ ràng: `Joined`, `AlreadyMember`, `NotFound`.

## `NotificationViewModel.listenToNotifications`

- Cơ chế:
  - Theo dõi realtime danh sách notification.
  - Tự tính `unreadCount` bằng stream map.
  - Chỉ phát âm thanh khi có thông báo mới phát sinh sau thời điểm app khởi động.
- Giá trị:
  - Tránh gây ồn do thông báo lịch sử.
  - Đảm bảo badge luôn đúng với dữ liệu thực.

## Cấu trúc luồng lỗi trong code

| Lớp | Cách xử lý lỗi | Hạn chế hiện tại |
|---|---|---|
| ViewModel | Map exception sang message thân thiện | Chưa chuẩn hóa error code |
| Repository | Trả `Result<T>` hoặc callback `onError` | Chưa đồng nhất một kiểu cho toàn bộ repo |
| UI | Hiển thị theo state `Error` | Một số màn hình còn bỏ qua lỗi (`onError = {}`) |

## Vấn đề kỹ thuật có thể cải tiến

1. **Giảm độ phụ thuộc trực tiếp singleton**
   - Hiện nhiều nơi gọi `FirebaseAuth.getInstance()` ngay trong lớp.
   - Đề xuất inject qua constructor để unit test tốt hơn.
2. **Tách logic lớn trong ViewModel**
   - Ví dụ `toggleTaskStatus` chứa cả update, notify, achievement.
   - Nên tách thành các hàm private theo mục đích.
3. **Chuẩn hóa domain result**
   - Dùng sealed result chung (`Success`, `Failure(code, message)`) thay vì trộn callback và `Result<T>`.
4. **Tăng độ an toàn push**
   - Chuyển gửi push từ client sang backend.
   - Client chỉ gọi endpoint nghiệp vụ, không giữ OAuth token.

## Đề xuất test chuyên sâu

| Phạm vi test | Ca kiểm thử quan trọng |
|---|---|
| Auth | Email sai định dạng, mật khẩu trống, tài khoản chưa verify |
| Personal Task | Toggle đúng hạn/trễ hạn, unlock thành tựu đúng mốc |
| Group Task | Transaction toggle trả delta đúng, update `groupTaskCount` không âm |
| Notification | unreadCount chính xác, markAllAsRead hoạt động đủ |
| Achievement | Mốc cá nhân + mốc nhóm không unlock trùng |
