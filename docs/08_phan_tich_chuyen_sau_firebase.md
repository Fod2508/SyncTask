# Phân tích chuyên sâu Firebase theo mã nguồn

## Mục tiêu tài liệu

Tài liệu này đi sâu vào các tệp và lớp liên quan trực tiếp đến Firebase trong dự án SyncTask, bao gồm cấu hình, luồng xác thực, luồng dữ liệu Realtime Database, FCM, và các rủi ro bảo mật ở cấp triển khai.

## Danh mục tệp Firebase quan trọng

| Nhóm | Tệp | Vai trò |
|---|---|---|
| Cấu hình dự án | `app/google-services.json` | Khai báo project Firebase, appId, package, API key |
| Cấu hình Gradle | `app/build.gradle.kts` | Bật plugin Google Services, khai báo Firebase BOM và thư viện |
| Xác thực | `viewmodel/AuthViewModel.kt` | Đăng nhập, đăng ký, reset mật khẩu, đồng bộ profile user |
| Task cá nhân | `data/repository/FirebaseHomeTaskRepository.kt` | CRUD task cá nhân trên Realtime DB |
| Nhóm | `data/repository/FirebaseGroupRepository.kt` | Tạo nhóm, join nhóm bằng invite code + transaction |
| Task nhóm | `data/repository/FirebaseGroupTaskRepository.kt` | CRUD task nhóm, toggle transaction, cập nhật groupTaskCount |
| Thông báo in-app | `data/repository/FirebaseNotificationRepository.kt` | Lưu/đọc thông báo nội bộ theo user |
| Push và token | `service/SyncTaskMessagingService.kt` | Nhận push FCM, cập nhật token về DB |
| Push HTTP v1 | `util/NotificationHelper.kt` | Gửi thông báo đẩy chủ động (mức demo) |
| Dashboard | `viewmodel/DashboardViewModel.kt` | Tổng hợp dữ liệu đa node Firebase để phân tích |

## Phân tích tệp cấu hình `google-services.json`

Tệp chứa các thông tin định danh Firebase app:

- `project_id`
- `firebase_url`
- `mobilesdk_app_id`
- `package_name`
- `api_key.current_key`

Ý nghĩa kỹ thuật:

1. Đây là tệp cấu hình client chuẩn của Firebase Android.
2. `api_key` trong tệp này không phải bí mật tuyệt đối như server key, nhưng vẫn cần quản trị quota và rules chặt chẽ.
3. Tệp cần khớp `package_name` và certificate hash để tránh sai môi trường build.

## Firebase Authentication trong code

## Luồng đăng nhập

Điểm vào: `AuthViewModel.loginWithEmail(email, pass)`

Các bước:

1. Kiểm tra rỗng và format email (`Patterns.EMAIL_ADDRESS`).
2. Gọi `FirebaseAuth.signInWithEmailAndPassword`.
3. Nếu tài khoản password chưa verify email:
   - Gửi lại email verify.
   - `signOut()`.
   - Trả thông báo lỗi có hướng dẫn.
4. Nếu hợp lệ: phát `AuthState.Success`.

## Luồng đăng ký

Điểm vào: `AuthViewModel.registerWithEmail(...)`

Các bước:

1. Validate tên, email, mật khẩu và confirm.
2. Tạo user bằng Firebase Auth.
3. Cập nhật `displayName` vào Auth profile.
4. Ghi hồ sơ vào Realtime DB tại `/users/{uid}`.
5. Gửi email xác thực, sau đó sign out.

Nhận xét:

- Luồng này đúng hướng production ở phần verify email.
- Cần bổ sung logging mã lỗi chuẩn để debug nhanh hơn trên môi trường thật.

## Realtime Database theo từng node

## Node `/users`

Nguồn ghi:

