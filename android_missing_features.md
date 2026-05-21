# 📱 Báo Cáo Phân Tích: Các Tính Năng Còn Thiếu Trên Android (So Với Web)

Dựa trên việc kiểm tra toàn bộ source code của ứng dụng Android (`d:\project\Drive\android`) và đối chiếu với danh sách tính năng trên Web, dưới đây là chi tiết các tính năng **đang bị thiếu hoặc chưa hoàn thiện** trên phiên bản di động.

---

## 1. 📁 Quản Lý File (File Manager)
*Fragment: `FilesFragment.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết lỗi / Thiếu sót trên Android |
|---|---|---|
| **Tìm kiếm file (Search)** | ❌ Thiếu | Không có thanh tìm kiếm, không có logic filter list file theo tên. |
| **Sắp xếp file (Sort)** | ❌ Thiếu | Không có menu sắp xếp (theo tên, ngày, kích thước). |
| **Upload nhiều file cùng lúc** | ❌ Thiếu | Intent file picker chỉ cho phép chọn **1 file** (`EXTRA_ALLOW_MULTIPLE` không được sử dụng). |
| **Thanh tiến trình Upload (Progress)** | ❌ Thiếu | Chỉ hiện Toast "Đang tải lên...", không có progress bar chi tiết (% tải lên) như bản Web. |
| **Đổi tên (Rename)** | ❌ Thiếu (Giống Web) | Hiển thị thông báo "Tính năng sẽ được bổ sung sớm". Cả web và Android đều chưa có. |
| **Hiển thị dung lượng file / ngày tạo** | ⚠️ Cần kiểm tra UI | Adapter có thể chưa hiển thị chi tiết như web (tùy thuộc vào layout item). |

---

## 2. ⚙️ Cài Đặt Tài Khoản (Profile / Account Settings)
*Fragment: `ProfileFragment.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết lỗi / Thiếu sót trên Android |
|---|---|---|
| **Cập nhật Avatar** | ❌ Thiếu | Không có tính năng chọn ảnh từ thư viện, crop và chuyển sang Base64 để lưu. |
| **Đổi Tên hiển thị (FullName)** | ❌ Thiếu | Không có form / API để chỉnh sửa tên. |
| **Đổi Username & Email** | ❌ Thiếu | Không có form chỉnh sửa. |
| **Đổi Mật khẩu** | ❌ Thiếu | Nút "Đổi mật khẩu" đang hiển thị Toast TODO: "Tính năng... sẽ được bổ sung sớm". |
| **API `PUT /api/users/profile`** | ❌ Thiếu | Backend có API này nhưng file `ApiService.java` của Android chưa khai báo và chưa sử dụng. |

---

## 3. 🔔 Thông Báo (Notifications)
*Fragment: `NotificationsFragment.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết lỗi / Thiếu sót trên Android |
|---|---|---|
| **Real-time Notifications (SSE)** | ❌ Thiếu | Android hiện tại chỉ dùng cơ chế Pull-to-refresh. Không có kết nối EventSource (SSE) để nhận thông báo real-time. |
| **Đánh dấu đã đọc (Từng cái)** | ❌ Thiếu API | Không có API `PUT /api/notifications/{id}/read` trong `ApiService.java`. |
| **Xóa thông báo** | ❌ Thiếu API | Không có API `DELETE /api/notifications/{id}`. |
| **Đánh dấu đọc tất cả (Mark all read)** | ⚠️ Chưa nối API | Code đang comment TODO chờ backend cấp API (dù backend bản chất bắt client lặp từng ID). |

---

## 4. 🛡️ Admin Dashboard (Quản trị viên)
*Packages / Code*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết lỗi / Thiếu sót trên Android |
|---|---|---|
| **Thống kê tổng quan** | ❌ Thiếu hoàn toàn | Không có bất kỳ Activity/Fragment nào dành cho Admin. |
| **Quản lý Users (Role, Tier, Xóa)** | ❌ Thiếu hoàn toàn | Không có giao diện danh sách người dùng. |
| **Duyệt/Từ chối Upgrade Requests** | ❌ Thiếu hoàn toàn | Admin không thể duyệt yêu cầu nâng cấp Premium từ App. |
| **Admin SSE Notifications** | ❌ Thiếu hoàn toàn | Không nhận được thông báo khi có người xin nâng cấp. |

---

## 5. 🗑️ Thùng Rác (Trash)
*Fragment: `TrashFragment.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết lỗi / Thiếu sót trên Android |
|---|---|---|
| **Bộ đếm thời gian (Days left)** | ⚠️ Chưa rõ UI | Khả năng cao chưa tính toán hiển thị "Còn X ngày" trước khi xóa vĩnh viễn như Web. |
| Dọn sạch, Khôi phục, Xóa vĩnh viễn | ✅ Đầy đủ | Đã sử dụng thao tác vuốt (Swipe) rất tốt. |

---

## 💡 Tổng Kết Hành Động Cần Làm Cho Android:

1. **Ưu tiên 1 (Core File Features):** Thêm chức năng **Upload nhiều file** (thêm `EXTRA_ALLOW_MULTIPLE` vào intent) và hiển thị thanh **Progress Bar** khi upload. Thêm chức năng **Tìm kiếm (Search)** và **Sắp xếp (Sort)** vào `FilesFragment`.
2. **Ưu tiên 2 (Account Management):** Bổ sung màn hình Edit Profile (cho phép đổi Avatar Base64, Tên, Username, Email, Mật khẩu) và khai báo API `PUT /api/users/profile`.
3. **Ưu tiên 3 (Notifications):** Tích hợp thư viện `okhttp-sse` (hoặc tương tự) để lắng nghe sự kiện Real-time. Bổ sung API đánh dấu đã đọc/xóa.
4. **Ưu tiên 4 (Admin):** Nếu yêu cầu app Android cũng phải có chức năng Admin, cần xây dựng nguyên một luồng giao diện mới (Admin Dashboard, User List, Request List) khi login với tài khoản có `role = ADMIN`.
