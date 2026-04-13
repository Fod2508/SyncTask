# Kịch bản tương tác đầy đủ trong ứng dụng SyncTask

## Mục tiêu tài liệu

Tài liệu mô tả chi tiết toàn bộ thao tác người dùng có thể bấm, thay đổi và tương tác trong ứng dụng, theo đúng luồng chạy thực tế của mã nguồn.

## 1. Luồng khởi động và điều hướng ban đầu

Khi mở ứng dụng:

1. `MainActivity` kiểm tra trạng thái đăng nhập Firebase.
2. Nếu chưa đăng nhập hoặc email chưa xác thực:
   - Điều hướng về `login`.
3. Nếu đã đăng nhập hợp lệ:
   - Nếu là lần đầu dùng: vào `welcome`.
   - Nếu không phải lần đầu: vào `main/false`.

Các điểm có thể tương tác:

- Nút bắt đầu ở `WelcomeScreen`.
- Điều hướng qua lại giữa `Login` và `Register`.

### Luồng hoạt động

1. App khởi tạo và kiểm tra trạng thái phiên hiện tại.
2. Hệ thống đánh giá điều kiện xác thực email và onboarding.
3. Điều hướng vào đúng điểm bắt đầu (`login`, `welcome`, hoặc `main`).
4. Người dùng thực hiện thao tác tiếp theo từ màn hình được cấp quyền.

## 2. Đăng ký tài khoản

Màn hình: `RegisterScreen`

Người dùng có thể thao tác:

1. Nhập **Họ tên**, **Email**, **Mật khẩu**, **Xác nhận mật khẩu**.
2. Bấm nút **Đăng ký**.
3. Hệ thống kiểm tra:
   - Tên không rỗng.
   - Email đúng định dạng.
   - Mật khẩu đủ độ dài.
   - Mật khẩu xác nhận khớp.
4. Nếu hợp lệ:
   - Tạo tài khoản Firebase Auth.
   - Cập nhật `displayName`.
   - Ghi hồ sơ lên `/users/{uid}`.
   - Gửi email xác thực.
   - Đăng xuất tạm thời để buộc xác thực email.

Trạng thái hiển thị:

- Loading khi gửi request.
- Thông báo thành công và hướng dẫn kiểm tra email.
- Thông báo lỗi theo từng trường hợp.

### Luồng hoạt động

1. Người dùng nhập form và gửi yêu cầu đăng ký.
2. ViewModel validate dữ liệu cục bộ trước khi gọi Firebase.
3. Firebase tạo tài khoản, cập nhật profile và ghi hồ sơ người dùng.
4. Hệ thống gửi email xác thực và kết thúc bằng thông báo trạng thái cho UI.

## 3. Đăng nhập

Màn hình: `LoginScreen`

Người dùng có thể thao tác:

1. Nhập email và mật khẩu.
2. Bấm **Đăng nhập**.
3. Bấm **Quên mật khẩu** để gửi email reset.
4. Bấm **Chưa có tài khoản? Đăng ký** để chuyển màn hình.

Luồng xác thực:

1. Validate dữ liệu đầu vào.
2. Firebase Auth kiểm tra tài khoản.
3. Nếu email chưa xác thực:
   - Tự gửi lại email xác thực.
   - Không cho vào app chính.
4. Nếu hợp lệ:
   - Điều hướng vào `welcome` hoặc `main`.

### Luồng hoạt động

1. Người dùng gửi thông tin đăng nhập.
2. ViewModel xác thực đầu vào và gọi Firebase Auth.
3. Hệ thống phân nhánh theo trạng thái xác thực email.
4. Nếu hợp lệ, điều hướng tới luồng sử dụng chính của ứng dụng.

## 4. Hướng dẫn Spotlight (Tutorial)

Màn hình: `MainScreen` + `SpotlightOverlay`

Điểm tương tác:

1. Lần đầu vào app: tutorial tự chạy theo bước.
2. Có thể bấm **Next** để sang bước kế tiếp.
3. Có thể bấm **Skip** để bỏ qua.
4. Có thể bấm icon hướng dẫn trên TopBar để xem lại tutorial.

Các mục được spotlight:

- Tab Cá nhân.
- Tab Nhóm.
- Tab Thành tựu.
- Tab Dashboard.

### Luồng hoạt động

1. Tutorial kích hoạt theo từng bước spotlight.
2. Người dùng chọn Next để đi tuần tự hoặc Skip để kết thúc sớm.
3. Kết quả tutorial được lưu để kiểm soát hiển thị cho lần mở sau.
4. Người dùng có thể chủ động mở lại tutorial từ TopBar bất kỳ lúc nào.

## 5. Chức năng Cá nhân (Personal)

Màn hình chính: `PersonalTaskScreen`

## 5.1 Thêm task mới

### Luồng hoạt động

Người dùng mở form, nhập thông tin, gửi yêu cầu; ViewModel ghi dữ liệu vào Firebase và UI đồng bộ realtime danh sách task mới.

### Các bước thao tác chi tiết trên UI

1. Bấm nút nổi `+` ở góc phải dưới `MainScreen` khi đang ở tab Cá nhân.
2. `AddTaskBottomSheet` xuất hiện với các trường nhập liệu.
3. Nhập **Tiêu đề** (bắt buộc):
   - Nếu để trống, form không hợp lệ và không tạo task.
4. Nhập **Mô tả** (không bắt buộc):
   - Dùng để bổ sung ngữ cảnh công việc.
5. Chọn 2 thuộc tính phân loại:
   - `isUrgent` (Khẩn cấp).
   - `isImportant` (Quan trọng).
6. Chọn **hạn hoàn thành** `dueDate` (không bắt buộc):
   - Nếu không chọn, task vẫn được tạo bình thường.
7. Bấm **Lưu** để submit.
8. BottomSheet đóng lại khi tạo thành công.

### Dữ liệu ghi xuống Firebase

- Điểm vào xử lý: `HomeViewModel.addTask(...)`.
- Repository ghi vào node: `/tasks/{uid}/{taskId}`.
- Các trường chính được lưu:
  - `id`
  - `title`
  - `description`
  - `isUrgent`
  - `isImportant`
  - `isCompleted = false`
  - `timestamp`
  - `dueDate` (nếu có)

### Phản hồi sau khi tạo

1. Danh sách task cập nhật realtime qua listener `observeTasks`.
2. Có âm thanh phản hồi thao tác tạo task.
3. Có thể tạo thêm thông báo nội bộ tùy luồng ViewModel hiện tại.
4. Task mới xuất hiện ngay trên danh sách, không cần tải lại màn hình.

### Trường hợp lỗi cần lưu ý

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Mất mạng khi bấm lưu | Task không xuất hiện | Kiểm tra mạng và tạo lại |
| Chưa đăng nhập | Không tạo được task | Đăng nhập lại |
| Dữ liệu tiêu đề trống | Form không hợp lệ | Nhập tiêu đề rồi lưu |

## 5.2 Task vào 4 mục Eisenhower như thế nào

### Luồng hoạt động

Sau khi lưu task, hệ thống đọc cặp cờ `isUrgent/isImportant`, ánh xạ sang quadrant tương ứng và hiển thị đúng ô trong ma trận.

Task được phân vào 4 mục theo 2 cờ:

| isUrgent | isImportant | Mục xuất hiện |
|---|---|---|
| true | true | Do Now |
| false | true | Plan |
| true | false | Delegate |
| false | false | Eliminate |

### Cách tạo đúng mục ngay từ lúc nhập form

1. Trong form thêm task, bật/tắt 2 công tắc tương ứng.
2. Sau khi lưu, task tự nằm đúng ô ma trận.
3. Bấm vào từng ô để vào `QuadrantDetailScreen` xem danh sách chi tiết.

### Quy tắc phân loại ở mức code

