import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '@features/auth/services/auth.service';

/**
 * Guard to prevent unauthenticated users from accessing protected routes.
 * Redirects to /login if the user is not logged in.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.getToken()) {
    return true;
  }

  // Store the attempted URL to redirect back after login (Premium UX)
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

/**
 * Guard to prevent logged-in users from accessing auth routes (login, register).
 * Redirects to /files if the user is already logged in.
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
