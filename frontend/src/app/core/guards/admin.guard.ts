import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@features/auth/services/auth.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

/**
 * Guard to prevent non-admin users from accessing administrative routes.
 * If unauthenticated: redirects to /login.
 * If authenticated but not an ADMIN: redirects to /files and triggers a danger alert message.
 */
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

  // User is authenticated but does not have the ADMIN role
  router.navigate(['/files']);
  
  // Show an alert indicating access denied (Premium UI dialog)
  dialogService.alert({
    title: 'Truy cập bị từ chối',
    message: 'Tài khoản của bạn không có quyền truy cập vào trang quản trị viên.',
    type: 'danger',
    confirmText: 'Đã hiểu'
  });

  return false;
};
