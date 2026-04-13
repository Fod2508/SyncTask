# 03 - Dữ liệu, logic và backend của SyncTask

## File này nói về điều gì

Phần này là nơi app “thực sự sống”. Khi người dùng bấm nút, thêm task, hoàn thành việc, nhận huy hiệu hay xem thông báo, chính các file ở đây sẽ làm việc.

## Mô hình dữ liệu

### [app/src/main/java/com/phuc/synctask/model/Task.kt](../app/src/main/java/com/phuc/synctask/model/Task.kt)

Đây là mẫu dữ liệu dùng cho phần lưu local bằng Room.

Bạn có thể hiểu nó như một hàng trong cuốn sổ nội bộ của app. Mỗi task sẽ có:

- mã số,
- tiêu đề,
- hạn chót,
- độ ưu tiên,
- mức effort,
- trạng thái hoàn thành,
- loại task,
- tên dự án,
- người được giao.

Hàm `getPriorityValue()` giúp app biết task nào quan trọng hơn để sắp xếp cho hợp lý.

### [app/src/main/java/com/phuc/synctask/model/FirebaseTask.kt](../app/src/main/java/com/phuc/synctask/model/FirebaseTask.kt)

Đây là mẫu dữ liệu cho task lưu trên Firebase.

Khác với `Task` local, model này có thêm 2 cờ rất quan trọng:

- `isUrgent` = có gấp không,
- `isImportant` = có quan trọng không.

Từ hai cờ này, app chia task vào 4 ô:

- `DO_NOW` = làm ngay,
- `PLAN` = lên kế hoạch,
- `DELEGATE` = giao cho người khác,
- `ELIMINATE` = loại bỏ hoặc không cần làm.

Hàm `quadrant()` là phần giúp chuyển từ dữ liệu thô sang cách nhìn dễ hiểu cho người dùng.

### [app/src/main/java/com/phuc/synctask/model/GroupModels.kt](../app/src/main/java/com/phuc/synctask/model/GroupModels.kt)

File này có 2 mẫu dữ liệu:

- `Group`: thông tin của một nhóm,
- `GroupTask`: công việc nằm trong nhóm đó.

`Group` giống như một phòng họp nhỏ, còn `GroupTask` là các việc nằm trong phòng đó.

### [app/src/main/java/com/phuc/synctask/model/UserProfile.kt](../app/src/main/java/com/phuc/synctask/model/UserProfile.kt)

Đây là hồ sơ người dùng trên Firebase.

Nó giữ lại những thứ quan trọng như:

- mã user,
- tên hiển thị,
- email,
- danh sách thành tựu đã mở,
- số task nhóm đã hoàn thành.

File này là chỗ nối mọi thứ lại với nhau: cá nhân, nhóm và thành tựu.

### [app/src/main/java/com/phuc/synctask/model/AppNotification.kt](../app/src/main/java/com/phuc/synctask/model/AppNotification.kt)

Đây là dữ liệu cho thông báo trong app.

Mỗi thông báo có:

- tiêu đề,
- nội dung,
- thời gian,
- trạng thái đã đọc hay chưa.

Nói đơn giản: app không chỉ báo một lần rồi thôi, mà còn lưu lại để người dùng mở ra xem lại.

## Room database

### [app/src/main/java/com/phuc/synctask/data/AppDatabase.kt](../app/src/main/java/com/phuc/synctask/data/AppDatabase.kt)

Đây là “kho lưu local” của app.

Nó đăng ký entity `Task`, tạo database singleton, và nếu schema đổi quá nhiều thì có thể tạo lại database.

Nói bình dân: đây là nơi app cất dữ liệu trên máy chứ không chỉ trên cloud.

### [app/src/main/java/com/phuc/synctask/data/TaskDao.kt](../app/src/main/java/com/phuc/synctask/data/TaskDao.kt)

DAO là bộ phận nói chuyện với database local.

Nó có đủ các việc cơ bản:

- thêm,
- sửa,
- xóa,
- đọc dữ liệu theo nhiều kiểu khác nhau,
- tính effort trong ngày,
- đếm số task,
- xóa theo project.

Nếu ví Room là cái tủ, thì DAO là tay mở tủ và lấy đúng ngăn cần dùng.

## Repository làm việc với Firebase

### [app/src/main/java/com/phuc/synctask/data/repository/FirebaseHomeTaskRepository.kt](../app/src/main/java/com/phuc/synctask/data/repository/FirebaseHomeTaskRepository.kt)

Repository này lo task cá nhân trên Firebase.

Nó làm các việc như:

- nghe task theo từng user,
- thêm task,
- xóa task,
- khôi phục task,
- đánh dấu hoàn thành,
- đọc profile user,
- lưu thành tựu đã mở.

Điểm hay là hàm lắng nghe trả về một nút tắt. Nghĩa là khi không cần nữa, ViewModel có thể dừng nghe để tránh tốn tài nguyên.

### [app/src/main/java/com/phuc/synctask/data/repository/FirebaseGroupRepository.kt](../app/src/main/java/com/phuc/synctask/data/repository/FirebaseGroupRepository.kt)

Repository này lo phần nhóm.

