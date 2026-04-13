# 02 - Công nghệ, cấu hình và kiến trúc

## Mục tiêu của file này

Phần này nói về “bộ xương” của app: app được build thế nào, lấy thư viện ở đâu, nói chuyện với Firebase ra sao, và theme chạy như thế nào. Nói ngắn gọn, đây là phần nền móng trước khi đi vào nội dung màn hình hay dữ liệu.

## Các file cấu hình gốc

### [build.gradle.kts](../build.gradle.kts)

Đây là file build cấp cao nhất của project. Nó không chứa chức năng của app, mà chỉ nói cho Gradle biết project sẽ dùng những plugin nào.

Trong file này có các phần chính:

- plugin Android Application,
- plugin Kotlin Android,
- Compose compiler,
- Google Services cho Firebase.

Nếu ví project như một ngôi nhà thì file này giống như bản chọn kiểu xây: không xây phòng nào cụ thể nhưng quyết định cả nhà sẽ được dựng theo phong cách gì.

### [settings.gradle.kts](../settings.gradle.kts)

File này nói project có những module nào và tải thư viện từ đâu.

Vai trò của nó khá đơn giản:

- khai báo repository,
- bật plugin quản lý toolchain,
- include module `:app`.

Nói theo kiểu đời thường: đây là bản đồ tổng của project.

### [gradle.properties](../gradle.properties)

Đây là nơi chỉnh hành vi build cho Gradle và Android.

Các thiết lập ở đây giúp:

- Gradle có thêm RAM,
- dùng AndroidX,
- build ổn định hơn,
- tránh một số lỗi lặt vặt khi project lớn.

Nó không phải thứ người dùng nhìn thấy, nhưng nó ảnh hưởng khá mạnh đến việc build có mượt hay không.

### [local.properties](../local.properties)

File này chứa đường dẫn Android SDK trên máy đang phát triển.

Nó chỉ hợp với máy cá nhân, không nên đưa lên Git vì máy người khác sẽ không có cùng đường dẫn.

### [build_output.txt](../build_output.txt)

Đây là file log build.

Nó dùng để:

- xem build có thành công không,
- dò lỗi khi build hỏng,
- xem lại quá trình build trước đó.

## Gradle wrapper và JDK

### [gradle/wrapper/gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties)

File này cố định phiên bản Gradle mà project sẽ dùng. Trong project này là Gradle 9.3.1.

Ý nghĩa rất thực tế:

- ai clone project cũng build cùng một bản Gradle,
- đỡ cảnh “máy tôi chạy, máy bạn không chạy”.

### [gradle/gradle-daemon-jvm.properties](../gradle/gradle-daemon-jvm.properties)

File này chỉ cho Gradle biết nên dùng JDK nào. Project đang trỏ tới JDK 21.

Điểm lợi là build sẽ ổn định hơn và ít phụ thuộc vào JDK cài rời trên máy.

## Version catalog

### [gradle/libs.versions.toml](../gradle/libs.versions.toml)

Đây là bảng quản lý version của các thư viện.

Nó chia làm 3 nhóm:

- `versions`: ghi phiên bản,
- `libraries`: ghi thư viện,
- `plugins`: ghi plugin.

Lợi ích của cách làm này là:

- dễ tìm version,
- dễ nâng cấp,
- không phải ghi version rải rác ở nhiều nơi.

## Module app

### [app/build.gradle.kts](../app/build.gradle.kts)

Đây là file build quan trọng nhất của app.

Nó quyết định:

- app tên gì,
- compile SDK bao nhiêu,
- min SDK và target SDK là gì,
- có dùng Compose hay không,
- có dùng Firebase, Room, DataStore, Lottie, Gson, OkHttp hay không,
- dùng Java 11,
- dùng kapt để sinh code cho Room.

Nếu nói đơn giản, file này là nơi khai báo “app có những món đồ nghề nào”.

### [app/google-services.json](../app/google-services.json)

Đây là file kết nối app với Firebase.

Trong file này có:

- project id,
- project number,
- API key,
- app id,
- OAuth client,
- certificate hash.

Thiếu file này hoặc cấu hình sai thì app sẽ không nói chuyện đúng với Firebase.

### [app/proguard-rules.pro](../app/proguard-rules.pro)

