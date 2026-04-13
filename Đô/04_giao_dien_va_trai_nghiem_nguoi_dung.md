# 04 - Giao diện và trải nghiệm người dùng

## Mục tiêu của file này

Phần này nói về những gì người dùng thật sự nhìn thấy. Không chỉ là “màn nào có gì”, mà còn là “người dùng bấm ở đâu”, “chuyện gì xảy ra sau khi bấm”, và “app phản hồi ra sao”.

## Kiến trúc giao diện tổng thể

SyncTask dùng **Jetpack Compose** để vẽ giao diện bằng code. Nghĩa là mỗi màn hình là một hàm giao diện, không phải file XML kiểu cũ.

Luồng nhìn tổng thể của app khá giống nhau cho mọi người dùng:

1. Vào app.
2. Đăng nhập hoặc đăng ký.
3. Nếu là lần đầu thì xem màn chào mừng.
4. Vào màn chính.
5. Từ màn chính đi sang Cá nhân, Nhóm, Thành tựu, Dashboard.

## Màn khởi động và xác thực

### [app/src/main/java/com/phuc/synctask/MainActivity.kt](../app/src/main/java/com/phuc/synctask/MainActivity.kt)

Đây là điểm xuất phát của app.

Từ góc nhìn giao diện, file này giống như người đứng ở cửa và quyết định bạn sẽ đi vào cánh nào:

- đăng nhập,
- chào mừng,
- hoặc vào thẳng màn chính.

Nó cũng bọc toàn bộ app trong theme chung và dùng `NavHost` để điều hướng.

### [app/src/main/java/com/phuc/synctask/ui/auth/LoginScreen.kt](../app/src/main/java/com/phuc/synctask/ui/auth/LoginScreen.kt)

Đây là màn đăng nhập.

Bên trong có:

- ô email,
- ô mật khẩu,
- nút hiện/ẩn mật khẩu,
- nút quên mật khẩu,
- nút đăng nhập,
- liên kết sang màn đăng ký.

Điểm khiến màn này “dễ chịu” hơn là:

- có animation Lottie,
- có thông báo lỗi rõ ràng,
- có âm thanh khi login thành công hoặc lỗi,
- có hộp thoại khôi phục mật khẩu.

### [app/src/main/java/com/phuc/synctask/ui/auth/RegisterScreen.kt](../app/src/main/java/com/phuc/synctask/ui/auth/RegisterScreen.kt)

Đây là màn tạo tài khoản.

Người dùng sẽ nhập:

- họ tên,
- email,
- mật khẩu,
- xác nhận mật khẩu.

Cũng giống màn login, màn này có animation, có phản hồi lỗi, và có nút quay lại màn đăng nhập nếu người dùng đã có tài khoản.

### [app/src/main/java/com/phuc/synctask/ui/onboarding/WelcomeScreen.kt](../app/src/main/java/com/phuc/synctask/ui/onboarding/WelcomeScreen.kt)

Đây là màn chào mừng cho người mới.

Mục đích của nó rất rõ: chào người dùng, nói ngắn gọn app dùng để làm gì, rồi mời người dùng bấm “Bắt đầu ngay”.

Màn này đi theo hướng nhẹ nhàng, thoáng, dễ nhìn, không nhồi quá nhiều thứ.

### [app/src/main/java/com/phuc/synctask/ui/onboarding/SpotlightOverlay.kt](../app/src/main/java/com/phuc/synctask/ui/onboarding/SpotlightOverlay.kt)

Đây là lớp hướng dẫn từng bước.

Nó làm màn hình tối lại, chừa sáng một vùng đúng chỗ cần giải thích, rồi hiện card hướng dẫn lên.

Nếu ví cho dễ hiểu thì nó giống một người cầm đèn pin soi đúng chỗ cần xem và chỉ cho bạn biết nên bấm gì.

## Màn chính và điều hướng

### [app/src/main/java/com/phuc/synctask/ui/main/MainScreen.kt](../app/src/main/java/com/phuc/synctask/ui/main/MainScreen.kt)

Đây là trung tâm của app sau khi đăng nhập.

Màn này có rất nhiều vai trò:

