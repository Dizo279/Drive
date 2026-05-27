# JWT Refresh Security Design (Stateless)

## Scope
Nâng cấp cơ chế xác thực hiện tại từ 1 JWT duy nhất sang mô hình access token + refresh token.

- Access token: 30 phút
- Refresh token: 7 ngày
- Refresh token lưu trong HTTP-only Secure cookie
- Chọn phương án stateless refresh token (không lưu DB)

## Current State
- Backend đang cấp 1 token tại `POST /api/auth/login` và dùng cho toàn bộ phiên.
- Frontend lưu token trong localStorage, interceptor gắn Bearer cho request API.
- Chưa có endpoint refresh, chưa có cơ chế tự gia hạn phiên.

## Target Architecture

### Backend
1. Mở rộng `JwtUtil`
   - `generateAccessToken(username, userId)` với TTL 30 phút
   - `generateRefreshToken(username, userId)` với TTL 7 ngày
   - Gắn claim `type` để phân biệt `access` và `refresh`
   - Validate chữ ký, hạn dùng, và loại token theo endpoint

2. Mở rộng `AuthResource`
   - `POST /auth/login`
     - Verify username/password
     - Trả JSON chứa access token
     - Set cookie refresh token
   - `POST /auth/refresh`
     - Đọc cookie refresh token
     - Validate refresh token
     - Trả access token mới
   - `POST /auth/logout`
     - Clear refresh cookie

3. Giữ nguyên `JwtAuthFilter`
   - Chỉ xử lý access token cho API bảo vệ
   - Không dùng refresh token tại filter tài nguyên

4. CORS/Security config
   - Cho phép credential để browser gửi/nhận cookie cross-origin

### Frontend
1. `AuthService`
   - login lưu access token + username
   - thêm API refresh/logout với `withCredentials: true`

2. `JwtInterceptor`
   - Gắn Bearer access token như hiện tại
   - Khi gặp 401 (ngoại trừ endpoint auth), gọi refresh
   - Nếu refresh thành công: cập nhật access token, retry request gốc 1 lần
   - Nếu refresh thất bại: logout local, điều hướng về login

## API Contract

### 1) POST /api/auth/login
Request:
```json
{
  "username": "string",
  "password": "string"
}
```

Success 200:
```json
{
  "accessToken": "<jwt>",
  "username": "string"
}
```

Response header:
- `Set-Cookie: refresh_token=<jwt>; HttpOnly; Secure; SameSite=None; Path=/api/auth; Max-Age=604800`

Failure:
- `401 Unauthorized` khi sai thông tin đăng nhập

### 2) POST /api/auth/refresh
Request body: rỗng

Success 200:
```json
{
  "accessToken": "<jwt>"
}
```

Failure `401 Unauthorized` khi:
- thiếu cookie `refresh_token`
- token hết hạn
- token sai chữ ký
- token không có `type=refresh`

### 3) POST /api/auth/logout
Request body: rỗng

Success 200:
- clear cookie:
  - `Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=None; Path=/api/auth; Max-Age=0`
- body: thông báo đăng xuất thành công

## Runtime Flows

### Login flow
1. User gọi login.
2. Backend trả access token + set refresh cookie HttpOnly.
3. Frontend lưu access token vào localStorage.

### Authenticated request flow
1. Interceptor gắn Bearer access token.
2. Backend trả 2xx => hoàn tất.
3. Nếu 401 => chạy flow refresh.

### Refresh flow
1. Interceptor nhận 401, gọi `/api/auth/refresh` với credentials.
2. Thành công: nhận access token mới, cập nhật localStorage.
3. Retry request gốc đúng 1 lần.
4. Nếu refresh fail: clear local auth và chuyển về login.

### Logout flow
1. Frontend gọi `/api/auth/logout` với credentials.
2. Backend clear refresh cookie.
3. Frontend clear access token + username localStorage.

## Concurrency & Edge Cases
- Không trigger refresh với endpoint `/auth/login`, `/auth/refresh`, `/auth/logout`.
- Mỗi request retry tối đa 1 lần để tránh loop vô hạn.
- Nếu nhiều request cùng 401, chỉ 1 refresh request chạy tại một thời điểm; request còn lại chờ kết quả.
- Nếu refresh thất bại, toàn bộ request đang chờ fail đồng nhất và app về trạng thái chưa đăng nhập.

## Security Notes (Chosen Trade-off)
Phương án stateless refresh token được chọn để đơn giản triển khai. Đổi lại:
- Server không revoke ngay token đã phát hành nếu token bị lộ.
- Logout chỉ xóa cookie phía client; token bị lộ vẫn có thể dùng tới khi hết 7 ngày.

Ràng buộc này được chấp nhận theo lựa chọn phương án 2 của user.

## Verification Strategy

### Backend
- Login success/fail và assert cookie flags.
- Refresh success/fail (missing/expired/invalid/wrong-type token).
- Logout trả clear-cookie đúng thuộc tính.
- Unit test `JwtUtil` cho TTL + claim `type`.

### Frontend
- Interceptor refresh-on-401 thành công thì retry request.
- Refresh fail thì logout local.
- Không refresh cho auth endpoints.
- Nhiều 401 đồng thời chỉ tạo 1 refresh call.

### Manual checks
- DevTools xác nhận refresh token không đọc được bằng JS.
- Cookie có `HttpOnly`, `Secure`, `SameSite=None`, `Path=/api/auth`.
- Access hết hạn thì tự refresh.
- Xóa refresh cookie rồi đợi access hết hạn => app về login.
- Logout xong không tự refresh lại được.

## Out of Scope
- Không thêm DB/session table cho refresh token.
- Không thêm rotate refresh token mỗi lần refresh.
- Không thêm logout-all-devices.
