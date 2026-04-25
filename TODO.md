# TODO: Upgrade Tier with SSE Notifications

## Backend
- [x] Bước 1: Tạo NotificationService (SSE broadcast service)
- [x] Bước 2: Thêm SSE endpoint vào AdminResource, sửa JwtAuthFilter hỗ trợ token qua query param
- [x] Bước 3: Gọi notification trong UserResource.requestUpgrade()

## Frontend
- [x] Bước 4: Fix file-list.ts (displayToast và duplicate requestUpgrade)
- [x] Bước 5: Cập nhật AdminService (thêm API methods cho upgrade requests)
- [x] Bước 6: Cập nhật Admin Dashboard (SSE connection + quản lý requests)
- [x] Bước 7: Cập nhật Account Settings (nút yêu cầu nâng cấp)

## Testing
- [ ] Build backend
- [ ] Test luồng end-to-end
