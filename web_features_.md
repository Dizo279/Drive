# 📋 Phân Tích Tính Năng Web — File Manager

> **Mục đích:** So sánh những gì README liệt kê vs. những gì thực sự đã được implement trong code (Frontend Angular + Backend Spring Boot), làm cơ sở để phát triển ứng dụng Android có đầy đủ tính năng tương đương.

---

## 1. So Sánh README vs. Thực Tế Code

| Tính năng trong README | Trạng thái | Ghi chú |
|---|---|---|
| Upload/Tải lên (tối đa 10MB) | ✅ **Đã implement** | Upload đơn/nhiều file, drag & drop, progress bar |
| Quản lý file (xem, rename, delete) | ⚠️ **Thiếu Rename** | Xem ✅, Delete (soft) ✅, **Rename chưa có** |
| Restore từ thùng rác | ✅ **Đã implement** | Khôi phục đệ quy cả folder |
| Chia sẻ file (link an toàn) | ✅ **Đã implement** | Public link + Email cụ thể, có ngày hết hạn |
| Authentication (đăng ký/đăng nhập) | ✅ **Đã implement** | JWT 24h, validate chặt |
| Quota (giới hạn dung lượng) | ✅ **Đã implement** | FREE=1GB, PREMIUM=100GB |
| Notifications real-time | ✅ **Đã implement** | SSE stream, bell icon, đọc/xóa thông báo |
| Admin Dashboard | ✅ **Đã implement** | Quản lý users, upgrade requests, thống kê |
| Thùng rác tự động cleanup | ⚠️ **Chưa verify** | Backend có `@EnableScheduling` nhưng không thấy job cleanup file sau 30 ngày |

---

## 2. Danh Sách Đầy Đủ Tính Năng Đã Implement (Theo Module)

---

### 🔐 Module 1: Xác Thực (Auth)
**Route:** `/login`, `/register`  
**API:** `POST /api/auth/register`, `POST /api/auth/login`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 1.1 | **Đăng ký tài khoản** | Nhập: fullName, email, username, password (≥8 ký tự). Server validate trùng username/email (case-insensitive) |
| 1.2 | **Đăng nhập** | Đăng nhập bằng username + password. Nhận JWT token (24h). Lưu vào localStorage |
| 1.3 | **Đăng xuất** | Xóa JWT khỏi localStorage, redirect về `/login` |
| 1.4 | **Route Guard** | Tự động redirect nếu chưa đăng nhập (JWT interceptor tự đính token vào mọi request) |

---

### 📁 Module 2: Quản Lý File & Thư Mục
**Route:** `/files`  
**API:** `/api/files/*`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 2.1 | **Xem danh sách file/folder** | Hiển thị theo thư mục hiện tại (`parentId`). Folders luôn hiển thị trước files |
| 2.2 | **Duyệt cây thư mục (Breadcrumb)** | Click vào folder → vào trong. Breadcrumb navigation để quay lại. Hỗ trợ đa cấp thư mục |
| 2.3 | **Upload file** | Click chọn file hoặc **drag & drop** lên vùng nội dung. Upload nhiều file cùng lúc. Hiển thị progress bar tổng hợp. File lưu vào thư mục hiện tại đang xem |
| 2.4 | **Tạo thư mục mới** | Nhập tên thư mục qua dialog. Tạo trong thư mục hiện tại |
| 2.5 | **Tải về file** | Download file về máy (streaming từ server). Hỗ trợ tên file tiếng Việt (URL encode) |
| 2.6 | **Xóa (Soft Delete)** | Chuyển file/folder vào Thùng Rác. Xóa đệ quy toàn bộ nội dung folder |
| 2.7 | **Tìm kiếm file** | Tìm kiếm real-time theo tên file trong thư mục hiện tại. Highlight kết quả trùng khớp |
| 2.8 | **Sắp xếp** | Theo tên / kích thước / ngày tạo (mới nhất) |
| 2.9 | **Phân loại file** | Nhận diện theo MIME type: Image, Document (PDF/Word/Excel/PPT), Media (Video/Audio), Archive (ZIP/RAR), Other |
| 2.10 | **Hiển thị dung lượng đã dùng** | Thanh progress quota real-time. Hiển thị `X MB / 1 GB` (FREE) hoặc `/ 100 GB` (PREMIUM) |
| 2.11 | **Yêu cầu nâng cấp PREMIUM** | Nút "Nâng cấp" → gửi request đến Admin. Hiển thị trạng thái "Đang chờ duyệt" |
| 2.12 | **Xem thông tin file chi tiết** | API `/api/files/{id}` — metadata: tên, kích thước, loại, ngày tạo |