File này là nơi để khai báo luật cho ProGuard/R8 khi build bản release.

Hiện tại nó gần như template mặc định, nghĩa là app chưa cần tùy biến quá nhiều ở chỗ này.

## Manifest

### [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

Đây là file khai báo trung tâm của Android app.

Nó cho hệ thống biết:

- app cần quyền gì,
- màn nào mở đầu,
- service nào chạy nền,
- app dùng theme nào.

Trong project này, các phần quan trọng là:

- `INTERNET`: để gọi mạng,
- `READ_EXTERNAL_STORAGE` và `WRITE_EXTERNAL_STORAGE`: để tương thích với Android cũ hơn,
- `MainActivity`: màn khởi động,
- `SyncTaskMessagingService`: nhận Firebase Messaging.

Nói dễ hiểu: đây là danh sách chính thức những gì app được phép làm và những gì hệ thống cần biết.

## Theme hệ thống

### [app/src/main/res/values/themes.xml](../app/src/main/res/values/themes.xml)

Theme sáng ở cấp XML.

Nó định nghĩa:

- khung Material3,
- màu primary,
- màu thanh trạng thái,
- màu thanh điều hướng.

### [app/src/main/res/values-night/themes.xml](../app/src/main/res/values-night/themes.xml)

Theme tối ở cấp XML.

Phần này chỉ khác chủ yếu ở việc màu nền và thanh điều hướng được chỉnh cho dark mode.

### [app/src/main/java/com/phuc/synctask/ui/theme/Theme.kt](../app/src/main/java/com/phuc/synctask/ui/theme/Theme.kt)

Đây là theme Compose thật sự mà app dùng khi vẽ giao diện.

Nó làm 3 việc chính:

- tạo bảng màu sáng,
- tạo bảng màu tối,
- chuyển màu mượt khi đổi theme.

Vì có animation chuyển màu nên app nhìn mềm hơn, không bị đổi cái rụp.

## Màu và chuỗi chữ

### [app/src/main/res/values/colors.xml](../app/src/main/res/values/colors.xml)

File chứa màu thương hiệu của app.

Các màu này được dùng để tạo cảm giác đồng nhất trong toolbar, tab, background, và các nút quan trọng.

### [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)

File này chứa chuỗi văn bản dùng chung.

Hiện nó có tên app là `SyncTask`.

## Backup rules

### [app/src/main/res/xml/backup_rules.xml](../app/src/main/res/xml/backup_rules.xml)

Đây là file mẫu để khai báo dữ liệu nào nên được backup.

### [app/src/main/res/xml/data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml)

Đây là file mẫu để khai báo quy tắc trích xuất dữ liệu khi backup hoặc chuyển máy.

Cả hai file này hiện chưa có nhiều cấu hình thật sự, nhưng vẫn là phần cần có để Android hiểu app nên xử lý dữ liệu ra sao.

## Tài nguyên hình và âm thanh

### [app/src/main/res/drawable/](../app/src/main/res/drawable)

Thư mục này chứa hình ảnh và icon dùng trong giao diện.

Các file ở đây giúp app không bị quá khô cứng. Ví dụ:

- ảnh loading,
- ảnh empty state,
- ảnh chào mừng,
- ảnh tutorial,
- ảnh xác nhận xóa,
- icon launcher.

### [app/src/main/res/mipmap-\*/](../app/src/main/res)

Đây là các bản icon launcher theo nhiều độ phân giải.

Lý do phải có nhiều bản là vì mỗi thiết bị sẽ hiển thị icon theo kích thước khác nhau. Có nhiều bản thì icon mới nét.

### [app/src/main/res/raw/](../app/src/main/res/raw)

Chứa âm thanh và animation JSON.

Có 2 nhóm lớn:

- âm thanh: click, ding, pop, yippee, wow, wasted, vine_boom, firework, achievement, bgm_lofi,
- animation: lottie_login.json và lottie_register.json.

## Kết luận phần kỹ thuật

Nếu chỉ nhìn ở mức cấu hình, SyncTask là một app Android hiện đại, build bằng Gradle, chạy với Kotlin 2.2.10, Compose, Firebase, Room, DataStore, và JDK 21. Toàn bộ phần cấu hình đã được gom lại để app vừa có khả năng phát triển tiếp, vừa có thể chạy ổn định trên máy người dùng.