Hệ thống dùng hàm `quadrant()` của model `FirebaseTask` để suy ra ô hiển thị:

1. Nếu `isUrgent = true` và `isImportant = true` -> `DO_NOW`.
2. Nếu `isUrgent = false` và `isImportant = true` -> `PLAN`.
3. Nếu `isUrgent = true` và `isImportant = false` -> `DELEGATE`.
4. Còn lại -> `ELIMINATE`.

### Hành vi khi thay đổi trạng thái task

- Đổi trạng thái hoàn thành **không làm thay đổi ô Eisenhower**, vì ô phụ thuộc `isUrgent/isImportant`.
- Chỉ khi tạo mới hoặc sửa 2 cờ này (nếu có luồng chỉnh sửa) thì task mới chuyển ô.

## 5.3 Tương tác trên từng task cá nhân

### Luồng hoạt động

Mỗi thao tác trên task (check, swipe, mở chi tiết) đi qua ViewModel để cập nhật dữ liệu, sau đó phản hồi lại UI bằng trạng thái mới và hiệu ứng liên quan.

Người dùng có thể:

1. Bấm icon check để hoàn thành/chưa hoàn thành.
2. Vuốt trái để xoá task.
3. Vuốt phải để toggle trạng thái nhanh.
4. Bấm vào task để xem chi tiết.

### Chi tiết từng thao tác

## a) Bấm icon check (toggle hoàn thành)

1. Nếu task đang chưa hoàn thành:
   - Chuyển sang hoàn thành.
   - Chạy logic kiểm tra đúng hạn/trễ hạn.
2. Nếu task đã hoàn thành:
   - Chuyển về chưa hoàn thành.
3. UI thay đổi ngay:
   - Gạch ngang tiêu đề.
   - Đổi màu trạng thái hiển thị.

## b) Vuốt trái để xóa

1. Task bị remove khỏi danh sách hiện tại.
2. Dữ liệu bị xóa ở `/tasks/{uid}/{taskId}`.
3. Danh sách tự cập nhật do listener realtime.

## c) Vuốt phải để toggle nhanh

1. Tương đương hành vi bấm icon check.
2. Sau khi xử lý xong, item trở về vị trí ban đầu để tiếp tục thao tác.

## d) Bấm card task để xem chi tiết

1. Mở bottom sheet chi tiết.
2. Hiển thị đầy đủ nội dung: tiêu đề, mô tả, deadline.
3. Người dùng có thể đóng lại để quay về danh sách.

### Chuỗi phản hồi khi hoàn thành task

- Có hiệu ứng pháo hoa (Konfetti) trong một số màn hình.
- Phát âm thanh theo trạng thái đúng hạn/trễ hạn.
- Tạo thông báo nội bộ.
- Kiểm tra mở khóa thành tựu.

### Điều kiện đúng hạn và trễ hạn

1. Nếu task có `dueDate` và thời điểm hoàn thành `<= dueDate` -> đúng hạn.
2. Nếu thời điểm hoàn thành `> dueDate` -> trễ hạn.
3. Nếu task không có `dueDate`:
   - Không xét đúng hạn/trễ hạn theo deadline cứng.

### Thành tựu có thể kích hoạt từ cụm Cá nhân

- Hoàn thành task đầu tiên.
- Hoàn thành mốc nhiều task.
- Hoàn thành trước hạn.
- Hoàn thành trong khung giờ đặc biệt (theo logic `AchievementManager`).

### Dấu hiệu UI giúp người dùng nhận biết trạng thái

| Trạng thái task | Biểu hiện giao diện |
|---|---|
| Chưa hoàn thành | Icon chưa check, chữ bình thường |
| Đã hoàn thành | Icon check, tiêu đề gạch ngang/mờ |
| Sắp đến hạn | Màu deadline cảnh báo |
| Quá hạn | Màu deadline lỗi (đỏ) |

## 6. Chức năng Nhóm (Group)

Màn hình chính: `GroupListScreen`, `GroupTaskScreen`

## 6.1 Tạo nhóm

### Luồng hoạt động

Người dùng gửi tên nhóm, hệ thống tạo bản ghi nhóm mới trên Firebase, sinh mã mời và đồng bộ kết quả về danh sách nhóm.

### Các bước thao tác chi tiết trên UI

1. Vào tab **Nhóm** từ Bottom Navigation.
2. Tại khu vực tạo nhóm, nhập **tên nhóm**.
3. Bấm nút **Tạo nhóm**.
4. Hệ thống phản hồi bằng thông báo thành công kèm **mã mời**.
5. Nhóm mới xuất hiện ngay trong danh sách nhóm đang tham gia.

### Dữ liệu ghi xuống Firebase

- Điểm vào xử lý: `GroupViewModel.createGroup(name, onResult)`.
- Repository xử lý: `FirebaseGroupRepository.createGroup(uid, name)`.
- Node được tạo: `/groups/{groupId}` với các trường:
  - `id = groupId`
  - `name`
  - `inviteCode` (6 ký tự)
  - `ownerId = uid người tạo`
  - `members = [ownerUid]`
- Đồng thời tạo một thông báo nội bộ cho owner:
  - Node `/notifications/{uid}/{notificationId}`.

### Phản hồi UI sau khi tạo nhóm

1. Danh sách nhóm tự cập nhật realtime.
2. Người dùng có thể bấm vào nhóm ngay sau khi tạo.
3. Có thông điệp xác nhận giúp sao chép/chia sẻ mã mời.

### Trường hợp lỗi thường gặp

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Chưa đăng nhập | Không tạo được nhóm | Đăng nhập lại |
| Tên nhóm trống | Tạo nhóm thất bại | Nhập tên hợp lệ |
| Mất mạng | Không nhận phản hồi thành công | Kiểm tra mạng và thử lại |

## 6.2 Vào nhóm bằng mã mời

### Luồng hoạt động

Ứng dụng nhận mã mời, tra cứu nhóm theo `inviteCode`, chạy transaction thêm thành viên rồi cập nhật realtime danh sách nhóm của người dùng.

### Các bước thao tác chi tiết

1. Tại tab Nhóm, nhập mã mời vào ô tham gia.
2. Bấm **Tham gia nhóm**.
3. Ứng dụng tự chuyển mã về uppercase trước khi gửi.
4. Hệ thống kiểm tra:
   - Mã có tồn tại không.
   - Người dùng đã là thành viên chưa.
5. Nếu hợp lệ, thêm `uid` vào `members` bằng transaction.

### Luồng code xử lý

- Điểm vào: `GroupViewModel.joinGroup(inviteCode, onResult)`.
- Repository: `FirebaseGroupRepository.joinGroup(uid, inviteCode.uppercase())`.
- Cơ chế transaction trên node `members`:
  - Nếu `uid` đã tồn tại -> `Transaction.abort()`.
  - Nếu chưa có -> thêm uid và commit.
- Trạng thái trả về:
  - `Joined`
  - `AlreadyMember`
  - `NotFound`

### Dữ liệu thay đổi khi join thành công

1. `/groups/{groupId}/members` thêm `uid`.
2. Chủ nhóm nhận thông báo “thành viên mới” qua node notifications.
3. Người dùng vừa join thấy nhóm xuất hiện trong danh sách của mình.

## 6.3 Mở chi tiết nhóm

### Luồng hoạt động

Khi bấm vào nhóm, app điều hướng vào `GroupTaskScreen`, tải thông tin nhóm và task nhóm để người dùng tiếp tục thao tác cộng tác.

1. Bấm vào một nhóm trong danh sách.
2. Điều hướng vào `GroupTaskScreen`.
3. Xem danh sách task nhóm, thành viên và các thao tác phân công.

### Thành phần có thể tương tác trong `GroupTaskScreen`

