# File-by-File for Beginners

Mục tiêu: người không rành code vẫn mở đúng file để hiểu chức năng.

## Entry

- app/src/main/java/com/phuc/synctask/MainActivity.kt: điều phối route khởi động.
- app/src/main/java/com/phuc/synctask/AuthActivity.kt: activity auth cũ.

## Data model

- model/FirebaseTask.kt: task cá nhân trên cloud.
- model/Task.kt: task local cho Room.
- model/GroupModels.kt: group + group task.
- model/UserProfile.kt: profile + achievements.
- model/AppNotification.kt: thông báo.

## Data layer

- data/AppDatabase.kt: Room database singleton.
- data/TaskDao.kt: query task local.
- data/repository/FirebaseHomeTaskRepository.kt: task cá nhân Firebase.
- data/repository/FirebaseGroupRepository.kt: tạo/join nhóm.
- data/repository/FirebaseGroupTaskRepository.kt: task nhóm.
- data/repository/FirebaseNotificationRepository.kt: thông báo.

## ViewModel layer

- viewmodel/AuthViewModel.kt: auth logic.
- viewmodel/HomeViewModel.kt: task cá nhân.
- viewmodel/GroupViewModel.kt: group list.
- viewmodel/GroupTaskViewModel.kt: group task.
- viewmodel/NotificationViewModel.kt: notification logic.
- viewmodel/DashboardViewModel.kt: dashboard state.
- viewmodel/DashboardAnalyticsUseCase.kt: hàm tính dashboard.
- viewmodel/ThemeViewModel.kt: dark/light.
- viewmodel/SoundSettingsViewModel.kt: sound settings.
- viewmodel/OnboardingViewModel.kt: first-time tutorial flag.

## UI layer

- ui/main/MainScreen.kt: shell chính + nav + top/bottom bar.
- ui/main/AddTaskBottomSheet.kt: form thêm task.
- ui/main/NotificationBottomSheet.kt: danh sách thông báo.
- ui/personal/\*: ma trận và chi tiết task cá nhân.
- ui/group/\*: danh sách nhóm và task nhóm.
- ui/dashboard/DashboardScreen.kt: thống kê.
- ui/achievement/AchievementScreen.kt: huy hiệu.
- ui/auth/\*: login/register.
- ui/onboarding/\*: welcome/tutorial.
- ui/common/\*: dialog/loading/empty state.

## Helpers

- utils/AchievementManager.kt: rule unlock.
- utils/AppSoundEffects.kt: sound events.
- utils/WorkloadChecker.kt: cảnh báo quá tải.
- utils/JsonSyncHelper.kt: import/export JSON.
- util/NotificationHelper.kt: hỗ trợ push.
- service/SyncTaskMessagingService.kt: nhận FCM.
