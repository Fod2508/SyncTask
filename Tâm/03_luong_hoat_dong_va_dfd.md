# Luồng hoạt động và DFD hệ thống SyncTask

## Mục tiêu tài liệu

Tài liệu mô tả chi tiết luồng xử lý nghiệp vụ dưới góc nhìn dữ liệu, phục vụ báo cáo đồ án và pha phân tích hệ thống.

## Luồng hoạt động chính

## Luồng đăng nhập và khởi tạo phiên làm việc

1. Người dùng nhập thông tin đăng nhập.
2. `AuthViewModel` xác thực qua Firebase Authentication.
3. `MainActivity` kiểm tra:
   - Phiên đăng nhập có hợp lệ hay không.
   - Email đã được xác thực hay chưa.
   - Người dùng có thuộc lần sử dụng đầu tiên hay không.
4. Điều hướng đến `login`, `welcome` hoặc `main`.

## Luồng tạo và quản lý task cá nhân

1. Người dùng tạo task tại `AddTaskBottomSheet`.
2. `HomeViewModel` gọi `FirebaseHomeTaskRepository` để ghi dữ liệu vào `/tasks/{uid}`.
3. Listener realtime đồng bộ lại danh sách task trong `HomeUiState`.
4. Khi người dùng hoàn thành task:
   - Hệ thống cập nhật `isCompleted`.
   - Đánh giá đúng hạn hoặc trễ hạn.
   - Ghi thông báo nội bộ vào `/notifications/{uid}`.
   - Kiểm tra điều kiện mở khóa thành tựu cá nhân.

## Luồng quản lý nhóm và task nhóm

1. Tạo nhóm: sinh record mới trong `/groups`, gán `ownerId`, tạo `inviteCode`.
2. Tham gia nhóm: tra cứu `inviteCode`, dùng transaction thêm `uid` vào `members`.
3. Tạo task nhóm: ghi vào `/groupTasks/{groupId}/{taskId}`.
4. Phân công task:
   - Cập nhật `assignedToId`.
   - Tạo thông báo nội bộ cho người nhận.
   - Nếu có FCM token thì gửi push notification.
5. Hoàn thành task nhóm:
   - Đảo trạng thái `isCompleted`.
   - Cập nhật `groupTaskCount` tại hồ sơ người dùng.
   - Kiểm tra thành tựu nhóm và phát dialog thành tựu.

## Tương tác giữa cụm Cá nhân và cụm Nhóm

1. Cùng chia sẻ hồ sơ người dùng trong node `/users/{uid}`.
2. Cùng ghi nhận thành tựu tại `unlockedAchievements`.
3. Cùng đẩy dữ liệu sang module Dashboard để tổng hợp hiệu suất.
4. Cùng dùng module Notification để phản hồi sự kiện nghiệp vụ.

## DFD mức ngữ cảnh (Context Diagram)

```mermaid
flowchart LR
    ND[Người dùng] -->|Thông tin đăng nhập, thao tác task| HT[Hệ thống SyncTask]
    HT -->|Trạng thái, danh sách task, dashboard| ND

    HT -->|Xác thực tài khoản| FA[Firebase Authentication]
    FA -->|Kết quả xác thực| HT

    HT -->|Đọc/Ghi dữ liệu| FR[Firebase Realtime Database]
    FR -->|Dữ liệu users/tasks/groups/groupTasks/notifications| HT

    HT -->|Gửi thông báo đẩy| FCM[Firebase Cloud Messaging]
    FCM -->|Push notification| OS[Android Notification System]
    OS -->|Thông báo hiển thị| ND
```

## DFD mức 1

```mermaid
flowchart TD
    E1[Người dùng]
    E2[Firebase Auth]
    E3[Firebase Realtime Database]
    E4[FCM]

    P1[P1 Xác thực và quản lý phiên]
    P2[P2 Quản lý task cá nhân]
    P3[P3 Quản lý nhóm]
    P4[P4 Quản lý task nhóm]
    P5[P5 Thành tựu và thông báo]
    P6[P6 Dashboard và thống kê]

    D1[(D1 users)]
    D2[(D2 tasks)]
    D3[(D3 groups)]
    D4[(D4 groupTasks)]
    D5[(D5 notifications)]

    E1 --> P1
    P1 <--> E2
    P1 --> D1

    E1 --> P2
    P2 <--> D2
    P2 --> P5

    E1 --> P3
    P3 <--> D3

    E1 --> P4
    P4 <--> D4
    P4 --> P5

    P5 <--> D1
    P5 <--> D5
    P5 --> E4

    D2 --> P6
    D4 --> P6
    D1 --> P6
    P6 --> E1

    E3 --- D1
    E3 --- D2
    E3 --- D3
    E3 --- D4
    E3 --- D5
```

## DFD mức 2 cho P4 (Quản lý task nhóm)

```mermaid
flowchart TD
    U[Người dùng trong nhóm] --> P41[P4.1 Tạo task nhóm]
    U --> P42[P4.2 Phân công/nhận task]
    U --> P43[P4.3 Cập nhật trạng thái task]

    P41 --> D4[(groupTasks)]
    P42 --> D4
    P43 --> D4

    P42 --> P51[P5.1 Tạo thông báo nội bộ]
    P42 --> P52[P5.2 Gửi push FCM]
    P43 --> P53[P5.3 Kiểm tra thành tựu nhóm]

    P51 --> D5[(notifications)]
    P53 --> D1[(users)]
```

## Ma trận đối tượng DFD

| Thành phần | Vai trò |
|---|---|
| External Entity | Người dùng, Firebase Auth, FCM, Android OS |
| Process | Xác thực, quản lý task cá nhân, quản lý nhóm, quản lý task nhóm, thông báo, dashboard |
| Data Store | users, tasks, groups, groupTasks, notifications |
| Data Flow | Đăng nhập, tạo task, phân công, cập nhật trạng thái, thông báo, thống kê |

## Nhận xét kỹ thuật

- Dữ liệu dùng listener realtime nên UI cập nhật gần như ngay lập tức.
- Các nghiệp vụ có transaction (join group, toggle status, count delta) giúp đảm bảo nhất quán.
- Luồng thông báo được tách thành thông báo nội bộ và thông báo đẩy để tăng khả năng mở rộng.
