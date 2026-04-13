# Tổng quan dự án SyncTask

## Mục tiêu hệ thống

SyncTask là ứng dụng Android hỗ trợ quản lý công việc cá nhân và công việc nhóm theo thời gian thực. Hệ thống giải quyết bài toán lập kế hoạch, theo dõi tiến độ, phối hợp giao việc và duy trì kỷ luật hoàn thành công việc.

## Ứng dụng làm gì

Ứng dụng cung cấp các năng lực cốt lõi sau:

1. Xác thực người dùng: đăng ký, đăng nhập, xác thực email, đăng xuất.
2. Quản lý công việc cá nhân theo mức độ khẩn cấp và quan trọng.
3. Quản lý công việc nhóm: tạo nhóm, tham gia nhóm, phân công, theo dõi tiến độ.
4. Cơ chế thông báo đa kênh: thông báo nội bộ và thông báo đẩy.
5. Dashboard theo dõi hiệu suất và hệ thống thành tựu.

## Ứng dụng làm bằng cách nào

## Kiến trúc tổng quát

Hệ thống áp dụng kiến trúc **MVVM kết hợp Repository**:

- **UI Layer (Jetpack Compose):** nhận thao tác và hiển thị trạng thái.
- **ViewModel Layer:** xử lý nghiệp vụ và quản lý luồng trạng thái.
- **Repository Layer:** truy cập Firebase theo từng miền dữ liệu.
- **Storage hỗ trợ:** DataStore cho cài đặt, Room cho lưu trữ cục bộ.

## Công nghệ chính

| Nhóm công nghệ | Công nghệ sử dụng | Vai trò |
|---|---|---|
| Nền tảng | Kotlin, Android SDK | Xây dựng ứng dụng Android gốc |
| UI | Jetpack Compose, Material 3 | Phát triển giao diện hiện đại |
| Điều hướng | Navigation Compose | Quản lý luồng màn hình |
| Xác thực | Firebase Authentication | Quản lý tài khoản và phiên đăng nhập |
| Dữ liệu thời gian thực | Firebase Realtime Database | Lưu users, tasks, groups, notifications |
| Thông báo đẩy | Firebase Cloud Messaging | Gửi thông báo đến người được phân công |
| Lưu cài đặt | DataStore Preferences | Lưu onboarding, giao diện, âm thanh |
| CSDL cục bộ | Room | Lưu dữ liệu local phục vụ hỗ trợ |

## Phạm vi dữ liệu nghiệp vụ

- `/users/{uid}`: hồ sơ người dùng, thành tựu, token FCM.
- `/tasks/{uid}/{taskId}`: công việc cá nhân.
- `/groups/{groupId}`: thông tin nhóm và thành viên.
- `/groupTasks/{groupId}/{taskId}`: công việc trong nhóm.
- `/notifications/{uid}/{notificationId}`: thông báo nội bộ theo người dùng.

## Kịch bản sử dụng tổng quan

1. Người dùng tạo tài khoản và xác thực email.
2. Hệ thống kiểm tra trạng thái onboarding để điều hướng phù hợp.
3. Người dùng quản lý task cá nhân, theo dõi deadline và cập nhật trạng thái.
4. Người dùng tạo nhóm hoặc tham gia nhóm bằng mã mời.
5. Người dùng phân công task nhóm, nhận phản hồi qua thông báo.
6. Hệ thống tổng hợp dữ liệu thành dashboard và mốc thành tựu.

## Lợi ích thực tiễn

- Giảm nguy cơ quên hoặc trễ công việc nhờ kiểm soát deadline.
- Tăng hiệu quả phối hợp nhóm nhờ quy trình phân công rõ ràng.
- Cải thiện động lực làm việc nhờ dashboard và gamification.
- Đảm bảo dữ liệu đồng bộ thời gian thực trên môi trường cloud.
