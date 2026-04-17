import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMsg = 'Đã xảy ra lỗi không xác định!';
        
        if (typeof ErrorEvent !== 'undefined' && error.error instanceof ErrorEvent) {
          // Xử lý lỗi client-side
          errorMsg = `Lỗi Client: ${error.error.message}`;
        } else {
          // Xử lý lỗi server-side
          errorMsg = `Lỗi Server: Mã ${error.status}, Thông báo: ${error.message}`;
        }

        // Bảo vệ SSR
        if (typeof window !== 'undefined') {
          alert(`Hệ thống thông báo: ${errorMsg}`);
        }
        
        return throwError(() => new Error(errorMsg));
      })
    );
  }
}