- Danh sách task nhóm.
- Nút tạo task nhóm.
- Hành động claim/assign.
- Nút toggle trạng thái hoàn thành.
- Nút rời nhóm hoặc xóa nhóm.

## 6.4 Thêm task nhóm

### Luồng hoạt động

Người dùng nhập thông tin task nhóm, hệ thống ghi vào `/groupTasks`, rồi phát sinh thông báo/đẩy push nếu có người được phân công.

### Các bước thao tác chi tiết trên UI

1. Trong `GroupTaskScreen`, bấm nút **Thêm task nhóm**.
2. Nhập:
   - Tiêu đề (bắt buộc).
   - Mô tả (tuỳ chọn).
   - Hạn hoàn thành (tuỳ chọn).
3. Chọn người được giao:
   - Có thể để trống để task chưa giao cho ai.
   - Có thể chỉ định ngay 1 thành viên.
4. Bấm lưu để tạo task.

### Dữ liệu ghi xuống Firebase

- Điểm vào: `GroupTaskViewModel.addGroupTask(...)`.
- Repository: `FirebaseGroupTaskRepository.addGroupTask(...)`.
- Node ghi: `/groupTasks/{groupId}/{taskId}` gồm:
  - `id`, `groupId`
  - `title`, `description`
  - `isCompleted = false`
  - `creatorId`
  - `assignedToId` (có thể null)
  - `timestamp`
  - `dueDate` (có thể null)

### Phản hồi hệ thống khi có người được giao

1. Tạo thông báo nội bộ cho người được giao.
2. Lấy `fcmToken` từ `/users/{assignedToId}/fcmToken`.
3. Nếu có token hợp lệ, gửi push notification.

### Phản hồi UI

- Task mới xuất hiện realtime trong danh sách nhóm.
- Người tạo nhận phản hồi âm thanh thành công.

## 6.5 Nhận việc và phân công

### Luồng hoạt động

Thao tác claim/assign cập nhật `assignedToId` của task nhóm, đồng bộ người nhận mới trên UI và gửi thông báo cho đối tượng liên quan.

Người dùng có thể:

1. **Claim task** (nhận việc về mình).
2. **Assign task** (giao cho thành viên khác).
3. Đổi người nhận nhiều lần theo tiến độ thực tế.

Mỗi lần phân công:

- Cập nhật `assignedToId`.
- Gửi thông báo tương ứng.

### Chi tiết xử lý theo code

## a) Claim task

1. Người dùng bấm nhận việc.
2. `GroupTaskViewModel.claimTask(groupId, taskId)` gọi repository.
3. Repository cập nhật `assignedToId = currentUserUid`.
4. UI refresh realtime và hiển thị task đã thuộc về người dùng.

## b) Assign task cho thành viên khác

1. Người dùng chọn thành viên từ danh sách.
2. `GroupTaskViewModel.assignTask(...)` cập nhật `assignedToId`.
3. Nếu người nhận khác người gán:
   - Tạo thông báo nội bộ cho người nhận.
   - Thử gửi push qua FCM.

### Trường hợp đặc biệt

| Tình huống | Hành vi hệ thống |
|---|---|
| Giao cho chính mình | Chỉ cập nhật assignee, không cần thông báo vòng lặp |
| Giao cho user chưa có token | Vẫn tạo in-app notification, bỏ qua push |
| Nhiều người thao tác cùng lúc | Dữ liệu cuối cùng theo lần ghi sau cùng |

## 6.6 Hoàn thành task nhóm

### Luồng hoạt động

Hệ thống transaction trạng thái hoàn thành, cập nhật bộ đếm đóng góp nhóm, kiểm tra thành tựu rồi phản hồi lại danh sách task theo thời gian thực.

1. Bấm đổi trạng thái hoàn thành trên task nhóm.
2. Hệ thống transaction cập nhật `isCompleted`.
3. Tính `delta` để cộng/trừ `groupTaskCount`.
4. Kiểm tra thành tựu nhóm.

### Phân tích sâu luồng hoàn thành task nhóm

1. Hàm xử lý: `GroupTaskViewModel.toggleTaskStatus(groupId, task)`.
2. Repository dùng transaction ở `toggleTaskStatus` để tránh lệch trạng thái khi đồng thời cập nhật.
3. Kết quả trả về:
   - `delta = +1` nếu task vừa hoàn thành.
   - `delta = -1` nếu task bị bỏ hoàn thành.
4. Hệ thống gọi `applyGroupTaskCountDelta(uid, delta)`:
   - Cập nhật bộ đếm task nhóm hoàn thành của user.
   - Chặn giá trị âm bằng `coerceAtLeast(0)`.
5. Nếu vừa hoàn thành:
   - Phân nhánh đúng hạn/trễ hạn.
   - Tạo thông báo nội bộ.
   - Kiểm tra thành tựu nhóm và hiện popup.

### Dấu hiệu UI khi task nhóm hoàn thành

- Task chuyển trạng thái completed.
- Thứ tự hiển thị có thể thay đổi theo logic sort của danh sách.
- Âm thanh phản hồi theo loại hoàn thành.

## 6.7 Rời nhóm hoặc xóa nhóm

### Luồng hoạt động

Ứng dụng xác định quyền owner/member, thực hiện rời nhóm hoặc xóa toàn bộ dữ liệu nhóm tương ứng, sau đó điều hướng người dùng về danh sách nhóm.

1. Nếu là member: bấm rời nhóm.
2. Nếu là owner: bấm xóa nhóm.

Kết quả:

- Member: uid bị xóa khỏi `members`.
- Owner: xóa cả node nhóm và toàn bộ task nhóm.

### Điều kiện quyền và hành vi chi tiết

1. Quyền được xác định bằng `uid == ownerId`.
2. Member:
   - Chỉ có quyền rời nhóm.
   - Không được xóa toàn bộ nhóm.
3. Owner:
   - Có quyền xóa nhóm và toàn bộ task liên quan.

### Dữ liệu Firebase thay đổi

| Vai trò thao tác | Node thay đổi | Kết quả |
|---|---|---|
| Member rời nhóm | `/groups/{groupId}/members` | Bỏ uid khỏi mảng |
| Owner xóa nhóm | `/groups/{groupId}` và `/groupTasks/{groupId}` | Xóa toàn bộ dữ liệu nhóm |

### Trường hợp lỗi cần lưu ý

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Mất mạng khi rời/xóa | UI chưa phản ánh ngay | Thử lại khi có mạng |
| Group đã bị xóa từ thiết bị khác | Màn hình lỗi hoặc rỗng | Quay lại danh sách nhóm và refresh |
| Không xác định được user hiện tại | Không thao tác được | Đăng nhập lại |

## 7. Thành tựu (Achievement)

Màn hình chính: `AchievementScreen`  
Popup khi mở khóa: `AchievementUnlockedDialog` (hiển thị từ màn hình có hoàn thành task, ví dụ luồng cá nhân / nhóm tương ứng ViewModel).

### 7.1 Cách vào màn hình Thành tựu

#### Luồng hoạt động

Người dùng chuyển tab Thành tựu, màn hình tải danh sách `unlockedAchievements` từ Firebase và render trạng thái khóa/mở của từng huy hiệu.

1. Ở `MainScreen`, bấm tab **Thành tựu** trên thanh điều hướng dưới.
2. Màn hình `AchievementScreen` tải dữ liệu đã mở khóa từ Firebase (một lần khi vào màn hình).

### 7.2 Các thao tác có thể bấm trên `AchievementScreen`

#### Luồng hoạt động

Mỗi lần đổi tab, hệ thống lọc tập huy hiệu theo nhóm hiển thị và giữ trạng thái mở khóa dựa trên id đã đồng bộ từ user profile.

