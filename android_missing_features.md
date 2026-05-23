# 📱 Báo Cáo Phân Tích: Các Tính Năng Còn Thiếu Trên Android (So Với Web)

Dựa trên việc kiểm tra **toàn bộ source code** của ứng dụng Android (`d:\Ki2Nam3\Drive\android`) và đối chiếu với danh sách tính năng trên Web, dưới đây là chi tiết trạng thái từng tính năng.

> **Cập nhật lần cuối:** 2026-05-23 — Đã rà soát lại toàn bộ code Java, trạng thái không thay đổi.

---

## 1. 📁 Quản Lý File (File Manager)
*Fragment: `FilesFragment.java` | Adapter: `FileAdapter.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Tìm kiếm file (Search)** | ✅ Đã có | `etSearch` (EditText) với `TextWatcher` gọi `fileAdapter.filter(query)`. Filter theo tên file (case-insensitive) trên danh sách local. |
| **Sắp xếp file (Sort)** | ✅ Đã có | `btnSort` (ImageButton) hiển thị `PopupMenu` với 4 tùy chọn: Tên A-Z, Tên Z-A, Mới nhất, Kích thước lớn→bé. Folder luôn ưu tiên lên đầu. |
| **Upload nhiều file cùng lúc** | ✅ Đã có | `Intent.EXTRA_ALLOW_MULTIPLE = true`. Xử lý `ClipData` để lấy danh sách Uri. Upload tuần tự qua `uploadNextFile()` (sequential, không parallel). |
| **Thanh tiến trình Upload (Progress)** | ✅ Đã có | `ProgressRequestBody` wrap `RequestBody` với `CountingSink` theo dõi bytesWritten. Hiển thị `ProgressBar` + `TextView` ("Đang tải lên 1/3: filename"). UI progress bar cập nhật realtime trên MainThread. |
| **Breadcrumb / Điều hướng thư mục** | ✅ Đã có | `Stack<BreadcrumbItem>` lưu lịch sử. Breadcrumb hiển thị `🏠 Home › Folder1 › Folder2`. Hỗ trợ click vào bất kỳ cấp nào để quay về. Back button Android cũng được xử lý (`onBackPressed()`). |
| **Tạo thư mục mới** | ✅ Đã có | Dialog `dialog_create_folder` với `TextInputEditText`. Gọi `POST /api/files/folder`. |
| **Download file** | ✅ Đã có | Gọi `GET /api/files/{id}/download`, lưu vào `ExternalFilesDir/Downloads` qua `FileUtils.saveToDownloads()`. |
| **Xóa file (Soft Delete → Trash)** | ✅ Đã có | Confirm dialog + `DELETE /api/files/{id}`. |
| **Chia sẻ file (từ context menu)** | ✅ Đã có | Bottom Sheet gọi `SharedFragment.showShareSheet()` — hỗ trợ share qua email + tạo public link. |
| **Hiển thị dung lượng file / ngày tạo** | ✅ Đã có | `FileAdapter.bind()` hiển thị `getFormattedSize()` (KB/MB/GB) + `DateUtils.formatRelative(createdAt)`. Icon emoji theo MIME type + màu nền theo loại file. |
| **Đổi tên (Rename)** | ✅ Đã có | Nút "Đổi tên" trong Bottom Sheet mở dialog, gọi `PUT /api/files/{id}/rename` để cập nhật tên file/folder. |
| **FAB menu** | ✅ Đã có | `FloatingActionButton` → `AlertDialog` với 2 lựa chọn: Upload File / Tạo thư mục mới. |
| **Pull-to-refresh** | ✅ Đã có | `SwipeRefreshLayout` gọi `loadFiles()`. |
| **Empty state** | ✅ Đã có | `layout_empty` ẩn/hiện tùy danh sách rỗng hay không. |

---

## 2. ⚙️ Cài Đặt Tài Khoản (Profile / Account Settings)
*Fragment: `ProfileFragment.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Hiển thị thông tin profile** | ✅ Đã có | Gọi `GET /api/users/me`. Hiển thị: Avatar chữ cái đầu, fullName, email, role, tier badge (FREE/⭐ PREMIUM/👑 ADMIN). |
| **Storage Quota** | ✅ Đã có | `LinearProgressIndicator` hiển thị % đã dùng. Đổi màu khi ≥80% (cam) hoặc ≥90% (đỏ). Hiển thị "X MB đã dùng / Y GB". |
| **Yêu cầu nâng cấp Premium** | ✅ Đã có | Confirm dialog → `POST /api/users/upgrade-request`. Xử lý 409 (đã gửi trước đó). Ẩn nút nếu đã là Premium/Admin. |
| **Đăng xuất** | ✅ Đã có | Confirm dialog → `SessionManager.clearSession()` + `ApiClient.reset()` → redirect về `LoginActivity`. |
| **Cập nhật Avatar** | ✅ Đã có | Sử dụng ImagePicker, resize ảnh và upload dưới dạng chuỗi Base64 qua PUT. Hiển thị ảnh thay thế cho chữ cái đầu. |
| **Đổi Tên hiển thị (FullName)** | ✅ Đã có | Form dialog chỉnh sửa, gọi API lưu thông tin mới. |
| **Đổi Username & Email** | ✅ Đã có | Form dialog chỉnh sửa, có yêu cầu nhập mật khẩu hiện tại để xác nhận bảo mật. |
| **Đổi Mật khẩu** | ✅ Đã có | Form dialog đổi mật khẩu yêu cầu nhập mật khẩu cũ, mật khẩu mới và xác nhận. |
| **API `PUT /api/users/profile`** | ✅ Đã có | Đã tích hợp `ProfileUpdateRequest` vào `ApiService.java` và backend. |

