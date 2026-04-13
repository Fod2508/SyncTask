# 01 - Tổng quan dự án SyncTask

## SyncTask là gì

SyncTask là một ứng dụng giúp người dùng ghi nhớ việc phải làm, sắp xếp việc theo mức độ quan trọng, và theo dõi tiến độ theo cách dễ nhìn hơn một cuốn sổ bình thường.

Người dùng có thể dùng app để:

- ghi việc cá nhân,
- tạo nhóm và làm việc chung,
- giao việc cho người khác,
- theo dõi ai đang làm gì,
- nhận thông báo khi có thay đổi,
- nghe âm thanh khi có thao tác quan trọng,
- xem thống kê công việc,
- nhận huy hiệu khi làm tốt.

Nói đơn giản, SyncTask là một app quản lý việc có thêm phần “vui” và “có động lực”, chứ không chỉ là nơi lưu danh sách công việc.

## App này giải quyết chuyện gì

Nhiều người dùng xong app ghi chú nhưng vẫn thấy rối. Lý do thường là việc chỉ được liệt kê ra, chứ không được chia theo cách dễ hiểu. SyncTask giải quyết bằng cách đi cùng lúc nhiều hướng:

- **việc cá nhân**: không quên task của mình,
- **việc nhóm**: biết ai làm gì,
- **động lực làm việc**: có âm thanh, hiệu ứng, huy hiệu,
- **nhìn tổng quan**: có dashboard và biểu đồ để xem tiến độ.

Điểm hay của app không phải chỉ nằm ở một tính năng to, mà là cách nhiều tính năng nhỏ ghép lại thành một trải nghiệm trọn vẹn.

## App hợp với ai

SyncTask hợp với:

- người muốn quản lý việc mỗi ngày,
- nhóm nhỏ cần phân công và theo dõi công việc,
- người thích xem tiến độ rõ ràng,
- người thích app có phản hồi đẹp, có animation và hiệu ứng.

## Khi mở app thì chuyện gì xảy ra

Luồng sử dụng của app khá dễ đoán:

1. App kiểm tra người dùng đã đăng nhập chưa.
2. Nếu là lần đầu mở, app hiện màn chào mừng và hướng dẫn.
3. Nếu đã đăng nhập rồi, app đi thẳng vào màn chính.
4. Trong màn chính, người dùng chuyển qua lại giữa:
   - phần cá nhân,
   - phần nhóm,
   - phần thành tựu,
   - phần thống kê.

## Công nghệ chính trong project

Project này dùng nhiều công nghệ Android phổ biến nhưng được ghép lại khá khéo:

- **Kotlin**: ngôn ngữ viết app.
- **Jetpack Compose**: dựng giao diện bằng code.
- **Firebase Auth**: đăng nhập/đăng ký.
- **Firebase Realtime Database**: lưu dữ liệu cloud.
- **Firebase Cloud Messaging**: nhận thông báo đẩy.
- **Room**: lưu dữ liệu local trên máy.
- **DataStore**: nhớ thiết lập như theme và âm lượng.
- **Material 3**: bộ thiết kế giao diện hiện đại.
- **Lottie**: animation cho màn login/register.
- **Konfetti**: hiệu ứng pháo hoa.
- **OkHttp + Gson**: phục vụ request và xử lý JSON.

## Dự án được chia làm mấy lớp

Có thể hiểu project này theo 4 tầng:

### 1. Lớp giao diện

Đây là những gì người dùng thấy và chạm vào. Nó nằm trong `ui/`.

### 2. Lớp logic

Đây là nơi xử lý trạng thái, quyết định khi nào hiện gì, khi nào mở huy hiệu, khi nào phát âm thanh. Nó nằm trong `viewmodel/`.

### 3. Lớp dữ liệu

Đây là nơi đọc/ghi dữ liệu thật, gồm `repository/`, `data/`, và `model/`.

### 4. Lớp công cụ phụ

Đây là các phần hỗ trợ như âm thanh, thông báo, import/export, hoặc tính toán tiến độ.

## Luồng hoạt động dễ hiểu

Khi người dùng làm một việc gì đó, app thường chạy theo chuỗi này:

- Người dùng bấm một nút.
- Màn hình gửi yêu cầu cho `ViewModel`.
- `ViewModel` nhờ `Repository` hoặc helper xử lý tiếp.
- Repository đọc hoặc ghi dữ liệu vào Firebase/Room.
- Dữ liệu thay đổi xong thì `ViewModel` cập nhật trạng thái.
- Giao diện tự đổi theo trạng thái mới.

Nói bình dân hơn: UI chỉ lo hiển thị, còn việc nặng được đẩy xuống dưới để code đỡ rối.

## Cảm giác tổng thể của app

SyncTask không phải kiểu app chỉ có text và bảng danh sách. Nó được làm để người dùng cảm thấy:

- có màu sắc riêng,
- có phản hồi ngay khi thao tác,
- có âm thanh khi cần,
- có hiệu ứng vui khi hoàn thành việc,
- có hướng dẫn rõ ràng cho người mới.

## Đọc tiếp ở đâu

Nếu muốn hiểu sâu hơn, hãy mở các file:

- [02_cong_nghe_cau_hinh_va_kien_truc.md](02_cong_nghe_cau_hinh_va_kien_truc.md)
- [03_du_lieu_logic_va_backend.md](03_du_lieu_logic_va_backend.md)
- [04_giao_dien_va_trai_nghiem_nguoi_dung.md](04_giao_dien_va_trai_nghiem_nguoi_dung.md)
- [05_tai_nguyen_va_file_he_thong.md](05_tai_nguyen_va_file_he_thong.md)
