# File Manager - Hệ thống quản lý file hiện đại

## 🎯 Giới thiệu

**File Manager** là hệ thống quản lý file toàn diện, bao gồm backend Spring Boot + Jersey, frontend Angular SSR và một ứng dụng Android native. Dự án sử dụng phong cách Apple Design, tính năng upload/download, chia sẻ file, quản lý thùng rác và dashboard quản trị.

### Tính năng chính
| Tính năng | Mô tả |
|-----------|-------|
| **Upload/Tải lên** | Hỗ trợ file, lưu trữ trên filesystem local |
| **Quản lý file** | Xem, đổi tên, xóa vào thùng rác và phục hồi |
| **Chia sẻ file** | Tạo link chia sẻ an toàn và JWT-based auth |
| **Authentication** | Đăng ký/đăng nhập với xác thực email/username/password |
| **Bảo mật & Phân quyền** | Chặn truy cập trái phép bằng Angular Route Guards (Auth/Admin/NoAuth) và JWT claims. Tự động điều hướng trả về đường dẫn cũ (`returnUrl`) |
| **Quota** | Giới hạn dung lượng theo user |
| **Notifications** | Thông báo real-time qua SSE |
| **Admin Dashboard** | Quản lý users, files, upgrade requests |
| **Thùng rác tự động** | Cleanup files theo lịch trình |

## 🏗️ Kiến trúc

```
file-manager/
├── backend/          # Spring Boot + Jersey REST API
├── frontend/         # Angular 21 SPA với SSR
├── android/          # Android native app
├── docs/             # Tài liệu và kế hoạch
├── uploads/          # Local filesystem uploads
└── README.md
```

## 🚀 Cài đặt & Chạy

### Yêu cầu
- **Java 17+** (Backend)
- **Node.js 20+** (Frontend)
- **MySQL** (Tùy chọn, có thể dùng H2 để test nhanh)

### 1. Backend (Spring Boot)
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
**API chạy tại:** `http://localhost:8080/api`

**H2 Console (test nhanh):** `http://localhost:8080/h2-console`

### 2. Frontend (Angular)
```bash
cd frontend
npm install
ng serve
```
**App chạy tại:** `http://localhost:4200`

### 3. Database (MySQL)
```sql
CREATE DATABASE file_manager_db;
```
Cập nhật `backend/src/main/resources/application.properties`

## 📖 API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/auth/register` | Đăng ký user | No |
| `POST` | `/api/auth/login` | Đăng nhập, trả JWT | No |
| `GET` | `/api/files` | Danh sách files | Yes |
| `POST` | `/api/files/upload` | Upload file | Yes |
| `POST` | `/api/files/share` | Tạo share link | Yes |
| `DELETE` | `/api/files/{id}` | Xóa file (vào thùng rác) | Yes |
| `GET` | `/api/notifications` | Lấy notifications | Yes |
| `GET` | `/api/admin/users` | Quản lý users (Admin) | Admin |

## 🛠️ Công nghệ Stack

### Backend
```
✅ Spring Boot 3.2.4 + Jersey (JAX-RS)
✅ Spring Data JPA + Hibernate
✅ MySQL / H2 (embedded)
✅ Spring Security + JWT (jjwt 0.11.5)
✅ File upload (jersey-multipart)
✅ Maven
```

### Frontend
```
✅ Angular 21.2 + SSR
✅ Standalone Components
✅ Reactive Forms + Validators
✅ Signal-based reactivity
✅ HttpClient + Interceptors (JWT)
✅ TailwindCSS (Apple Design inspired)
✅ RxJS 7.8
```

## 🎨 Design System

- **Typography:** SF Pro (Display/Text) với optical sizing
- **Colors:** Apple Blue (`#0071e3`) + Monochrome (Black/`#f5f5f7`)
- **Spacing:** 8px base system
- **Shadows:** Soft diffused (`rgba(0,0,0,0.22)`)
- **Responsive:** Mobile-first (360px → 1440px+)

Xem chi tiết: [apple_design.md](apple_design.md)