| Thành phần | Hành vi khi bấm | Ghi chú |
|---|---|---|
| Tab **Cá nhân** | Chuyển lưới hiển thị thành tựu nhóm cá nhân | Dùng `TabRow` + `Tab` |
| Tab **Nhóm** | Chuyển lưới thành tựu liên quan nhóm | Cùng cơ chế tab |
| Tab **Đặc biệt** | Chuyển lưới thành tựu đặc biệt (dữ liệu tĩnh trên UI) | Chỉ hiển thị, chưa gắn logic mở khóa trong `AchievementManager` |
| Thẻ (Card) từng huy hiệu | **Không có** `onClick` | Người dùng chỉ xem, không mở chi tiết riêng |

### 7.3 Dữ liệu nguồn và cách đánh dấu đã mở khóa

#### Luồng hoạt động

Màn hình đọc danh sách id thành tựu đã mở, chuyển thành `Set` cục bộ và map lại từng item để quyết định `isUnlocked`.

1. Khi `AchievementScreen` khởi tạo, `LaunchedEffect(Unit)` gọi:
   - Đường dẫn: `/users/{uid}/unlockedAchievements`
   - Kiểu dữ liệu: danh sách `List<String>` (id thành tựu).
2. Danh sách được chuyển thành `Set<String>` trong bộ nhớ cục bộ.
3. Mỗi huy hiệu trong lưới được map lại:
   - `isUnlocked = (achievement.id in unlockedIds)`.

### 7.4 Biểu hiện giao diện khi đã mở / chưa mở

#### Luồng hoạt động

UI áp dụng style khác nhau theo cờ `isUnlocked`, giúp người dùng nhận biết ngay trạng thái thành tựu mà không cần thao tác bổ sung.

| Trạng thái | Giao diện |
|---|---|
| Đã mở | Icon có màu accent, nền card sáng hơn, độ nổi (elevation) lớn hơn, không có icon khóa |
| Chưa mở | Giảm `alpha`, icon xám theo theme, góc phải có icon **khóa**, elevation thấp |

### 7.5 Popup khi vừa mở khóa thành tựu (`AchievementUnlockedDialog`)

#### Luồng hoạt động

Khi ViewModel phát id thành tựu mới, dialog xuất hiện với animation + âm thanh + konfetti, sau đó đóng theo thao tác người dùng hoặc luồng dismiss.

Luồng người dùng nhìn thấy:

1. Dialog toàn màn che phủ với nội dung tên + mô tả thành tựu.
2. Có hiệu ứng **scale** (spring animation) khi xuất hiện.
3. Tự phát âm thanh `ACHIEVEMENT_UNLOCKED`.
4. Có hiệu ứng **Konfetti** hai bên.
5. Có nút để **đóng** dialog (`onDismiss`).

Luồng kỹ thuật (tóm tắt):

- ViewModel đặt `achievementUnlocked = achievementId`.
- UI lắng nghe StateFlow và hiển thị dialog khi giá trị khác null.
- Sau khi đóng, ViewModel gọi `dismissAchievementDialog()` để trả về null.

### 7.6 Điều kiện mở khóa thực sự trong code (`AchievementManager`)

#### Luồng hoạt động

Sau mỗi sự kiện hoàn thành task, `AchievementManager` kiểm tra điều kiện theo mốc và callback id mới để ViewModel lưu vào Firebase.

Các id do `AchievementManager` kiểm tra và có thể ghi vào Firebase:

| Id | Điều kiện (theo logic code) | Ngữ cảnh |
|---|---|---|
| `rookie_badge` | `completedCount >= 1` và chưa có trong danh sách đã mở | Task cá nhân |
| `diligent_badge` | `completedCount >= 10` | Task cá nhân |
| `warrior_badge` | `completedCount >= 50` | Task cá nhân |
| `legend_badge` | `completedCount >= 200` | Task cá nhân |
| `night_owl_badge` | Giờ hệ thống `Calendar.HOUR_OF_DAY` thuộc `1..4` và chưa mở | Task cá nhân (lưu ý: mô tả UI có thể ghi 1–5 giờ, nhưng điều kiện code là 1–4) |
| `on_time_badge` | Có `dueDate` và `System.currentTimeMillis() < dueDate` khi check | Task cá nhân |
| `team_player_badge` | `isGroupTask = true`, `groupTaskCount + 1 >= 5` | Task nhóm |
| `captain_badge` | `isGroupTask = true`, `isOwner = true` | Task nhóm, chủ nhóm |

Sau khi callback `onUnlocked` chạy:

1. Cập nhật bản sao `userProfile` cục bộ để tránh mở trùng trong cùng phiên.
2. Ghi lại danh sách đầy đủ `unlockedAchievements` lên `/users/{uid}`.
3. Có thể tạo thông báo nội bộ kèm tên thành tựu.

### 7.7 Thành tựu hiển thị trên UI nhưng chưa có logic mở khóa trong `AchievementManager`

#### Luồng hoạt động

Các item chỉ có trong danh sách tĩnh UI vẫn được render, nhưng nếu không có id trong Firebase thì luôn ở trạng thái khóa.

Trong `AchievementScreen`, tab **Nhóm** và **Đặc biệt** có một số mục id kiểu `group_join`, `group_star`, `pioneer`, …  
Các id này **chỉ tồn tại trong danh sách tĩnh của UI** để trình bày; hiện không thấy được ghi vào `unlockedAchievements` từ `AchievementManager`.  
Do đó:

- Nếu Firebase không chứa id tương ứng, thẻ luôn ở trạng thái **khóa**.
- Muốn các mục này hoạt động thật, cần bổ sung logic nghiệp vụ + ghi id vào `unlockedAchievements` (mở rộng tương lai).

### 7.8 Trường hợp lỗi và hành vi cần biết

#### Luồng hoạt động

Khi phát sinh lỗi tải dữ liệu hoặc trạng thái phiên, màn hình fallback theo dữ liệu hiện có và yêu cầu người dùng thực hiện thao tác khôi phục phù hợp.

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Chưa đăng nhập | `LaunchedEffect` không tải được `unlockedAchievements` | Đăng nhập lại |
| Node `unlockedAchievements` trống | Toàn bộ thẻ hiển thị khóa | Bình thường cho user mới |
| Mất mạng khi vào tab | Có thể không cập nhật danh sách đã mở | Kéo refresh / vào lại tab (hiện tại màn hình chỉ `get()` một lần) |
| Mở nhiều thành tựu liên tiếp | Dialog có thể chồng theo từng sự kiện | Đóng từng popup theo luồng ViewModel |

## 8. Thông báo

Màn hình chính: `NotificationBottomSheet`  \nDữ liệu và trạng thái: `NotificationViewModel`

### 8.1 Cách mở khu vực thông báo

#### Luồng hoạt động

Bấm icon chuông sẽ mở `NotificationBottomSheet`, nơi danh sách thông báo được lấy từ stream realtime của `NotificationViewModel`.

1. Ở `MainScreen`, bấm icon **chuông** trên TopBar.
2. `NotificationBottomSheet` mở dạng modal, chiều cao ~85% màn hình.
3. Danh sách thông báo hiển thị theo thứ tự mới nhất trước.

### 8.2 Các thao tác người dùng có thể bấm

#### Luồng hoạt động

Tùy trạng thái thông báo, người dùng có thể đánh dấu từng mục hoặc toàn bộ; hệ thống cập nhật dữ liệu và đồng bộ lại badge tức thì.

| Thành phần | Hành vi | Điều kiện |
|---|---|---|
| Icon chuông TopBar | Mở bottom sheet thông báo | Luôn bấm được khi ở màn chính |
| Badge số đỏ | Chỉ hiển thị số chưa đọc | `unreadCount > 0` |
| Nút “Đánh dấu đã đọc tất cả” | Đặt toàn bộ thông báo chưa đọc thành đã đọc | Chỉ xuất hiện khi còn ít nhất 1 thông báo chưa đọc |
| Từng item thông báo | Đánh dấu item đó đã đọc | Chỉ gọi cập nhật khi item đang `isRead = false` |
| Vuốt xuống/đóng sheet | Đóng bottom sheet | Không làm thay đổi dữ liệu |