- `AuthViewModel.saveUserToDatabase` (uid, email, displayName)
- `SyncTaskMessagingService.onNewToken` (fcmToken)
- `HomeViewModel`/`GroupTaskViewModel` (unlockedAchievements)
- `FirebaseGroupTaskRepository.applyGroupTaskCountDelta` (groupTaskCount)

Rủi ro nếu rules yếu:

- Có thể bị ghi đè hồ sơ user khác nếu không chặn theo `auth.uid == $uid`.

## Node `/tasks/{uid}`

Repository: `FirebaseHomeTaskRepository`

Đặc điểm code:

1. `observeTasks` dùng `ValueEventListener` realtime.
2. `addTask` sinh id bằng `push().key`.
3. `updateTaskCompleted` chỉ cập nhật field con `isCompleted`.
4. Dữ liệu được sort theo `timestamp` trước khi đưa lên UI.

## Node `/groups` và `/groupTasks`

Repositories: `FirebaseGroupRepository`, `FirebaseGroupTaskRepository`

Cơ chế nổi bật:

1. Join group theo `inviteCode`.
2. `members` cập nhật bằng `runTransaction` để tránh race condition.
3. Toggle trạng thái task nhóm cũng dùng transaction, trả `delta`.
4. `groupTaskCount` cập nhật transaction + `coerceAtLeast(0)` để chống âm.

## Node `/notifications/{uid}`

Repository: `FirebaseNotificationRepository`

Đặc điểm:

1. Tạo notification mới bằng `push`.
2. Mặc định `isRead = false`.
3. `NotificationViewModel` stream realtime và tính badge chưa đọc.

## FCM trong dự án

## Nhận thông báo đẩy

Tệp: `SyncTaskMessagingService.kt`

Luồng:

1. `onMessageReceived` nhận payload notification.
2. Tạo Android notification channel (`group_task_channel`) nếu API >= 26.
3. Hiển thị notification local bằng `NotificationCompat`.

## Đồng bộ token thiết bị

`onNewToken(token)` ghi token vào `/users/{uid}/fcmToken` nếu đang có phiên đăng nhập.

## Gửi thông báo đẩy chủ động

Tệp: `NotificationHelper.kt`

Trạng thái hiện tại:

1. Dùng HTTP v1 endpoint dạng `https://fcm.googleapis.com/v1/projects/...`.
2. Cần OAuth2 token và project id điền thủ công.
3. Nếu chưa cấu hình thì hàm tự bỏ qua gửi (`skip push send on client`).

Đánh giá:

- Phù hợp demo/đồ án.
- Không phù hợp production vì token OAuth không nên nằm phía client.

## Dashboard và tải dữ liệu Firebase quy mô lớn

Tệp: `DashboardViewModel.kt`, `DashboardAnalyticsUseCase.kt`

Cách hoạt động:

1. Đồng thời lắng nghe:
   - `/tasks/{uid}`
   - `/groups`
   - nhiều node `/groupTasks/{groupId}`
2. Dùng `combine` để hợp nhất dữ liệu và tái tính toán dashboard.
3. Huỷ listener động khi người dùng rời nhóm hoặc màn hình bị destroy.

Lưu ý hiệu năng:

- Khi số nhóm lớn, số listener groupTasks tăng theo số group.
- Cần cân nhắc phân trang hoặc tổng hợp server-side nếu mở rộng quy mô.

## Đề xuất Firebase Rules tối thiểu

```xml
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "tasks": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "notifications": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null"
      }
    }
  }
}
```

Ghi chú:

- Đoạn trên là mẫu tối thiểu để tham khảo trong báo cáo.
- Với `groups/groupTasks` cần rule chi tiết theo thành viên nhóm (`members`).

## Kết luận chuyên sâu

1. Phần tích hợp Firebase trong dự án đã đầy đủ các trụ cột Auth + Realtime DB + FCM.
2. Code đã có transaction ở các điểm xung đột quan trọng, đây là điểm mạnh lớn.
3. Điểm cần nâng cấp chính là bảo mật luồng push chủ động và chuẩn hóa rules theo membership.
