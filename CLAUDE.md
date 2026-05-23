# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

File Manager is a full-stack file management system with an Apple Design-inspired UI. It consists of three clients (Angular web, Android app) backed by a single Spring Boot REST API. The project language (comments, UI text) is Vietnamese.

## Build & Run Commands

### Backend (Spring Boot + Jersey)
```bash
cd backend
mvn clean install          # Build
mvn spring-boot:run        # Run (localhost:8080/api)
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run single test class
```

### Frontend (Angular 21)
```bash
cd frontend
npm install                # Install dependencies
ng serve                   # Dev server (localhost:4200)
npm run build              # Production build
npm run test               # Run tests (Vitest)
```

### Android
```bash
cd android
./gradlew assembleDebug    # Build debug APK
```
Requires Android SDK 34, Java 17, minSdk 26.

### Database
MySQL required. Create database `file_manager_db`. Config in `backend/src/main/resources/application.properties`. H2 available for quick testing via `http://localhost:8080/h2-console`.

## Architecture

### Monorepo Structure
- **`backend/`** — Spring Boot 3.2.4 REST API using **JAX-RS/Jersey** (NOT Spring MVC `@RestController`)
- **`frontend/`** — Angular 21 SPA with SSR support, standalone components, no NgModules
- **`android/`** — Native Android app (Java, Retrofit2, Material Design 3)

### Backend Architecture (`com.filemanager.*`)

**REST layer uses JAX-RS annotations** (`@Path`, `@GET`, `@POST`, `@Produces`, `@Consumes`), not Spring MVC. All resource classes are registered in `JerseyConfig`.

| Layer | Package | Purpose |
|-------|---------|---------|
| Config | `config/` | `JerseyConfig` (registers resources + filters), `SecurityConfig` (CORS, BCrypt) |
| Entities | `entity/` | `User`, `FileMetadata`, `FileShare`, `Notification`, `UpgradeRequest` |
| Repositories | `repository/` | Spring Data JPA interfaces. `FileRepository` has native SQL with CTEs for recursive folder queries |
| Resources | `resource/` | JAX-RS REST endpoints: `AuthResource`, `FileResource`, `UserResource`, `AdminResource`, `NotificationResource` |
| Services | `service/` | `StorageService` (filesystem), `QuotaService` (upload limits), `NotificationService` (SSE push), `TrashCleanupService` (scheduled @3AM) |
| Security | `security/` | `JwtUtil` — HMAC-SHA256 tokens, 24h expiry |
| Filters | `filter/` | `JwtAuthFilter` (ContainerRequestFilter, skips `/auth/*` and `/shared/*`), `CorsFilter` |

**Key patterns:**
- **Soft delete with trash:** `FileMetadata.isDeleted` + `deletedAt`, auto-purged after 30 days by `TrashCleanupService`
- **Hierarchical folders:** `parentId` field on `FileMetadata`, recursive CTE queries
- **File sharing:** Public links (token-based, no auth) and email-based (specific user). Share tokens are UUIDs with optional expiration
- **Quota system:** FREE tier = 1GB, PREMIUM = 100GB. Enforced transactionally in `QuotaService`
- **Real-time SSE:** `NotificationService` manages per-user and admin SSE connections via `ConcurrentHashMap`
- **JWT in request context:** `JwtAuthFilter` stores userId/username in `ContainerRequestContext` properties; resources access via `@Context`

### Frontend Architecture

**Path aliases** (from tsconfig.json):
- `@core/*` → `src/app/core/*`
- `@features/*` → `src/app/features/*`
- `@notification/*` → `src/app/notification/*`
- `@env/*` → `src/environments/*`

| Area | Path | Contents |
|------|------|----------|
| Core | `core/interceptors/` | `JwtInterceptor` (adds Bearer token), `ErrorInterceptor` (logs errors) |
| Core | `core/services/` | `ConfirmDialogService` (signal-based dialog state) |
| Auth | `features/auth/` | Login, Register components + `AuthService` (JWT in localStorage) |
| Files | `features/files/` | FileList (drag-drop upload, bulk ops, search/sort, sharing modal), FileUpload, TrashList, SharedList, AccountSettings |
| Admin | `features/admin/` | AdminDashboard (user management, upgrade requests, SSE notifications) |
| Notifications | `notification/` | NotificationBell component + NotificationService (SSE real-time) |

**Key patterns:**
- All components are **standalone** (no NgModules)
- Routes are **eager-loaded** (no lazy loading currently)
- No route guards — auth is not enforced at the routing level
- SSE connections use `?token={jwt}` query param (EventSource doesn't support headers)
- `FileListComponent` is the most complex component (~770 lines) with drag-drop folder upload using `FileSystemEntry` API

### Android Architecture (`com.filemanager.android.*`)

Feature-based package structure mirroring the web frontend. Uses Retrofit2 + OkHttp3 for API calls, `AuthInterceptor` for JWT, `SessionManager` for token storage, Glide for image loading.

### API Endpoints (all under `/api`)

| Prefix | Auth | Purpose |
|--------|------|---------|
| `/auth/register`, `/auth/login` | No | Authentication |
| `/files/**` | JWT | File CRUD, upload, download, trash, sharing |
| `/users/**` | JWT | Profile, quota, upgrade requests |
| `/admin/**` | Admin | User management, stats, upgrade processing |
| `/notifications/**` | JWT | Notification list, mark read, SSE stream |

## Code Conventions

### Backend (Java)
- **4-space indentation**
- Classes: `PascalCase`, methods/variables: `camelCase`, constants: `SCREAMING_SNAKE_CASE`
- Database tables/columns: `snake_case`
- Do not change dependency versions in `pom.xml` unless explicitly requested

### Frontend (TypeScript/Angular)
- **2-space indentation**, single quotes, 100-char print width (Prettier)
- Files: `kebab-case`, classes: `PascalCase`, variables/methods: `camelCase`
- Do NOT set `standalone: true` in decorators (default in Angular 20+)
- Use `input()`/`output()` functions instead of `@Input`/`@Output` decorators
- Use `inject()` function instead of constructor injection
- Use native control flow (`@if`, `@for`, `@switch`) instead of `*ngIf`, `*ngFor`
- Use `computed()` for derived state, signals for local state
- Set `changeDetection: ChangeDetectionStrategy.OnPush`
- Use `class` bindings instead of `ngClass`, `style` bindings instead of `ngStyle`
- Do not change dependency versions in `package.json` unless explicitly requested

## Design System

Apple Design-inspired. See `apple_design.md` for full spec. Key rules:
- Single accent color: Apple Blue (`#0071e3`) — interactive elements only
- Backgrounds alternate: black (`#000000`) and light gray (`#f5f5f7`)
- Typography: SF Pro Display (≥20px) / SF Pro Text (<20px) with negative letter-spacing at all sizes
- Shadows: one soft diffused shadow (`rgba(0,0,0,0.22) 3px 5px 30px`) or none
- No borders on cards, no gradients/textures, no additional accent colors
- Pill CTAs use `border-radius: 980px`

## File Storage

Files are stored at `D:/mydrive-storage/uploads` with UUID filenames. Original names preserved in `FileMetadata` DB records. Max upload size: 10MB.