### 8.3 Luồng dữ liệu và xử lý ở mức code

#### Luồng hoạt động

ViewModel đăng ký listener vào node notifications, parse dữ liệu thành model, rồi phát StateFlow để UI cập nhật danh sách và số chưa đọc.

1. `NotificationViewModel` khởi tạo và gọi `listenToNotifications()`.
2. Lấy `uid` hiện tại từ FirebaseAuth.
3. Đăng ký realtime listener ở node:
   - `/notifications/{uid}`.
4. Mỗi lần dữ liệu thay đổi:
   - Parse thành `List<AppNotification>`.
   - Sort giảm dần theo `timestamp`.
   - Cập nhật `_notifications`.
5. `unreadCount` được tính bằng stream:
   - `list.count { !it.isRead }`.

### 8.4 Cách đánh dấu đã đọc

#### Luồng hoạt động

Khi người dùng bấm đọc, app ghi `isRead=true` cho item mục tiêu hoặc lặp qua toàn bộ item chưa đọc, sau đó listener tự đồng bộ lại UI.

## a) Đánh dấu từng thông báo

1. Người dùng bấm vào một item chưa đọc.
2. UI gọi `viewModel.markAsRead(notificationId)`.
3. Repository cập nhật:
   - `/notifications/{uid}/{notificationId}/isRead = true`.
4. Listener realtime tự đồng bộ lại list và badge.

## b) Đánh dấu tất cả đã đọc

1. Người dùng bấm **Đánh dấu đã đọc tất cả**.
2. ViewModel duyệt toàn bộ item `!isRead`.
3. Gọi `markAsRead` cho từng item.
4. Sau khi cập nhật xong:
   - Nút “đánh dấu tất cả” biến mất.
   - Badge về 0.

### 8.5 Biểu hiện UI khi đã đọc/chưa đọc

#### Luồng hoạt động

Mỗi item đổi màu nền và kiểu chữ theo `isRead`, cho phép người dùng phân biệt nhanh thông báo mới và thông báo đã xử lý.

| Trạng thái | Giao diện |
|---|---|
| Chưa đọc | Nền card đậm hơn (primaryContainer), title đậm |
| Đã đọc | Nền nhạt hơn (surfaceVariant alpha), title thường |
| Không có dữ liệu | Hiển thị “Bạn chưa có thông báo nào.” |

Ngoài ra, mỗi thông báo hiển thị thời gian tương đối (ví dụ “5 phút trước”) qua `DateUtils.getRelativeTimeSpanString`.

### 8.6 Nguồn sinh thông báo trong ứng dụng

#### Luồng hoạt động

Thông báo được tạo từ các luồng nghiệp vụ chính (task, nhóm, thành tựu) và ghi tập trung về node notifications theo uid người nhận.

- Tạo task cá nhân.
- Hoàn thành task cá nhân (đúng hạn/trễ hạn).
- Tạo nhóm.
- Thành viên mới tham gia nhóm.
- Tạo task nhóm.
- Được phân công task nhóm.
- Mở khóa thành tựu.

### 8.7 Âm thanh thông báo

#### Luồng hoạt động

ViewModel so sánh thông báo mới với `appStartTime`; chỉ khi thỏa điều kiện mới phát âm để tránh lặp âm từ dữ liệu lịch sử.

`NotificationViewModel` có logic chống phát âm thanh cho dữ liệu cũ:

1. Lưu `appStartTime` tại thời điểm khởi tạo ViewModel.
2. Chỉ phát âm thanh `NOTIFICATION` khi có item mới:
   - Có `timestamp > appStartTime`.
   - Và item mới ở trạng thái chưa đọc.

Mục tiêu:

- Tránh phát âm thanh hàng loạt khi app vừa mở và tải lịch sử cũ.

### 8.8 Trường hợp lỗi và edge cases

#### Luồng hoạt động

Khi gặp lỗi mạng hoặc dữ liệu không chuẩn, hệ thống giữ hành vi an toàn (không crash) và chờ chu kỳ đồng bộ tiếp theo để phục hồi.

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Chưa đăng nhập | Không load được list thông báo | Đăng nhập lại |
| Mất mạng | Danh sách không cập nhật realtime | Kiểm tra mạng, mở lại sheet |
| Dữ liệu nhiều | Cuộn danh sách dài | Dùng LazyColumn, vẫn ổn ở mức hiện tại |
| Bấm liên tiếp “đã đọc tất cả” | Có thể gọi nhiều request lặp | Không gây sai dữ liệu, nhưng có thể tối ưu batch trong tương lai |
| timestamp thiếu/chưa chuẩn | Thời gian tương đối hiển thị không đúng kỳ vọng | Chuẩn hóa dữ liệu ghi vào `timestamp` |

## 9. Dashboard thống kê

Màn hình: `DashboardScreen`  \nViewModel xử lý: `DashboardViewModel`  \nUse case tính toán: `DashboardAnalyticsUseCase`

### 9.1 Cách mở Dashboard và phạm vi dữ liệu

#### Luồng hoạt động

Người dùng mở tab Dashboard, ViewModel khởi tạo listener đa nguồn và tổng hợp dữ liệu cá nhân/nhóm vào một trạng thái hiển thị thống nhất.

1. Từ Bottom Navigation, bấm tab **Dashboard**.
2. Màn hình tải dữ liệu tổng hợp từ nhiều node Firebase:
   - `/tasks/{uid}` (task cá nhân).
   - `/groups` (lọc theo nhóm có chứa uid hiện tại).
   - `/groupTasks/{groupId}` cho từng nhóm mà user tham gia.
3. Khi dữ liệu đổi ở bất kỳ node nào, dashboard tự tính toán lại và render lại.

### 9.2 Các thành phần có thể tương tác

#### Luồng hoạt động

Dashboard nhận thao tác chủ yếu qua bộ lọc thời gian; các phần còn lại đóng vai trò hiển thị kết quả đã tính toán.

| Thành phần | Hành vi khi bấm | Kết quả |
|---|---|---|
| Nút lọc “Tuần này/Tháng này” | Mở `DropdownMenu` | Cho phép chọn phạm vi thống kê |
| Item “Tuần này” | `setFilter(WEEK)` | Tính lại dữ liệu theo tuần |
| Item “Tháng này” | `setFilter(MONTH)` | Tính lại dữ liệu theo tháng |
| Các thẻ số tổng quan | Chỉ hiển thị, không click | Cập nhật realtime theo dữ liệu |
| Biểu đồ + danh sách | Cuộn dọc để xem | Không thao tác sửa dữ liệu trực tiếp |

### 9.3 Chuyển bộ lọc WEEK/MONTH hoạt động như thế nào

#### Luồng hoạt động

Mỗi lần đổi filter, ViewModel cập nhật `filterType`, kích hoạt pipeline tính toán lại và render lại toàn bộ card/biểu đồ tương ứng.

1. Người dùng bấm nút lọc ở góc phải phần header.
2. Chọn một trong hai chế độ:
   - `WEEK`: phạm vi từ thứ 2 đến hết chủ nhật tuần hiện tại.
   - `MONTH`: phạm vi từ ngày 1 đến hết tháng hiện tại.
3. `DashboardViewModel` cập nhật `_filterType`.
4. Luồng `combine(...)` tự gọi lại `recalculateDashboardData(...)`.
5. Tất cả card/biểu đồ/list đồng bộ theo bộ lọc mới.

### 9.4 Ý nghĩa và nguồn dữ liệu của từng khối hiển thị

#### Luồng hoạt động

Mỗi khối UI lấy dữ liệu từ kết quả compute chung, đảm bảo mọi chỉ số được cập nhật đồng bộ theo cùng một mốc lọc thời gian.

