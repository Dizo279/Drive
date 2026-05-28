# Hướng dẫn dành cho AI Agent (AGENTS.md)

Tài liệu này cung cấp hướng dẫn về kiến trúc, quy định code và công nghệ cho các AI Agent tham gia phát triển dự án File Manager.

## 1. Kiến trúc Project

Dự án là monorepo gồm backend Java, frontend Angular và phần mobile Android.

### Backend (`/backend`)
Dùng Spring Boot với Jersey cho REST API:
- `com.filemanager.config`: Cấu hình hệ thống, đăng ký Jersey resources, cấu hình bảo mật.
- `com.filemanager.entity`: Các thực thể JPA (mapping tới MySQL hoặc H2).
- `com.filemanager.repository`: Spring Data JPA repositories.
- `com.filemanager.resource`: REST endpoints bằng JAX-RS/Jersey.
- `com.filemanager.service`: Business logic và xử lý nghiệp vụ.
- `com.filemanager.security`: JWT, mã hóa, xác thực.
- `com.filemanager.exception`: Xử lý lỗi toàn cục.

### Frontend (`/frontend`)
Sử dụng Angular feature-based với SSR:
- `src/app/core`: interceptors, dịch vụ dùng chung, cấu hình app.
- `src/app/features`: chia theo auth, files, admin.
- `src/app/features/files`: quản lý tệp, upload, trash, chia sẻ.
- `src/app/features/auth`: đăng ký, đăng nhập.
- `src/app/features/admin`: dashboard admin.

### Android (`/android`)
Phần Android là ứng dụng native Java, dùng Retrofit/OkHttp cho API và JWT lưu trữ session.

---

## 2. Code Convention

### Naming Convention
- **Java:**
  - Class: `PascalCase`
  - Method/variable: `camelCase`
  - Constant: `SCREAMING_SNAKE_CASE`
  - Package: `lowercase`
- **Angular:**
  - File names: `kebab-case`
  - Class: `PascalCase`
  - Variable/method: `camelCase`
- **Database:**
  - Table/column: `snake_case`

### Format & Style Guide
- Java: 4 spaces indent, standard Java style.
- Angular/TypeScript: 2 spaces indent, Prettier formatting.
- Thêm comment cho logic phức tạp.
- Không sửa version `pom.xml` / `package.json` trừ khi có yêu cầu rõ ràng.

---

## 3. Quy tắc công nghệ (Quan trọng)

### Client Side
- **Frontend:** Angular 21
- **Mobile:** Android native Java
- **Web:** HTML, CSS, JS

### Server Side
- **API Layer:** Jersey (JAX-RS) trên Spring Boot
- **Data Access:** Spring Data JPA + Hibernate
- **Database:** MySQL (H2 cho test)
- **Security:** Spring Security + JWT

---

## 4. Quy trình làm việc của Agent
1. **Đọc hiểu:** Luôn kiểm tra cấu trúc hiện tại và dự án trước khi tạo hoặc sửa file.
2. **Xác thực:** Nếu cần thay đổi kiến trúc hoặc công nghệ, xác nhận với yêu cầu user.
3. **Tuân thủ:** Không tự ý nâng cấp dependency version trong `pom.xml` hoặc `package.json` nếu không có chỉ dẫn rõ ràng.