- chào người dùng ở trên cùng,
- mở lại hướng dẫn,
- mở hộp thông báo,
- mở chỉnh âm lượng,
- đổi theme sáng/tối,
- đăng xuất,
- cho chuyển giữa các tab chính,
- mở các màn phụ như thêm task hay xem chi tiết.

Các tab chính ở thanh dưới gồm:

- Cá nhân,
- Nhóm,
- Thành tựu,
- Dashboard.

### [app/src/main/java/com/phuc/synctask/ui/navigation/Screen.kt](../app/src/main/java/com/phuc/synctask/ui/navigation/Screen.kt)

File này là danh sách tên các màn chính.

Nó giống như bảng chỉ đường để app biết tab nào dẫn tới đâu.

## Màn cá nhân

### [app/src/main/java/com/phuc/synctask/ui/home/HomeScreen.kt](../app/src/main/java/com/phuc/synctask/ui/home/HomeScreen.kt)

Đây là màn danh sách task cá nhân.

Người dùng sẽ thấy:

- top bar,
- nút đăng xuất,
- nút `+` để thêm task,
- danh sách task,
- task card có thể bấm,
- vuốt để xóa hoặc hoàn thành,
- confetti khi hoàn thành việc,
- dialog thành tựu.

Đây là màn người dùng sẽ quay lại nhiều nhất trong ngày.

### [app/src/main/java/com/phuc/synctask/ui/personal/PersonalTaskScreen.kt](../app/src/main/java/com/phuc/synctask/ui/personal/PersonalTaskScreen.kt)

Màn này giúp nhìn task cá nhân theo ma trận Eisenhower.

Phía trên có 3 số liệu nhanh:

- hôm nay,
- quá hạn,
- hoàn thành.

Bên dưới là 4 ô lớn:

- Làm ngay,
- Lên kế hoạch,
- Ủy quyền,
- Loại bỏ.

Màn này đặc biệt dễ hiểu vì mỗi ô có màu và icon riêng, người không chuyên vẫn nhìn ra ngay.

### [app/src/main/java/com/phuc/synctask/ui/personal/QuadrantDetailScreen.kt](../app/src/main/java/com/phuc/synctask/ui/personal/QuadrantDetailScreen.kt)

Khi người dùng bấm vào một ô, app mở màn này ra.

Màn này cho thấy:

- tiêu đề của ô đang xem,
- số task đang có,
- số task hoàn thành,
- số task quá hạn,
- danh sách task còn lại,
- danh sách task đã hoàn thành.

Ngoài ra còn có:

- dialog xóa có hoàn tác,
- confetti,
- dialog mở khóa thành tựu.

### [app/src/main/java/com/phuc/synctask/ui/personal/TaskListBottomSheet.kt](../app/src/main/java/com/phuc/synctask/ui/personal/TaskListBottomSheet.kt)

Đây là một cách xem task theo từng ô.

Nó hiển thị:

- tên ô,
- mô tả ô,
- số lượng task,
- danh sách task,
- checkbox hoàn thành,
- vuốt để xóa,
- deadline nếu có.

## Màn nhóm

### [app/src/main/java/com/phuc/synctask/ui/group/GroupListScreen.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupListScreen.kt)

Đây là màn danh sách nhóm.

Người dùng có thể:

- tạo nhóm mới,
- tham gia nhóm bằng mã mời,
- xem danh sách nhóm đang có,
- xem trạng thái trống nếu chưa có nhóm nào.

Hai hộp thoại chính ở đây là:

- `CreateGroupDialog`,
- `JoinGroupDialog`.

### [app/src/main/java/com/phuc/synctask/ui/group/GroupTaskScreen.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupTaskScreen.kt)

Đây là màn chi tiết của một nhóm.

Nó hiển thị:

- tên nhóm,
- tiến độ của từng thành viên,
- bảng hoạt động task,
- nút thêm task nhóm,
- hộp xác nhận rời/giải tán nhóm,
- hộp xóa task,
- confetti khi hoàn thành task.

Đây là màn nhiều thông tin nhất trong app vì nó vừa là màn xem, vừa là màn thao tác.

### [app/src/main/java/com/phuc/synctask/ui/group/GroupTaskChrome.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupTaskChrome.kt)

File này chứa phần “khung” của màn nhóm:

- thanh tiêu đề,
- nút thêm task,
- confetti.

Nó giúp màn nhóm có hình dáng đồng nhất, không bị mỗi nơi một kiểu.

