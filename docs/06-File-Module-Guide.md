# File and Module Guide

## Đọc file nào trước?

1. MainActivity.kt: hiểu cửa vào app.
2. MainScreen.kt: hiểu điều hướng và khung UI chính.
3. HomeViewModel.kt: hiểu task cá nhân.
4. GroupTaskViewModel.kt: hiểu task nhóm.
5. DashboardViewModel.kt: hiểu thống kê.

## Bản đồ module

- model: định nghĩa cấu trúc dữ liệu.
- data/repository: gọi Firebase/Room.
- viewmodel: nghiệp vụ và state.
- ui: hiển thị và tương tác.
- utils/util: helper (achievement, sound, notification, sync).
- service: nhận push FCM.

## Cách nghĩ dễ hiểu

- UI = nơi bấm.
- ViewModel = nơi quyết định logic.
- Repository = nơi đi lấy/lưu dữ liệu.
- Model = khuôn dữ liệu.
- Utils = phụ trợ.