> ❌ **THIẾU:** Đổi tên file/folder (Rename) — README đề cập nhưng **chưa có API lẫn UI**

---

### 🗑️ Module 3: Thùng Rác (Trash)
**Route:** `/trash`  
**API:** `/api/files/trash`, `/api/files/{id}/restore`, `/api/files/{id}/permanent`, `/api/files/trash/empty`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 3.1 | **Xem danh sách trong Thùng Rác** | Chỉ hiển thị các item gốc (root), không lặp con |
| 3.2 | **Đếm ngày còn lại** | Hiển thị "còn X ngày" trước khi tự động xóa (tính từ `deletedAt + 30 ngày`) |
| 3.3 | **Khôi phục** | Restore file/folder về Drive. Nếu folder cha đang ở Trash → đưa về root (tránh "mồ côi") |
| 3.4 | **Xóa vĩnh viễn 1 item** | Xóa khỏi DB + xóa file vật lý trên disk. Đệ quy với folder |
| 3.5 | **Dọn sạch toàn bộ Trash** | "Empty Trash" — xóa vĩnh viễn tất cả items của user |

---

### 🔗 Module 4: Chia Sẻ File (Share)
**Route:** `/shared`  
**API:** `/api/files/{id}/share`, `/api/files/shared/{token}`, `/api/files/list/shared-by-me`, `/api/files/list/shared-with-me`, `/api/files/revoke-share/{shareId}`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 4.1 | **Tạo Public Link** | Tạo link download công khai (bất kỳ ai có link đều tải được). Tái sử dụng nếu đã tồn tại |
| 4.2 | **Chia sẻ qua Email** | Nhập email của người dùng khác trong hệ thống → họ nhận quyền truy cập. Hỗ trợ nhiều email (cách nhau bởi dấu phẩy) |
| 4.3 | **Đặt ngày hết hạn** | Tùy chọn `expireDays` cho cả public link và email share |
| 4.4 | **Copy link** | Copy link chia sẻ vào clipboard ngay trên giao diện |
| 4.5 | **Xem file đã chia sẻ BỞI TÔI** | Tab "Shared by me" — danh sách file, email người nhận, ngày hết hạn |
| 4.6 | **Xem file được chia sẻ VỚI TÔI** | Tab "Shared with me" — danh sách file người khác chia sẻ cho mình |
| 4.7 | **Thu hồi quyền truy cập** | Cả người chia sẻ lẫn người nhận đều có thể xóa share record |
| 4.8 | **Tải file từ Share Link** | Download từ token share (không cần đăng nhập với public link). Kiểm tra hết hạn |
| 4.9 | **Thông báo khi được share** | Khi ai đó share file qua email → người nhận nhận notification |

---

### 🔔 Module 5: Thông Báo (Notifications)
**Route:** Component `NotificationBell` tích hợp trên header  
**API:** `/api/notifications`, `/api/notifications/stream` (SSE), `/api/notifications/{id}/read`, `/api/notifications/{id}`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 5.1 | **Xem danh sách thông báo** | Dropdown bell icon. Hiển thị thông báo mới nhất trước |
| 5.2 | **Đếm số chưa đọc** | Badge đỏ hiện số lượng thông báo chưa đọc |
| 5.3 | **Đánh dấu đã đọc** | Click từng thông báo → mark as read |
| 5.4 | **Đánh dấu tất cả đã đọc** | Một click đánh dấu toàn bộ |
| 5.5 | **Xóa thông báo** | Xóa từng notification |
| 5.6 | **Real-time (SSE)** | Kết nối Server-Sent Events để nhận thông báo mới tức thì, không cần refresh |
| 5.7 | **Các loại thông báo** | `FILE_SHARED` (ai đó share file cho bạn), `UPGRADE_REQUEST` (user gửi yêu cầu nâng cấp — cho Admin) |

---