Nó cho phép:

- xem danh sách nhóm của user,
- tạo nhóm mới,
- tham gia nhóm bằng mã mời.

Phần tham gia nhóm dùng transaction để đỡ lỗi khi nhiều người thao tác gần như cùng lúc.

### [app/src/main/java/com/phuc/synctask/data/repository/FirebaseGroupTaskRepository.kt](../app/src/main/java/com/phuc/synctask/data/repository/FirebaseGroupTaskRepository.kt)

Đây là repository quan trọng nhất cho phần làm việc nhóm.

Nó xử lý gần như mọi thứ liên quan đến nhóm:

- đọc thông tin nhóm,
- đọc task nhóm,
- lấy tên thành viên,
- thêm task,
- giao task,
- nhận task,
- đổi trạng thái hoàn thành,
- cộng số task nhóm hoàn thành,
- lưu thành tựu,
- xóa và khôi phục task,
- rời nhóm,
- xóa luôn cả nhóm nếu owner giải tán,
- lấy token FCM để gửi thông báo.

Nếu phần cá nhân là một căn phòng gọn gàng, thì phần nhóm giống như một văn phòng nhỏ có nhiều người cùng làm việc một lúc.

### [app/src/main/java/com/phuc/synctask/data/repository/FirebaseNotificationRepository.kt](../app/src/main/java/com/phuc/synctask/data/repository/FirebaseNotificationRepository.kt)

Repository này chỉ lo một việc: thông báo.

Nó có thể:

- lấy danh sách thông báo,
- thêm thông báo mới,
- đánh dấu đã đọc.

Đây là cầu nối giữa các hành động trong app và khung thông báo của người dùng.

## ViewModel

### [app/src/main/java/com/phuc/synctask/viewmodel/AuthViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/AuthViewModel.kt)

Đây là nơi xử lý đăng nhập, đăng ký và quên mật khẩu.

Nó làm các việc rất đời thường nhưng quan trọng:

- kiểm tra email có hợp lệ không,
- kiểm tra mật khẩu có khớp không,
- gọi Firebase Auth,
- gửi email xác thực,
- lưu tên người dùng lên database,
- đổi lỗi kỹ thuật thành câu dễ hiểu.

Nói ngắn gọn, file này lo phần “cửa vào” của app.

### [app/src/main/java/com/phuc/synctask/viewmodel/ThemeViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/ThemeViewModel.kt)

ViewModel này nhớ app đang ở chế độ sáng hay tối.

Nó dùng DataStore để lưu lựa chọn của người dùng.

Nghĩa là hôm nay chọn tối thì lần sau mở app nó vẫn nhớ.

### [app/src/main/java/com/phuc/synctask/viewmodel/SoundSettingsViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/SoundSettingsViewModel.kt)

File này quản lý âm thanh của app.

Nó nhớ 2 thứ:

- có bật âm thanh không,
- âm lượng là bao nhiêu phần trăm.

### [app/src/main/java/com/phuc/synctask/viewmodel/OnboardingViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/OnboardingViewModel.kt)

File này lo chuyện người dùng đã xem hướng dẫn lần đầu hay chưa.

Nó trả về 3 trạng thái rất dễ hiểu:

- chưa load xong,
- lần đầu mở app,
- đã xem rồi.

### [app/src/main/java/com/phuc/synctask/viewmodel/NotificationViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/NotificationViewModel.kt)

File này điều khiển toàn bộ danh sách thông báo trên giao diện.

Nó:

- nghe dữ liệu thông báo từ repository,
- đếm thông báo chưa đọc,
- phát âm thanh khi có thông báo mới,
- cho phép đánh dấu từng cái hoặc tất cả là đã đọc.

### [app/src/main/java/com/phuc/synctask/viewmodel/HomeViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/HomeViewModel.kt)

Đây là bộ não của màn cá nhân.

Nó làm rất nhiều việc:

- nghe task cá nhân theo thời gian thực,
- thêm task,
- xóa task,
- khôi phục task,
- đổi trạng thái hoàn thành,
- tải profile user,
- kiểm tra thành tựu,
- phát âm thanh,
- tính task hôm nay, task hoàn thành, task quá hạn.

Nói theo kiểu dễ hình dung: đây là người đứng sau màn hình cá nhân, quyết định lúc nào phải hiện gì và làm gì.

### [app/src/main/java/com/phuc/synctask/viewmodel/GroupViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/GroupViewModel.kt)

File này lo danh sách nhóm.

Nó:

- theo dõi đăng nhập,
- tự load nhóm khi user vào,
- tạo nhóm,
- tham gia nhóm bằng mã mời,
- báo trạng thái thành công hoặc lỗi cho màn hình.

### [app/src/main/java/com/phuc/synctask/viewmodel/GroupTaskViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/GroupTaskViewModel.kt)

Đây là ViewModel của một nhóm cụ thể.

Nó lo:

- thông tin nhóm,
- danh sách task nhóm,
- tên thành viên,
- thêm task,
- giao hoặc nhận việc,
- đổi trạng thái task,
- tăng số task nhóm hoàn thành,
- mở khóa thành tựu nhóm,
- xóa hoặc khôi phục task,
- rời nhóm hoặc xóa nhóm.