## a) Thẻ tổng quan (Cá nhân / Nhóm / Quá hạn)

- **Cá nhân**: số task cá nhân hoàn thành trong phạm vi lọc.
- **Nhóm**: số task nhóm hoàn thành mà user được giao trong phạm vi lọc.
- **Quá hạn**: tổng task chưa hoàn thành nhưng đã quá `dueDate` (cả cá nhân + nhóm được giao).

## b) Cân bằng công việc (Stacked Bar Chart)

- Mỗi cột là một mốc thời gian:
  - WEEK: theo ngày trong tuần.
  - MONTH: theo tuần trong tháng.
- Mỗi cột gồm 3 lớp:
  - Personal done.
  - Group done.
  - Overdue.

## c) Phân bổ Cá nhân (Donut Eisenhower)

- Thể hiện 4 ô:
  - Làm ngay.
  - Lên kế hoạch.
  - Ủy quyền.
  - Loại bỏ.
- Dữ liệu dựa trên task cá nhân trong phạm vi lọc và hàm `quadrant()`.

## d) Tiến độ dự án nhóm

- Mỗi nhóm hiển thị:
  - Tỷ lệ hoàn thành task của nhóm.
  - Mức đóng góp của user (số task được assign cho user trong nhóm đó).

## e) Tiêu điểm cần xử lý (Focus Tasks)

- Trộn từ:
  - Task cá nhân chưa hoàn thành.
  - Task nhóm được giao cho user và chưa hoàn thành.
- Sắp xếp ưu tiên:
  1. Task quá hạn.
  2. Deadline gần hơn.
  3. Theo tên task (ổn định hiển thị).
- Chỉ lấy top 3 mục để người dùng tập trung.

### 9.5 Luồng tính toán ở mức code

#### Luồng hoạt động

Dữ liệu thô từ nhiều listener đi qua `combine`, rồi `DashboardAnalyticsUseCase` xử lý và trả về các chỉ số cấu trúc cho UI.

1. `DashboardViewModel.startListening()` tạo nhiều listener Firebase.
2. Dữ liệu thô được đẩy vào:
   - `_rawPersonalTasks`
   - `_userGroups`
   - `_rawGroupTasks`
3. `combine(_rawPersonalTasks, _userGroups, _rawGroupTasks, _filterType)` gọi `recalculateDashboardData`.
4. `DashboardAnalyticsUseCase.compute(...)` trả về:
   - `personalCompleted`
   - `groupCompleted`
   - `overdueCount`
   - `eisenhowerStats`
   - `workload`
   - `groupProgress`
   - `pendingFocusTasks`
5. ViewModel cập nhật các StateFlow tương ứng để UI render.

### 9.6 Trường hợp tải dữ liệu và hiệu năng

#### Luồng hoạt động

Khi số nhóm hoặc dữ liệu tăng, ViewModel quản lý vòng đời listener để giới hạn rò rỉ và duy trì hiệu năng chấp nhận được.

| Tình huống | Hiện tượng | Cách xử lý/ghi chú |
|---|---|---|
| User tham gia nhiều nhóm | Số listener `groupTasks` tăng theo số nhóm | Hiện tại vẫn ổn ở quy mô nhỏ; có thể tối ưu bằng tổng hợp server-side nếu mở rộng lớn |
| Nhóm bị xóa trong lúc đang mở dashboard | Dữ liệu nhóm biến mất ở lần cập nhật tiếp theo | ViewModel đã có cơ chế remove listener nhóm không còn tồn tại |
| Mất mạng tạm thời | Dữ liệu có thể đứng yên | Khi có mạng lại, listener sẽ đồng bộ lại |
| Chưa đăng nhập | Dashboard không có dữ liệu | Cần quay lại login |

### 9.7 Các thao tác không hỗ trợ trực tiếp trong Dashboard

#### Luồng hoạt động

Dashboard giữ vai trò quan sát và ưu tiên xử lý; mọi thao tác chỉnh sửa dữ liệu được chuyển sang các màn nghiệp vụ chuyên biệt.

- Không tạo/sửa/xóa task trực tiếp tại Dashboard.
- Không bấm vào biểu đồ để drill-down sang màn chi tiết (phiên bản hiện tại).
- Dashboard tập trung vai trò **quan sát và ưu tiên xử lý**, không thay thế màn thao tác nghiệp vụ.

## 10. Đổi màu giao diện, âm thanh, hiệu ứng

Màn hình thao tác chính: TopBar trong `MainScreen`  \nViewModel liên quan: `ThemeViewModel`, `SoundSettingsViewModel`  \nEngine âm thanh: `AppSoundPlayer`

### 10.1 Các thành phần có thể bấm trong cụm thiết lập trải nghiệm

#### Luồng hoạt động

Người dùng thao tác trên TopBar để mở/đổi cấu hình, sau đó trạng thái mới được đẩy xuống UI và bộ phát âm thanh theo thời gian thực.

| Thành phần trên TopBar | Hành vi khi bấm | Kết quả |
|---|---|---|
| Icon mặt trời/trăng | Đổi theme sáng/tối | Toàn bộ app đổi giao diện ngay |
| Icon âm lượng | Mở dialog cài đặt âm thanh | Cho phép bật/tắt SFX và chỉnh volume |
| Switch trong dialog âm thanh | Bật/tắt toàn bộ hiệu ứng âm thanh | `AppSoundPlayer` dừng/phát lại BGM và SFX |
| Slider âm lượng | Đổi mức 0–100 | Cập nhật volume SFX và BGM theo tỷ lệ |

### 10.2 Đổi màu giao diện sáng/tối hoạt động như thế nào

#### Luồng hoạt động

Nút theme ghi giá trị mới vào DataStore, `MainActivity` collect lại và áp dụng vào `SyncTaskTheme` để recompose toàn app.

1. Người dùng bấm icon theme trên TopBar.
2. `MainScreen` gọi `themeViewModel.toggleTheme()`.
3. `ThemeViewModel` ghi giá trị mới vào DataStore:
   - File prefs: `theme_prefs`
   - Key: `dark_mode`
4. `MainActivity` đang collect `isDarkTheme`:
   - Truyền vào `SyncTaskTheme(useDarkTheme = isDark)`.
5. Compose recompose toàn cục, giao diện đổi ngay.

### 10.3 Âm thanh và âm lượng hoạt động như thế nào

#### Luồng hoạt động

Dialog âm thanh cập nhật DataStore cho cờ bật/tắt và volume; `AppSoundPlayer` nhận state mới để điều chỉnh SFX/BGM tương ứng.

## a) Luồng bật/tắt âm thanh

1. Mở dialog âm thanh bằng icon loa.
2. Gạt Switch “Bật hiệu ứng âm thanh”.
3. `SoundSettingsViewModel.setEnabled(value)` ghi DataStore:
   - File prefs: `sound_prefs`
   - Key: `sound_enabled`
4. `MainActivity` collect state và gọi:
   - `AppSoundPlayer.setEnabled(soundSettings.isEnabled)`.

Tác động thực tế:

- Nếu tắt:
  - SFX không phát.
  - BGM bị pause.
- Nếu bật lại:
  - BGM resume.
  - SFX phát bình thường khi có sự kiện.

## b) Luồng chỉnh âm lượng

1. Kéo slider 0–100 trong dialog.
2. `SoundSettingsViewModel.setVolumePercent(value)`:
   - Tự `coerceIn(0, 100)`.
   - Ghi DataStore key `sound_volume`.
3. `MainActivity` apply lại qua:
   - `AppSoundPlayer.setVolumePercent(...)`.

Chi tiết kỹ thuật:

- SFX phát theo mức `volumePercent / 100f`.
- BGM được giảm nhẹ còn khoảng 40% mức SFX (`* 0.4f`) để không lấn tiếng hiệu ứng.