### ⚙️ Module 6: Cài Đặt Tài Khoản (Account Settings)
**Route:** `/settings`  
**API:** `GET /api/users/me`, `PUT /api/users/profile`, `GET /api/users/quota`, `POST /api/users/upgrade-request`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 6.1 | **Xem thông tin hồ sơ** | Hiển thị: fullName, username, email, tier (FREE/PREMIUM), avatar, quota đã dùng/tổng |
| 6.2 | **Cập nhật tên hiển thị** | Sửa fullName không cần mật khẩu |
| 6.3 | **Đổi Avatar** | Upload ảnh từ máy → preview ngay lập tức → lưu dưới dạng Base64 |
| 6.4 | **Đổi username** | Yêu cầu xác nhận mật khẩu hiện tại |
| 6.5 | **Đổi email** | Yêu cầu xác nhận mật khẩu hiện tại |
| 6.6 | **Đổi mật khẩu** | Nhập mật khẩu cũ → mật khẩu mới → xác nhận. Server mã hóa bcrypt |
| 6.7 | **Xem quota** | Hiển thị dung lượng đã dùng, tổng quota, phần trăm |
| 6.8 | **Yêu cầu nâng cấp PREMIUM** | Gửi request lên Admin. Kiểm tra nếu đã PREMIUM hoặc đã có pending request |

---

### 🛡️ Module 7: Admin Dashboard
**Route:** `/admin` (chỉ dành cho ROLE=ADMIN)  
**API:** `/api/admin/*`

| # | Tính năng | Mô tả chi tiết |
|---|---|---|
| 7.1 | **Thống kê tổng quan** | Tổng user, tổng admin, tổng dung lượng hệ thống đang dùng |
| 7.2 | **Danh sách toàn bộ user** | Bảng hiển thị: fullName, email, role, tier, quota |
| 7.3 | **Tìm kiếm user** | Tìm theo tên, email real-time |
| 7.4 | **Sắp xếp user** | Theo tên / email / role |
| 7.5 | **Chỉnh sửa Role** | Thay đổi role của user: USER ↔ ADMIN |
| 7.6 | **Chỉnh sửa Tier** | Thay đổi tier: FREE ↔ PREMIUM (tự động set quota tương ứng 1GB/100GB) |
| 7.7 | **Xóa User** | Xóa tài khoản người dùng khỏi hệ thống |
| 7.8 | **Danh sách yêu cầu nâng cấp** | Tab "Upgrade Requests" — danh sách yêu cầu đang chờ (PENDING) |
| 7.9 | **Phê duyệt yêu cầu** | Approve → user tự động lên PREMIUM 100GB |
| 7.10 | **Từ chối yêu cầu** | Reject yêu cầu nâng cấp |
| 7.11 | **Real-time thông báo (SSE)** | Admin nhận thông báo tức thì khi có user gửi upgrade request |

---

## 3. Tổng Hợp API Backend Đã Implement

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký | No |
| `POST` | `/api/auth/login` | Đăng nhập → JWT | No |
| `GET` | `/api/users/me` | Thông tin cá nhân | JWT |
| `PUT` | `/api/users/profile` | Cập nhật hồ sơ | JWT |
| `GET` | `/api/users/quota` | Thông tin quota | JWT |
| `POST` | `/api/users/upgrade-request` | Yêu cầu nâng cấp | JWT |
| `GET` | `/api/files` | Danh sách file theo parentId | JWT |
| `GET` | `/api/files/{id}` | Chi tiết file | JWT |
| `GET` | `/api/files/{id}/download` | Tải về | JWT |
| `POST` | `/api/files/upload` | Upload file (multipart) | JWT |
| `POST` | `/api/files/folder` | Tạo thư mục | JWT |
| `GET` | `/api/files/list/all` | Tất cả file của user | JWT |
| `DELETE` | `/api/files/{id}` | Xóa vào Trash | JWT |
| `GET` | `/api/files/trash` | Xem Trash | JWT |
| `POST` | `/api/files/{id}/restore` | Khôi phục từ Trash | JWT |
| `DELETE` | `/api/files/{id}/permanent` | Xóa vĩnh viễn | JWT |
| `DELETE` | `/api/files/trash/empty` | Dọn sạch Trash | JWT |
| `POST` | `/api/files/{id}/share` | Tạo share link / share qua email | JWT |
| `GET` | `/api/files/shared/{token}` | Download từ share token | No |
| `GET` | `/api/files/list/shared-by-me` | File tôi đã share | JWT |
| `GET` | `/api/files/list/shared-with-me` | File được share cho tôi | JWT |
| `DELETE` | `/api/files/revoke-share/{shareId}` | Thu hồi quyền share | JWT |
| `GET` | `/api/notifications` | Danh sách thông báo | JWT |
| `GET` | `/api/notifications/stream` | SSE stream real-time | JWT |
| `PUT` | `/api/notifications/{id}/read` | Đánh dấu đã đọc | JWT |
| `DELETE` | `/api/notifications/{id}` | Xóa thông báo | JWT |
| `GET` | `/api/admin/users` | Tất cả users | Admin |
| `GET` | `/api/admin/stats` | Thống kê hệ thống | Admin |
| `PUT` | `/api/admin/users/{userId}/role` | Đổi role user | Admin |
| `PUT` | `/api/admin/users/{userId}/tier` | Đổi tier user | Admin |
| `DELETE` | `/api/admin/users/{userId}` | Xóa user | Admin |
| `GET` | `/api/admin/upgrade-requests` | Yêu cầu nâng cấp pending | Admin |
| `POST` | `/api/admin/upgrade-requests/{id}/process` | Duyệt/từ chối | Admin |
| `GET` | `/api/admin/sse/notifications` | SSE cho Admin | Admin |

