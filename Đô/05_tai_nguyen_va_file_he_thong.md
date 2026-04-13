# 05 - Tài nguyên, test, cache và file hệ thống

## Mục tiêu của file này

Phần này giải thích những file không trực tiếp chạy logic nghiệp vụ nhưng lại rất quan trọng: ảnh, âm thanh, icon, test mẫu, cấu hình IDE, cache build, và các file hệ thống sinh tự động.

## Tài nguyên giao diện trong `res/`

### [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)

Đây là nơi chứa các đoạn chữ dùng chung trong app.

Hiện file này mới có tên app, nhưng về nguyên tắc nó là nơi gom toàn bộ text để tái sử dụng.

### [app/src/main/res/values/colors.xml](../app/src/main/res/values/colors.xml)

Đây là bảng màu của app.

Các màu ở đây giúp app có nhận diện riêng, không bị mỗi màn một màu lung tung.

### [app/src/main/res/values/themes.xml](../app/src/main/res/values/themes.xml)

Theme sáng ở cấp XML.

Nó kết hợp với theme Compose để toàn app có một phong cách chung.

### [app/src/main/res/values-night/themes.xml](../app/src/main/res/values-night/themes.xml)

Theme tối ở cấp XML.

File này giúp app trông ổn hơn khi máy đang để dark mode.

### [app/src/main/res/xml/backup_rules.xml](../app/src/main/res/xml/backup_rules.xml)

File mẫu cho backup dữ liệu.

Nó nói dữ liệu nào nên được đưa vào bản sao lưu của Android.

### [app/src/main/res/xml/data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml)

File mẫu cho quy tắc trích xuất dữ liệu.

Nó liên quan đến chuyện Android backup và restore dữ liệu ra sao.

## Hình ảnh tĩnh

### [app/src/main/res/drawable/](../app/src/main/res/drawable)

Đây là nơi chứa ảnh và vector dùng trong app.

Các file ở đây giúp giao diện dễ hiểu hơn, đỡ khô hơn. Ví dụ:

- ảnh loading,
- ảnh empty state,
- ảnh chào mừng,
- ảnh tutorial,
- ảnh xác nhận xóa,
- icon thành tựu,
- icon app.

### [app/src/main/res/mipmap-\*/](../app/src/main/res)

Đây là icon launcher ở nhiều độ phân giải.

Vì mỗi máy hiển thị icon khác nhau nên app phải chuẩn bị nhiều bản để icon luôn nét.

## File âm thanh và animation

### [app/src/main/res/raw/](../app/src/main/res/raw)

Đây là nơi chứa âm thanh và file animation JSON.

Nhóm âm thanh có:

- nhạc nền,
- âm click,
- âm báo,
- âm thành công,
- âm lỗi,
- âm pháo hoa,
- âm thành tựu,
- âm thông báo.

Nhóm animation có:

- file Lottie cho login,
- file Lottie cho register.

Những file này không phải code, nhưng chúng làm app có cảm xúc hơn rất nhiều.

## File test

### [app/src/test/java/com/example/synctask/ExampleUnitTest.java](../app/src/test/java/com/example/synctask/ExampleUnitTest.java)

Đây là unit test mẫu.

Nội dung của nó chỉ là ví dụ đơn giản, chưa phải test thật của project.

### [app/src/androidTest/java/com/example/synctask/ExampleInstrumentedTest.java](../app/src/androidTest/java/com/example/synctask/ExampleInstrumentedTest.java)

Đây là instrumented test mẫu chạy trên emulator hoặc thiết bị thật.

Nó chỉ kiểm tra một thứ cơ bản là package name của app.

Lưu ý nhỏ: package trong test đang là `com.example.synctask`, hơi lệch với package chính của project.

## File cấu hình IDE và editor

### [.idea/](../.idea)

Đây là thư mục cấu hình của Android Studio / IntelliJ.

Bên trong thường có:

- file run configuration,
- file compiler,
- file gradle,
- file inspection,
- file misc,
- file vcs.

Những file này phục vụ IDE, không phải phần app chạy trên máy người dùng.

### [.vscode/settings.json](../.vscode/settings.json)

Đây là file cấu hình của VS Code.

Nó hiện chủ yếu dùng để tự động cập nhật build configuration.

### [.gradle/](../.gradle)

Đây là cache của Gradle.

Mục đích của nó là giúp build nhanh hơn, chứ không phải mã nguồn.

### [.kotlin/](../.kotlin)

Cache của Kotlin compiler.

### [app/build/](../app/build)

Đây là thư mục kết quả build của module `app`.

Bên trong có rất nhiều thứ sinh tự động như:

- source được generate,
- intermediate files,
- class đã compile,
- manifest đã merge,
- báo cáo build,
- test result,
- dex,
- APK staging.

Nói ngắn gọn: đây là nơi chứa “rác có ích” của quá trình build, không phải mã nguồn chính.

### [app/src/main/build/](../app/src/main/build)

Nếu xuất hiện thì đây cũng là output sinh tự động.

## File quan trọng nhưng chưa có logic

### [app/src/main/java/com/phuc/synctask/ui/main/git_main.kt](../app/src/main/java/com/phuc/synctask/ui/main/git_main.kt)

File này hiện đang trống.

Nó có mặt trong project, nhưng chưa chứa chức năng thật.

## Kết luận phần tài nguyên

Nhóm file này không trực tiếp tạo ra nghiệp vụ, nhưng nó làm app đẹp hơn, có cảm xúc hơn, build ổn hơn, và dễ dùng hơn. Với SyncTask, đây là phần nền rất quan trọng dù ít được để ý.