---

## 3. 🔗 Chia Sẻ File (Shared)
*Fragment: `SharedFragment.java` | Adapter: `SharedItemAdapter.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Tab "Tôi đã chia sẻ"** | ✅ Đã có | `MaterialButtonToggleGroup` → `GET /api/files/list/shared-by-me`. Hiển thị fileName, targetEmail/Public link, expiresAt. Nút "Thu hồi". |
| **Tab "Chia sẻ với tôi"** | ✅ Đã có | `GET /api/files/list/shared-with-me`. Hiển thị fileName, email người chia sẻ, expiresAt. Nút "Tải xuống". |
| **Thu hồi quyền chia sẻ (Revoke)** | ✅ Đã có | Confirm dialog → `DELETE /api/files/revoke-share/{shareId}`. |
| **Download file được chia sẻ** | ✅ Đã có | `GET /api/files/shared/{token}` → `FileUtils.saveToDownloads()`. Xử lý 410 (link hết hạn). |
| **Chia sẻ qua email** | ✅ Đã có | Bottom Sheet `bottom_sheet_share`: nhập email (hỗ trợ nhiều email cách nhau bởi dấu phẩy), chọn thời hạn (7/30/Không giới hạn), gọi `POST /api/files/{id}/share`. |
| **Tạo Public Link** | ✅ Đã có | Gửi request với `emails: []` → backend tạo public token. Hiển thị link + nút "Copy" (ClipboardManager). |
| **Pull-to-refresh** | ✅ Đã có | `SwipeRefreshLayout` tải lại tab đang hiển thị. |
| **Hardcode URL trong public link** | ✅ Đã có | `shareLink = ApiClient.BASE_URL + "files/shared/" + token` — URL tự động tạo theo config `BASE_URL`. |

---

## 4. 🔔 Thông Báo (Notifications)
*Fragment: `NotificationsFragment.java` | Adapter: `NotificationAdapter.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Hiển thị danh sách thông báo** | ✅ Đã có | `GET /api/notifications`. Icon/màu khác nhau theo type (FILE_SHARED, UPGRADE_REQUEST, FILE_DELETED, SYSTEM, DOWNLOAD). Bold + dot xanh cho thông báo chưa đọc. |
| **Pull-to-refresh** | ✅ Đã có | `SwipeRefreshLayout`. |
| **Nút "Đọc hết"** | ✅ Đã có | Quét toàn bộ thông báo chưa đọc và gọi `PUT /api/notifications/{id}/read` để cập nhật về server. |
| **Real-time Notifications (SSE)** | ❌ Thiếu | Android chỉ dùng Pull-to-refresh. Không có kết nối EventSource (SSE) để nhận thông báo real-time như Web. |
| **Đánh dấu đã đọc (Từng thông báo)** | ✅ Đã có | Click vào thông báo sẽ gọi `PUT /api/notifications/{id}/read` và cập nhật UI. |
| **Xóa thông báo** | ✅ Đã có | Vuốt ngang (Swipe to delete) gọi API `DELETE /api/notifications/{id}`. |
| **Điều hướng từ thông báo** | ✅ Đã có | Dựa vào `targetUrl`, tự động điều hướng sang Tab Chia Sẻ (`/shared`) hoặc Tab Thùng Rác (`/trash`). |

---

