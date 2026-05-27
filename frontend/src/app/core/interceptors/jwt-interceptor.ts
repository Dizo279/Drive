import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '@features/auth/services/auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!isPlatformBrowser(this.platformId)) {
      return next.handle(request);
    }

    const token = this.authService.getToken();
    const authRequest = this.addToken(request, token);

    return next.handle(authRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401 || this.isAuthEndpoint(request.url) || request.headers.has('x-retried')) {
          return throwError(() => error);
        }

        if (!this.isRefreshing) {
          this.isRefreshing = true;
          this.refreshTokenSubject.next(null);

          return this.authService.refreshToken().pipe(
            switchMap(response => {
              this.isRefreshing = false;
              const newToken = response.accessToken;
              localStorage.setItem('jwt_token', newToken);
              this.refreshTokenSubject.next(newToken);
              return next.handle(this.addToken(request.clone({ setHeaders: { 'x-retried': '1' } }), newToken));
            }),
            catchError(refreshError => {
              this.isRefreshing = false;
              this.authService.logout();
              return throwError(() => refreshError);
            })
          );
        }

        return this.refreshTokenSubject.pipe(
          filter(newToken => newToken !== null),
          take(1),
          switchMap(newToken => next.handle(this.addToken(request.clone({ setHeaders: { 'x-retried': '1' } }), newToken)))
        );
      })
    );
  }

  private addToken(request: HttpRequest<any>, token: string | null): HttpRequest<any> {
    if (token && request.url.startsWith('http://localhost:8080')) {
      return request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return request;
  }

  private isAuthEndpoint(url: string): boolean {
    return url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');
  }
}