### 10.4 Danh sách hiệu ứng âm thanh trong app

#### Luồng hoạt động

Mỗi sự kiện nghiệp vụ map tới một `AppSoundEffect`, sau đó `AppSoundPlayer` phát file âm tương ứng nếu cấu hình cho phép.

`AppSoundPlayer` map nhiều sự kiện sang file âm thanh:

- `TASK_CREATED`
- `TASK_DELETED`
- `TASK_RESTORED`
- `TASK_COMPLETED_ON_TIME`
- `TASK_COMPLETED_LATE`
- `TASK_ASSIGNED`
- `NOTIFICATION`
- `ACHIEVEMENT_UNLOCKED`
- `ERROR`

Ngoài SFX, app có BGM loop (`bgm_lofi`) khi bật âm thanh.

### 10.5 Hiệu ứng thành công và thành tựu

#### Luồng hoạt động

Khi có sự kiện thành công, hệ thống kích hoạt đồng thời phản hồi âm thanh, hiệu ứng hình ảnh và popup thành tựu theo context.

## a) Hiệu ứng khi hoàn thành task

1. Khi user hoàn thành task (cá nhân/nhóm), ViewModel phát sound event theo ngữ cảnh:
   - Đúng hạn -> âm thanh tích cực.
   - Trễ hạn -> âm thanh khác.
2. Ở một số màn hình có overlay Konfetti để tăng phản hồi thị giác.

## b) Hiệu ứng khi mở khóa thành tựu

1. ViewModel đặt `achievementUnlocked`.
2. UI hiển thị `AchievementUnlockedDialog`.
3. Dialog có:
   - Animation scale.
   - Konfetti.
   - Âm thanh `ACHIEVEMENT_UNLOCKED`.

### 10.6 Dữ liệu được lưu để giữ cấu hình sau khi thoát app

#### Luồng hoạt động

Các giá trị theme/sound được persist ở DataStore, và tự động nạp lại khi app khởi động để giữ trải nghiệm nhất quán.

| Cấu hình | Nơi lưu | Key |
|---|---|---|
| Theme sáng/tối | DataStore `theme_prefs` | `dark_mode` |
| Bật/tắt âm thanh | DataStore `sound_prefs` | `sound_enabled` |
| Âm lượng | DataStore `sound_prefs` | `sound_volume` |

Khi mở lại app:

1. ViewModel đọc lại DataStore.
2. `MainActivity` apply sang `SyncTaskTheme` và `AppSoundPlayer`.
3. Người dùng giữ nguyên trạng thái đã chỉnh trước đó.

### 10.7 Trường hợp lỗi và edge cases

#### Luồng hoạt động

Với tình huống biên (giá trị ngoài ngưỡng, init muộn, recompose nhiều), hệ thống dùng clamp và guard để duy trì hành vi ổn định.

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Kéo slider quá biên | Giá trị vẫn hợp lệ | Đã clamp về 0..100 trong ViewModel |
| Tắt âm thanh nhưng vẫn hoàn thành task | Không phát SFX/Konfetti âm thanh | Đúng hành vi, chỉ còn phản hồi hình ảnh |
| Đổi theme liên tục | UI recompose nhiều lần | Hành vi bình thường, dữ liệu vẫn nhất quán |
| Chưa init sound engine | Có thể không phát âm lần đầu | `MainActivity` đã gọi `AppSoundPlayer.init(...)` khi khởi tạo |

## 11. Đăng xuất

### 11.1 Cách thao tác trên UI

#### Luồng hoạt động

Người dùng bấm logout từ TopBar, callback được kích hoạt và chuyển điều khiển sang luồng kết thúc phiên ở tầng Activity.

1. Ở `MainScreen`, bấm icon **Logout** trên TopBar.
2. Ứng dụng thực thi callback `onLogout`.
3. Người dùng bị đưa về màn hình `login`.

### 11.2 Luồng xử lý ở mức code

#### Luồng hoạt động

`signOut()` được gọi và back stack bị xóa trước khi điều hướng login, đảm bảo không thể quay lại màn chính bằng nút back.

1. `MainScreen` nhận hàm `onLogout` từ `MainActivity`.
2. Khi bấm icon logout:
   - `MainScreen` gọi `onLogout()`.
3. Trong `MainActivity`, `onLogout` thực hiện:
   - `FirebaseAuth.getInstance().signOut()`.
   - `rootNavController.navigate("login") { popUpTo(0) { inclusive = true } }`.

Ý nghĩa:

- `signOut()` xóa phiên xác thực hiện tại.
- `popUpTo(0)` xóa toàn bộ back stack để không thể bấm back quay lại màn hình bên trong sau khi logout.

### 11.3 Dữ liệu nào thay đổi khi đăng xuất

#### Luồng hoạt động

Logout chỉ thay đổi trạng thái phiên xác thực; dữ liệu cloud giữ nguyên, còn cấu hình local vẫn được lưu trên thiết bị.

| Thành phần | Trạng thái sau logout | Ghi chú |
|---|---|---|
| FirebaseAuth session | Bị xóa | Cần đăng nhập lại để thao tác nghiệp vụ |
| Dữ liệu trên Realtime DB | Không bị xóa | Logout không xóa dữ liệu user trên server |
| DataStore (theme/sound/onboarding) | Vẫn giữ nguyên | Đây là cấu hình cục bộ của thiết bị |
| UI state màn hiện tại | Bị thay bằng route `login` | Do điều hướng reset back stack |

### 11.4 Hành vi của các module sau khi logout

#### Luồng hoạt động

Các module phụ thuộc uid ngừng thao tác, và sẽ tái khởi tạo listener tương ứng khi người dùng đăng nhập lại.

1. Các màn hình nghiệp vụ không còn khả năng thao tác do thiếu `uid`.
2. Một số ViewModel khi không có user sẽ:
   - Trả trạng thái lỗi “Chưa đăng nhập”.
   - Hoặc không khởi tạo listener mới.
3. Khi đăng nhập lại:
   - Listener được thiết lập lại theo user mới.
   - Dữ liệu hiển thị theo đúng tài khoản đăng nhập.

### 11.5 Trường hợp liên quan đến bảo mật phiên

#### Luồng hoạt động

Hệ thống ưu tiên an toàn phiên bằng cách buộc về login ở các tình huống rủi ro xác thực hoặc session không hợp lệ.

| Tình huống | Hành vi hệ thống |
|---|---|
| User chưa verify email nhưng còn session cũ | `MainActivity` có nhánh phòng hờ signOut và đưa về login |
| Logout rồi bấm nút back hệ điều hành | Không quay lại màn chính do back stack đã xóa |
| Chuyển tài khoản khác sau logout | Được hỗ trợ, dữ liệu sync theo uid mới |

### 11.6 Edge cases và lưu ý vận hành

#### Luồng hoạt động

Trong các tình huống bất thường, app giữ nguyên nguyên tắc: kết thúc phiên rõ ràng, không làm mất dữ liệu cloud và hướng dẫn người dùng thao tác lại.

| Tình huống | Hiện tượng | Cách xử lý |
|---|---|---|
| Mạng yếu lúc bấm logout | Vẫn có thể về login vì logout là trạng thái local auth | Đăng nhập lại khi có mạng để đồng bộ dữ liệu |
| ViewModel cũ còn state tạm thời | Có thể thấy thoáng trạng thái cũ trước khi route chuyển | Hành vi ngắn hạn, route login sẽ thay thế |
| Người dùng tưởng logout sẽ xóa dữ liệu | Dữ liệu vẫn còn trên cloud | Cần ghi chú rõ: logout chỉ kết thúc phiên, không xóa tài khoản |

## 12. Danh sách toàn bộ thao tác click chính trong app

### 12.1 Checklist thao tác click theo từng khu vực

