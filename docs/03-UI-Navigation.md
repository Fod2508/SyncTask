# UI and Navigation

## Route chính

```mermaid
flowchart TD
    A[MainActivity] --> B{Auth hợp lệ?}
    B -- No --> C[Login]
    C --> D[Register]
    C --> E[Main]
    B -- Yes --> F{First time?}
    F -- Yes --> G[Welcome + Tutorial]
    G --> E
    F -- No --> E
    E --> P[Tab Personal]
    E --> Q[Tab Group]
    E --> R[Tab Achievement]
    E --> S[Tab Dashboard]
```

## Màn hình chính

- Personal: xem ma trận Eisenhower.
- Group: xem danh sách nhóm và vào từng nhóm.
- Achievement: xem huy hiệu.
- Dashboard: xem thống kê.

## Thành phần UI nổi bật

- Top bar: user, notification, theme, sound, logout.
- Bottom bar: 4 tab.
- FAB: thêm task cá nhân.
- Bottom sheet: add task, notification list.
- Overlay tutorial: hướng dẫn từng vùng thao tác.
