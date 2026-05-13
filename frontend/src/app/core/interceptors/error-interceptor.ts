import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Chỉ in ngầm ra console để lập trình viên kiểm tra khi cần
        if (typeof window !== 'undefined') {
          console.error('HTTP Error Intercepted:', error);
        }
        
        //Trả lại nguyên bản object error (chứa status 400 và JSON từ Backend)
        return throwError(() => error);
      })
    );
  }
}