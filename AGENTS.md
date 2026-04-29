# Hướng dẫn dành cho AI Agent (AGENTS.md)

Tài liệu này cung cấp hướng dẫn về kiến trúc, quy định code và công nghệ cho các AI Agent tham gia phát triển dự án.

## 1. Kiến trúc Project

Dự án được chia thành hai phần chính: Backend (Java/Spring) và Frontend (Angular).

### Backend (`/backend`)
Sử dụng cấu trúc phân lớp chuẩn của Spring Boot kết hợp với JAX-RS (Jersey):
- `com.filemanager.config`: Cấu hình hệ thống (Security, Jersey).
- `com.filemanager.entity`: Các thực thể JPA (Mapping với Database MySQL).
- `com.filemanager.repository`: Lớp truy xuất dữ liệu (Spring Data JPA).
- `com.filemanager.resource`: Các REST Endpoint (Sử dụng JAX-RS/Jersey).
- `com.filemanager.service`: Lớp nghiệp vụ (Business Logic).
- `com.filemanager.security`: Xử lý bảo mật, JWT.
- `com.filemanager.exception`: Xử lý lỗi toàn cục.

### Frontend (`/frontend`)
Sử dụng Angular theo kiến trúc Feature-based:
- `src/app/core`: Chứa các interceptor (JWT, Error), guard và các service dùng chung toàn hệ thống.
- `src/app/features`: Chia theo tính năng (auth, files, admin). Mỗi tính năng có components, services và models riêng.
- `src/app/features/files`: Quản lý tệp tin, tải lên, chia sẻ.
- `src/app/features/auth`: Đăng ký, đăng nhập.
- `src/app/features/admin`: Giao diện quản trị viên.

---

## 2. Code Convention

### Naming Convention
- **Java:**
    - Class: `PascalCase` (ví dụ: `FileService`)
    - Method/Variable: `camelCase` (ví dụ: `uploadFile`)
    - Constants: `SCREAMING_SNAKE_CASE` (ví dụ: `MAX_FILE_SIZE`)
    - Package: `lowercase`
- **Frontend (Angular):**
    - File names: `kebab-case` (ví dụ: `file-list.component.ts`)
    - Class: `PascalCase` (ví dụ: `FileListComponent`)
    - Variables/Methods: `camelCase`
- **Database:**
    - Table: `snake_case` (ví dụ: `file_metadata`)
    - Column: `snake_case`

### Format & Style Guide
- Sử dụng Indent là 4 spaces cho Java và 2 spaces cho Angular/TypeScript.
- Luôn có comment cho các method phức tạp.
- Sử dụng Prettier cho frontend và standard Java style cho backend.

---

## 3. Quy tắc công nghệ (Quan trọng)

Dự án bắt buộc tuân thủ các công nghệ sau:

### Client Side
- **Framework chính:** Angular.
- **Web base:** HTML, JS, CSS.
- **Mobile:** Android (Sử dụng Java/Kotlin cho app Android).
- **Legacy/Template:** JSP, JSF (Sử dụng khi có yêu cầu tích hợp hoặc các module cũ).

### Server Side
- **Framework:** Spring Framework.
- **Web Layer:** Spring MVC và JAX-RS (Jersey) cho REST APIs.
- **Data Access:** Spring Data JPA (Hibernate).
- **Database:** MySQL.
- **Security:** Spring Security + JWT.

---

## 4. Quy trình làm việc của Agent
1. **Đọc hiểu:** Luôn kiểm tra cấu trúc thư mục hiện tại trước khi tạo file mới.
2. **Tuân thủ:** Không tự ý thay đổi version của các thư viện trong `pom.xml` hoặc `package.json` trừ khi được yêu cầu.