## 5. 🗑️ Thùng Rác (Trash)
*Fragment: `TrashFragment.java` | Adapter: `TrashAdapter.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Hiển thị danh sách đã xóa** | ✅ Đã có | `GET /api/files/trash`. Hiển thị tên, emoji icon, thời gian xóa (`DateUtils.formatRelative`), kích thước file. |
| **Khôi phục file** | ✅ Đã có | Vuốt phải (nền xanh lá "↩ Khôi phục") → Confirm dialog → `POST /api/files/{id}/restore`. Hoàn tác animation nếu hủy. |
| **Xóa vĩnh viễn** | ✅ Đã có | Vuốt trái (nền đỏ "Xóa vĩnh viễn ✕") → Confirm dialog → `DELETE /api/files/{id}/permanent`. |
| **Dọn sạch thùng rác** | ✅ Đã có | Nút `btnEmptyTrash` → Confirm dialog (hiện số file) → `DELETE /api/files/trash/empty`. Disable nút khi danh sách rỗng. |
| **Pull-to-refresh** | ✅ Đã có | `SwipeRefreshLayout`. |
| **Bộ đếm "Còn X ngày" trước khi xóa tự động** | ✅ Đã có | Sử dụng thuật toán `getDaysUntilDeletion()` trong `DateUtils` để tính toán và hiển thị "Còn X ngày". |

---

## 6. 🔐 Xác Thực (Auth)
*Activity: `LoginActivity.java` | `RegisterActivity.java` | `SplashActivity.java`*

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Đăng nhập** | ✅ Đã có | `POST /api/auth/login`. Validate phía client (username, password required). Xử lý 401 (sai thông tin). Lưu JWT + username vào `SessionManager` (SharedPreferences). IME Action Done trên bàn phím. |
| **Đăng ký** | ✅ Đã có | `POST /api/auth/register`. 5 fields: fullName, email, username, password, confirmPassword. Validate đầy đủ (email format, username ≥3, password ≥8, confirm match). Hiện lỗi từ server. Chuyển về Login sau khi thành công (delay 1.5s). |
| **Splash Screen** | ✅ Đã có | Hiển thị 1.5s → kiểm tra `SessionManager.isLoggedIn()` → điều hướng Login/Main. |
| **Auto redirect khi token hết hạn** | ✅ Đã có | `AuthInterceptor` tự gắn Bearer token. Nếu nhận 401 → `clearSession()`. `ProfileFragment` kiểm tra 401 → gọi `redirectToLogin()`. |
| **Quên mật khẩu** | ❌ Thiếu (Giống Web) | Cả Web và Android đều chưa có tính năng quên mật khẩu / reset password. |

---

## 7. 🛡️ Admin Dashboard (Quản trị viên)

| Tính năng trên Web | Trạng thái trên Android | Chi tiết |
|---|---|---|
| **Thống kê tổng quan** | ❌ Thiếu hoàn toàn | Không có bất kỳ Activity/Fragment nào dành cho Admin. |
| **Quản lý Users (Role, Tier, Xóa)** | ❌ Thiếu hoàn toàn | Không có giao diện danh sách người dùng. |
| **Duyệt/Từ chối Upgrade Requests** | ❌ Thiếu hoàn toàn | Admin không thể duyệt yêu cầu nâng cấp Premium từ App. |
| **Admin SSE Notifications** | ❌ Thiếu hoàn toàn | Không nhận được thông báo khi có người xin nâng cấp. |

---

## 8. 🏗️ Hạ Tầng & Kỹ Thuật (Infrastructure)

| Hạng mục | Trạng thái | Chi tiết |
|---|---|---|
| **Network layer (Retrofit + OkHttp)** | ✅ Đầy đủ | Singleton `ApiClient` + `AuthInterceptor` (JWT auto-attach) + `HttpLoggingInterceptor` (debug). Timeout: connect 30s, read/write 60s. |
| **Session management** | ✅ Đầy đủ | `SessionManager` dùng `SharedPreferences`. Lưu token, username. Hỗ trợ `isLoggedIn()`, `clearSession()`. |
| **Bottom Navigation** | ✅ Đầy đủ | 5 tab: Files, Shared, Trash, Notifications, Profile. Fragment replace trong `fragment_container`. |
| **Back navigation** | ✅ Đầy đủ | `MainActivity.onBackPressed()` kiểm tra `FilesFragment` → nếu đang ở thư mục con thì back về cha. |
| **BASE_URL config** | ✅ Đầy đủ | Đã chuyển `BASE_URL` sang biến `public static final` dễ dàng thay đổi khi test. |

---

## 💡 Tổng Kết Hành Động Cần Làm Cho Android:

### ✅ Đã hoàn thành (không cần làm thêm):
- Tìm kiếm file (Search) + Sắp xếp (Sort)
- Upload nhiều file + Progress bar
- Breadcrumb navigation + Back button
- Tạo thư mục, Download, Xóa file, Đổi tên File/Folder
- Chia sẻ file (email + public link linh hoạt + thu hồi + download shared)
- Thùng rác (khôi phục, xóa vĩnh viễn, dọn sạch, đếm ngược ngày xóa, swipe gestures)
- Profile (hiển thị, Quota, Upgrade, Đổi Avatar/Tên/Username/Email/Mật khẩu, Đăng xuất)
- Auth (Login + Register + Splash + Auto-redirect)
- Thông báo (Hiển thị, Đọc, Xóa, Đọc tất cả, Điều hướng)
- Bottom Navigation + Session management

### 🔧 Cần sửa / hoàn thiện:
1. **Ưu tiên 1 — Real-time (SSE):** Tích hợp `okhttp-sse` để nhận thông báo real-time thay vì chỉ pull-to-refresh.
2. **Ưu tiên 2 — Admin Dashboard:** Xây dựng luồng Admin (Thống kê, Quản lý Users, Duyệt/Từ chối Upgrade Requests) nếu có yêu cầu.