| Khu vực | Thành phần có thể bấm | Điều kiện trước khi bấm | Kết quả trực tiếp |
|---|---|---|---|
| Login/Register | Đăng nhập, đăng ký, quên mật khẩu, chuyển màn | Có route auth | Xác thực hoặc điều hướng giữa `login/register` |
| Welcome/Tutorial | Get Started, Next, Skip | Người dùng ở welcome/spotlight | Hoàn tất onboarding hoặc bỏ qua |
| TopBar | Chuông, âm lượng, theme, logout, tutorial | Đang ở MainScreen | Mở sheet/dialog hoặc đổi trạng thái app |
| Bottom Navigation | Personal, Group, Achievement, Dashboard | Đang ở MainScreen | Chuyển module tức thì |
| Personal | Nút `+`, card task, swipe, checkbox | Đang ở tab Personal | Tạo/sửa/xóa/đọc task cá nhân |
| Group | Tạo nhóm, nhập mã mời, mở nhóm | Đang ở tab Group | Tạo/join nhóm hoặc vào chi tiết |
| Group Detail | Tạo task nhóm, assign/claim, toggle, rời/xóa nhóm | Đã vào `GroupTaskScreen` | Điều phối công việc nhóm |
| Notification | Bấm item, mark read all | Mở NotificationBottomSheet | Cập nhật trạng thái đã đọc |
| Dashboard | Bộ lọc tuần/tháng | Đang ở tab Dashboard | Tái tính toán số liệu |

#### Luồng hoạt động 12.1

1. Người dùng chọn khu vực tương tác theo nhu cầu nghiệp vụ.
2. Hệ thống kiểm tra điều kiện trước khi cho phép thao tác.
3. Tác vụ được chuyển đến ViewModel/Repository tương ứng.
4. UI phản hồi ngay theo trạng thái thành công hoặc lỗi.

### 12.2 Checklist thao tác theo thứ tự người dùng mới

1. Mở app -> đăng ký hoặc đăng nhập.
2. Nếu lần đầu: hoàn tất welcome/tutorial.
3. Vào tab Personal -> tạo task đầu tiên.
4. Kiểm tra task nằm đúng ô Eisenhower.
5. Hoàn thành task để xem phản hồi (âm thanh/thành tựu/thông báo).
6. Vào tab Group -> tạo nhóm hoặc nhập mã mời để tham gia nhóm.
7. Vào chi tiết nhóm -> tạo task nhóm -> phân công thành viên.
8. Vào tab Notification -> kiểm tra thông báo mới và đánh dấu đã đọc.
9. Vào tab Dashboard -> chuyển WEEK/MONTH và quan sát thay đổi số liệu.
10. Đổi theme + chỉnh âm lượng để kiểm tra lưu cấu hình.
11. Đăng xuất và xác nhận quay về màn login.

#### Luồng hoạt động 12.2

1. Luồng được thực hiện từ onboarding đến tác vụ nâng cao theo thứ tự.
2. Mỗi bước tạo tiền đề dữ liệu cho bước kế tiếp.
3. Người dùng hoàn thành chuỗi thao tác có thể bao phủ gần toàn bộ chức năng chính.

### 12.3 Ma trận “bấm gì -> đổi dữ liệu gì”

| Hành động click | Node Firebase / DataStore thay đổi | Dạng thay đổi |
|---|---|---|
| Đăng ký | `/users/{uid}` | Tạo/cập nhật hồ sơ user |
| Tạo task cá nhân | `/tasks/{uid}/{taskId}` | Tạo bản ghi task |
| Toggle task cá nhân | `/tasks/{uid}/{taskId}/isCompleted` | Cập nhật trạng thái |
| Tạo nhóm | `/groups/{groupId}` | Tạo nhóm + members ban đầu |
| Join nhóm | `/groups/{groupId}/members` | Transaction thêm uid |
| Tạo task nhóm | `/groupTasks/{groupId}/{taskId}` | Tạo task nhóm |
| Assign/Claim task nhóm | `/groupTasks/{groupId}/{taskId}/assignedToId` | Cập nhật người nhận |
| Toggle task nhóm | `/groupTasks/{groupId}/{taskId}/isCompleted`, `/users/{uid}/groupTaskCount` | Transaction + đồng bộ biến đếm |
| Mark read thông báo | `/notifications/{uid}/{notificationId}/isRead` | Cập nhật đã đọc |
| Đổi theme | DataStore `theme_prefs.dark_mode` | Lưu cấu hình giao diện |
| Bật/tắt âm thanh | DataStore `sound_prefs.sound_enabled` | Lưu cấu hình âm thanh |
| Đổi âm lượng | DataStore `sound_prefs.sound_volume` | Lưu cấu hình âm lượng |
| Logout | FirebaseAuth session | Kết thúc phiên đăng nhập |

#### Luồng hoạt động 12.3

1. Mỗi thao tác click phát sinh một hành động ghi dữ liệu rõ ràng.
2. Dữ liệu được ghi vào Firebase hoặc DataStore tùy bản chất nghiệp vụ.
3. Listener và StateFlow cập nhật giao diện đồng bộ theo dữ liệu mới.

### 12.4 Ma trận “bấm gì -> feedback gì”

| Hành động | Feedback UI | Feedback hệ thống |
|---|---|---|
| Tạo task | Task xuất hiện ngay trong list | Có thể phát âm thanh tạo task |
| Hoàn thành task | Trạng thái card đổi, có thể có confetti | Thông báo nội bộ + check thành tựu |
| Join nhóm thành công | Nhóm xuất hiện trong danh sách | Có thể tạo thông báo cho owner |
| Assign task nhóm | Assignee hiển thị mới | In-app notification + push (nếu có token) |
| Mark read | Màu item đổi sang đã đọc | Badge chưa đọc giảm |
| Đổi theme | Toàn app đổi sáng/tối tức thì | Trạng thái được lưu DataStore |
| Chỉnh âm lượng | Âm thanh thay đổi mức phát | Trạng thái được lưu DataStore |
| Logout | Về login, mất quyền truy cập màn chính | Phiên auth bị xóa |

#### Luồng hoạt động 12.4

1. Sau mỗi thao tác, hệ thống trả phản hồi trực quan ngay tại UI.
2. Đồng thời phát sinh phản hồi hệ thống như notification, âm thanh hoặc cập nhật session.
3. Người dùng dựa vào phản hồi để xác nhận thao tác đã thành công.

### 12.5 Kiểm tra nhanh sau mỗi thao tác quan trọng (UAT mini)

| Bước kiểm tra | Điều kiện đạt |
|---|---|
| Tạo task cá nhân | Có bản ghi mới trong list và đúng ô Eisenhower |
| Join nhóm | Nhóm xuất hiện ở tab Group sau realtime sync |
| Assign task nhóm | Người nhận thấy thông báo mới |
| Mark all read | Badge về 0, nút “đã đọc tất cả” ẩn |
| Đổi theme | Đóng/mở app vẫn giữ theme đã chọn |
| Đổi âm lượng | Đóng/mở app vẫn giữ mức âm lượng |
| Logout | Không thể back về màn chính |

#### Luồng hoạt động 12.5

1. Thực hiện từng bước kiểm tra theo thứ tự bảng UAT.
2. Đối chiếu kết quả thực tế với điều kiện đạt.
3. Nếu một bước không đạt, truy ngược module liên quan để khoanh vùng lỗi.

## Kết luận

SyncTask cung cấp gần như đầy đủ các tương tác cần thiết cho một ứng dụng quản lý công việc cá nhân + nhóm theo thời gian thực. Từ góc nhìn người dùng, mọi thao tác chính đều có phản hồi trực quan (UI state), phản hồi hệ thống (notification), và phản hồi trải nghiệm (âm thanh/hiệu ứng/thành tựu).
