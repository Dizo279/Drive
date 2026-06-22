import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@features/auth/services/auth.service';
import { ConfirmDialogService } from '@ui/services/confirm-dialog.service';

/**
 * Guard để ngăn chặn người dùng không phải admin truy cập vào Administrative
 * Nếu chưa xác thực: chuyển hướng đến /login.
 * Nếu đã xác thực nhưng không phải ADMIN: chuyển hướng đến /files và hiển thị một thông báo cảnh báo nguy hiểm.
 **/

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const dialogService = inject(ConfirmDialogService);

  const token = authService.getToken();
  if (!token) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const role = authService.getUserRole();
  if (role === 'ADMIN') {
    return true;
  }

  router.navigate(['/files']);
  
  dialogService.alert({
    title: 'Truy cập bị từ chối',
    message: 'Tài khoản của bạn không có quyền truy cập vào trang quản trị viên.',
    type: 'danger',
    confirmText: 'Đã hiểu'
  });

  return false;
};