Đây là phần rối nhất của app, vì nó chạm đến gần như mọi hệ thống khác: nhóm, task, thông báo, âm thanh, thành tựu.

### [app/src/main/java/com/phuc/synctask/viewmodel/DashboardViewModel.kt](../app/src/main/java/com/phuc/synctask/viewmodel/DashboardViewModel.kt)

File này gom dữ liệu để làm dashboard.

Nó nghe:

- task cá nhân,
- nhóm,
- task nhóm.

Sau đó nó đẩy dữ liệu sang phần tính toán để tạo biểu đồ và con số thống kê.

### [app/src/main/java/com/phuc/synctask/viewmodel/DashboardAnalyticsUseCase.kt](../app/src/main/java/com/phuc/synctask/viewmodel/DashboardAnalyticsUseCase.kt)

Đây là nơi tính toán ra các số liệu mà dashboard cần.

Nó tạo ra:

- số task hoàn thành,
- số task trễ hạn,
- dữ liệu cho biểu đồ Eisenhower,
- dữ liệu workload theo tuần hoặc tháng,
- tiến độ nhóm,
- danh sách task cần ưu tiên xử lý.

Nói dễ hiểu: đây là “máy tính nhỏ” biến dữ liệu thô thành con số và biểu đồ có ý nghĩa.

## Service và thông báo đẩy

### [app/src/main/java/com/phuc/synctask/service/SyncTaskMessagingService.kt](../app/src/main/java/com/phuc/synctask/service/SyncTaskMessagingService.kt)

Đây là service nhận tin nhắn từ Firebase Cloud Messaging.

Nó có 2 việc chính:

- lưu token mới của thiết bị,
- nhận message rồi tạo notification trên điện thoại.

Nếu nói bình dân, đây là cửa hậu để thông báo từ server có thể đi vào máy người dùng.

## Tiện ích và helper

### [app/src/main/java/com/phuc/synctask/util/SoundManager.kt](../app/src/main/java/com/phuc/synctask/util/SoundManager.kt)

File này dùng `SoundPool` để phát âm thanh ngắn.

Nó hợp cho các âm như:

- click,
- pháo hoa,
- thành tựu.

### [app/src/main/java/com/phuc/synctask/util/LocalSoundManager.kt](../app/src/main/java/com/phuc/synctask/util/LocalSoundManager.kt)

Đây là cách để truyền `SoundManager` vào các màn hình Compose mà không phải bưng qua quá nhiều lớp.

Nói dễ hình dung: nó giống như một chỗ đặt sẵn cái loa chung cho cả cây giao diện dùng chung.

### [app/src/main/java/com/phuc/synctask/util/NotificationHelper.kt](../app/src/main/java/com/phuc/synctask/util/NotificationHelper.kt)

File này hỗ trợ gửi push notification qua FCM HTTP v1.

Nó có nhắc khá rõ rằng:

- project id phải đúng,
- token OAuth2 phải có,
- nên để phần tạo token ở backend chứ không nên nhét cứng trong app production.

### [app/src/main/java/com/phuc/synctask/utils/AchievementManager.kt](../app/src/main/java/com/phuc/synctask/utils/AchievementManager.kt)

Đây là nơi chứa luật mở khóa thành tựu.

Nó định nghĩa các mốc như:

- task đầu tiên,
- 10 task,
- 50 task,
- 200 task,
- làm việc vào khung giờ đêm,
- hoàn thành trước deadline,
- hoàn thành task nhóm,
- làm tốt vai trò trưởng nhóm.

Hàm `checkAndUnlock()` sẽ được gọi sau khi task hoàn thành để xem người dùng có được nhận huy hiệu không.

### [app/src/main/java/com/phuc/synctask/utils/AppSoundEffects.kt](../app/src/main/java/com/phuc/synctask/utils/AppSoundEffects.kt)

File này định nghĩa âm thanh của app.

Có 2 phần:

- `AppSoundEffect`: danh sách loại âm thanh,
- `AppSoundPlayer`: bộ phát âm thanh chính.

Nó nối các hành động như tạo task, xóa task, hoàn thành task, đăng nhập, lỗi, thông báo… với file âm thanh tương ứng trong `raw/`.

### [app/src/main/java/com/phuc/synctask/utils/JsonSyncHelper.kt](../app/src/main/java/com/phuc/synctask/utils/JsonSyncHelper.kt)

File này hỗ trợ xuất và nhập JSON.

Nó có 3 việc:

- xuất task ra file JSON,
- đọc file JSON thành danh sách task,
- tìm file JSON trong thư mục Download.

### [app/src/main/java/com/phuc/synctask/utils/WorkloadChecker.kt](../app/src/main/java/com/phuc/synctask/utils/WorkloadChecker.kt)

File này dùng để kiểm tra một ngày có quá tải công việc không.

Nếu effort trong ngày vượt ngưỡng, app sẽ cảnh báo người dùng.

Nói kiểu dễ hiểu: nó giống một người nhắc “ngày này nặng quá rồi, đừng nhồi thêm”.
