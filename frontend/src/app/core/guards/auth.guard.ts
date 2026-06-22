import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@features/auth/services/auth.service';

/**
 * Guard để ngăn chặn người dùng chưa xác thực truy cập vào các route được bảo vệ.
 * Nếu chưa xác thực: chuyển hướng đến /login.
 * (Premium UX) Lưu lại URL đã cố gắng truy cập để chuyển hướng sau khi đăng nhập thành công.
 */

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getToken()) {
    return true;
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/**
 * Guard để ngăn chặn người dùng đã xác thực truy cập vào các route dành cho khách (như /login, /register).
 * Nếu đã xác thực: chuyển hướng đến /files.
 */
export const noAuthGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getToken()) {
    router.navigate(['/files']);
    return false;
  }

  return true;
};
