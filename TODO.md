# TODO - Restructure Frontend Angular App

## Phase 1: File System Operations ✅
- [x] Rename `frontend/src/app/shared/` → `frontend/src/app/notification/`
- [x] Move `features/auth/auth.ts` → `features/auth/services/auth.service.ts`
- [x] Move `features/auth/auth.spec.ts` → `features/auth/services/auth.service.spec.ts`
- [x] Move `features/files/file.ts` → `features/files/services/file.service.ts`
- [x] Move `features/admin/admin.service.ts` → `features/admin/services/admin.service.ts`

## Phase 2: Add Path Aliases ✅
- [x] Update `frontend/tsconfig.json` with `baseUrl` and `paths`

## Phase 3: Update Imports ✅
- [x] `frontend/src/app/app.config.ts` — use `@core/...` aliases
- [x] `frontend/src/app/app.routes.ts` — use `@features/...` aliases
- [x] `frontend/src/app/core/interceptors/jwt-interceptor.ts` — update AuthService import
- [x] `frontend/src/app/features/auth/components/login/login.ts` — update AuthService import
- [x] `frontend/src/app/features/auth/components/register/register.ts` — update AuthService import
- [x] `frontend/src/app/features/auth/services/auth.service.ts` — update environment import
- [x] `frontend/src/app/features/files/components/file-list/file-list.ts` — update FileService, AuthService, NotificationBellComponent imports
- [x] `frontend/src/app/features/files/components/file-upload/file-upload.ts` — update FileService import
- [x] `frontend/src/app/features/files/services/file.service.ts` — update environment import
- [x] `frontend/src/app/features/admin/components/admin-dashboard/admin-dashboard.ts` — update AdminService, NotificationBellComponent imports
- [x] `frontend/src/app/notification/services/notification.service.ts` — update environment import
- [x] `frontend/src/app/features/auth/services/auth.service.spec.ts` — fix incorrect import/name

## Verification ✅
- [x] Search confirms no remaining old path references
- [x] `ng build --configuration production` succeeded