## 📁 Cấu trúc thư mục

```
d:/file-manager/
├── backend/
│   ├── src/main/java/com/filemanager/
│   │   ├── FileManagerApplication.java  # Main class
│   │   ├── config/ (JerseyConfig, SecurityConfig)
│   │   ├── entity/ (User, FileMetadata...)
│   │   ├── repository/ (JPA Repos)
│   │   ├── resource/ (REST Controllers)
│   │   └── service/ (Business Logic)
│   ├── src/main/resources/application.properties
│   └── uploads/  # Thư mục lưu file
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── guards/ (auth.guard.ts, admin.guard.ts)
│   │   │   └── interceptors/ (error-interceptor.ts, jwt-interceptor.ts)
│   │   ├── features/
│   │   │   ├── auth/ (login/register)
│   │   │   ├── files/ (file-list, upload, trash, shared)
│   │   │   └── admin/ (dashboard)
│   │   └── app.ts  # Root component
│   ├── angular.json
│   └── package.json
├── android/          # Android native app
├── docs/             # Documentation and plans
├── uploads/          # Local filesystem upload storage
└── AGENTS.md  # Hướng dẫn cho AI Agents
```

## 🧪 Test & Debug

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm run test
```

### API Test (Swagger alternative)
Sử dụng Postman hoặc curl:
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H \"Content-Type: application/json\" \
  -d '{\"fullName\":\"Test User\",\"email\":\"test@example.com\",\"username\":\"testuser\",\"password\":\"password123\"}'
```

## 🔒 Bảo mật & Phân quyền

- **Xác thực JWT (24h expiry):** Tích hợp phân quyền trực tiếp qua JWT claims (chứa thông tin `role` của người dùng).
- **Angular Route Guards (Bảo vệ đường dẫn):**
  - `authGuard`: Chặn người dùng chưa đăng nhập, tự động lưu và chuyển hướng quay lại đúng trang cũ (`returnUrl`) sau khi login thành công.
  - `noAuthGuard`: Chặn người dùng đã đăng nhập quay lại các trang `/login`, `/register`.
  - `adminGuard`: Giải mã JWT kiểm tra quyền `ADMIN`. Nếu người dùng thường (`USER`) cố ý truy cập `/admin`, guard sẽ tự động đẩy về `/files` kèm theo hộp thoại cảnh báo (Danger Dialog UI) sắc nét.
- **Xác thực Lớp kép (Server-side Validation):** Mọi REST endpoint quản trị (`/api/admin/*`) đều được bảo vệ nghiêm ngặt bằng helper `isAdmin()` đối chiếu ID với CSDL, loại bỏ nguy cơ tấn công bypass client.
- **Input Validation:** Xác thực chặt chẽ dữ liệu đầu vào ở cả client-side (Angular Reactive Forms) và server-side.
- **SQL Injection Protection:** Ngăn ngừa tấn công SQLi nhờ cơ chế Spring Data JPA & Hibernate.
- **Case-insensitive:** Bảo vệ trùng lặp duy nhất email và username không phân biệt chữ hoa chữ thường.

## 📈 Performance

- **Backend:** Spring Boot optimized, HikariCP connection pool
- **Frontend:** Angular SSR, lazy loading, signal reactivity
- **Storage:** Local filesystem (D:/mydrive-storage/uploads)
- **Database:** Index trên `user_id`, `parent_id`, `deleted_at`

## 🤝 Contributing

1. Fork repository
2. Tạo feature branch: `git checkout -b feature/new-feature`
3. Commit changes: `git commit -m \"Add new feature\"`
4. Push và tạo Pull Request

Tuân thủ [AGENTS.md](AGENTS.md) và code conventions.

## 👥 Authors

- **Developer**: Duy Nguyen

---

**Built with ❤️ using Apple Design Principles • Java 17 • Angular 21 • Spring Boot 3.2**

```bash
# Chạy fullstack development
# Terminal 1: cd backend && mvn spring-boot:run
# Terminal 2: cd frontend && ng serve
# Browser: http://localhost:4200