### [app/src/main/java/com/phuc/synctask/ui/group/GroupTaskSections.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupTaskSections.kt)

File này chứa các phần nội dung nhỏ trong màn nhóm.

Có thể hiểu nó là các mảnh ghép như:

- vòng tiến độ thành viên,
- card hoạt động,
- deadline,
- trạng thái task.

### [app/src/main/java/com/phuc/synctask/ui/group/GroupTaskOverlays.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupTaskOverlays.kt)

Dù tên file là overlays, thực tế nó chứa:

- bottom sheet thêm task nhóm,
- bottom sheet xem chi tiết task,
- dialog rời nhóm hoặc giải tán nhóm.

### [app/src/main/java/com/phuc/synctask/ui/group/GroupTaskUiTokens.kt](../app/src/main/java/com/phuc/synctask/ui/group/GroupTaskUiTokens.kt)

Đây là file chứa màu và token giao diện riêng cho phần nhóm.

Tác dụng của nó là giữ cho màn nhóm có một phong cách riêng, nhìn vào là biết đây là phần nhóm chứ không lẫn với màn khác.

## Màn dashboard và thành tựu

### [app/src/main/java/com/phuc/synctask/ui/dashboard/DashboardScreen.kt](../app/src/main/java/com/phuc/synctask/ui/dashboard/DashboardScreen.kt)

Màn này hiển thị số liệu tổng hợp.

Nó có:

- bộ lọc tuần/tháng,
- card tổng quan,
- biểu đồ workload,
- donut chart Eisenhower,
- tiến độ dự án nhóm,
- danh sách task ưu tiên.

Mục tiêu của màn này là giúp người dùng nhìn tổng thể thay vì chỉ nhìn từng task rời rạc.

### [app/src/main/java/com/phuc/synctask/ui/achievement/AchievementScreen.kt](../app/src/main/java/com/phuc/synctask/ui/achievement/AchievementScreen.kt)

Màn thành tựu được chia thành 3 tab:

- Cá nhân,
- Nhóm,
- Đặc biệt.

Mỗi achievement có thể ở trạng thái đã mở hoặc chưa mở, và card sẽ đổi màu/độ mờ tương ứng.

## Thành phần dùng chung

### [app/src/main/java/com/phuc/synctask/ui/common/EmptyTaskState.kt](../app/src/main/java/com/phuc/synctask/ui/common/EmptyTaskState.kt)

Component này hiển thị khi chưa có dữ liệu.

Tác dụng:

- tránh màn hình trống khô khan,
- giúp người dùng hiểu rằng chưa có task chứ không phải app bị lỗi.

### [app/src/main/java/com/phuc/synctask/ui/common/DeleteConfirmationDialog.kt](../app/src/main/java/com/phuc/synctask/ui/common/DeleteConfirmationDialog.kt)

Hộp thoại xác nhận xóa.

Tác dụng:

- giảm xóa nhầm,
- làm thao tác nguy hiểm có bước xác nhận.

### [app/src/main/java/com/phuc/synctask/ui/common/AnimatedLoadingScreen.kt](../app/src/main/java/com/phuc/synctask/ui/common/AnimatedLoadingScreen.kt)

Màn loading có chuyển động.

Tác dụng:

- tạo cảm giác app đang xử lý,
- giảm cảm giác chờ đợi khô cứng.

### [app/src/main/java/com/phuc/synctask/ui/common/AchievementUnlockedDialog.kt](../app/src/main/java/com/phuc/synctask/ui/common/AchievementUnlockedDialog.kt)

Dialog ăn mừng thành tựu.

Tác dụng:

- phát pháo hoa,
- phát âm thanh,
- hiển thị tên và lời chúc mừng.

Đây là phần khiến app có cảm xúc tích cực hơn.

## Trải nghiệm tổng kết

Nếu nhìn toàn bộ giao diện, SyncTask có một triết lý khá rõ:

- màn đầu tiên phải dễ vào,
- màn chính phải rõ chức năng,
- mọi thao tác quan trọng phải có phản hồi ngay,
- mỗi hành vi thành công nên được thưởng bằng âm thanh hoặc hiệu ứng,
- mỗi dữ liệu quan trọng nên có đường đi rõ ràng để người dùng không bị lạc.