---

## 4. Các Điểm Còn Thiếu / Chưa Đủ

| # | Tính năng thiếu | README đề cập? | Ưu tiên cho Android |
|---|---|---|---|
| ❌ | **Rename file/folder** | ✅ Có ("rename" trong tính năng) | Cao |
| ❌ | **Tự động cleanup Trash sau 30 ngày** | ✅ Có ("@EnableScheduling") | Trung bình (logic server) |
| ❌ | **Xem/Preview file trực tuyến** | Không đề cập | Thấp (tùy loại file) |
| ❌ | **Move/Copy file giữa thư mục** | Không đề cập | Trung bình |
| ❌ | **Tìm kiếm toàn bộ Drive** (không giới hạn thư mục hiện tại) | Không đề cập | Trung bình |
| ⚠️ | **Avatar lưu Base64 trong DB** (dung lượng lớn) | Không | Cần lưu ý cho Android |

---

## 5. Data Models Cần Cho Android

### User
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "fullName": "string",
  "avatarUrl": "base64 string",
  "role": "USER | ADMIN",
  "tier": "FREE | PREMIUM",
  "maxQuota": 1073741824,
  "usedQuota": 512000
}
```

### FileMetadata
```json
{
  "id": 1,
  "fileName": "document.pdf",
  "filePath": "uuid_filename.pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "isFolder": false,
  "parentId": null,
  "ownerId": 1,
  "isDeleted": false,
  "deletedAt": null,
  "createdAt": "2024-01-01T00:00:00"
}
```

### FileShare
```json
{
  "shareId": 1,
  "fileId": 5,
  "fileName": "document.pdf",
  "targetEmail": "user@example.com | Public Link (Bất kỳ ai)",
  "expiresAt": "2024-02-01T00:00:00",
  "shareToken": "uuid-string"
}
```

### Notification
```json
{
  "id": 1,
  "userId": 2,
  "type": "FILE_SHARED | UPGRADE_REQUEST",
  "message": "Người dùng X đã chia sẻ file \"abc.pdf\" với bạn.",
  "targetUrl": "/shared",
  "read": false,
  "createdAt": "2024-01-01T10:00:00"
}
```

---

## 6. Lưu Ý Khi Phát Triển Android

1. **Authentication:** Lưu JWT token vào `SharedPreferences` hoặc `EncryptedSharedPreferences`. Token hết hạn sau **24 giờ** — cần handle 401 và redirect về login.

2. **File Upload:** Backend nhận `multipart/form-data` với field `file` và optional `parentId`. Cần dùng `OkHttp` hoặc `Retrofit` với `MultipartBody`.

3. **File Download:** Response header `Content-Disposition` chứa tên file. Response body là binary stream.

4. **SSE (Real-time):** Android cần dùng `OkHttp` với `EventSource` (thư viện `launchdarkly/okhttp-eventsource` hoặc implement tay) để subscribe real-time notifications.

5. **Quota:** Hiển thị thanh progress `usedQuota / maxQuota`. Gọi `/api/users/quota` để lấy số chính xác.

6. **Avatar:** Lưu dưới dạng Base64 trong DB — cần xử lý decode khi hiển thị. Với Android nên dùng `Glide` với `data:image/...;base64,...` URI.

7. **Share Token URL:** Format: `http://[server]/api/files/shared/{token}` — là link download trực tiếp, không cần login.

8. **Phân cấp thư mục:** Luôn track `currentParentId` (null = root). Khi navigate vào folder, cập nhật parentId và call lại danh sách file.
