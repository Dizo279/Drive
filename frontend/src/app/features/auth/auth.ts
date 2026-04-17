import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        if (response && response.token) {
          localStorage.setItem('jwt_token', response.token);
          localStorage.setItem('username', response.username);
        }
      })
    );
  }

  register(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, user, { responseType: 'text' });
  }

  getToken(): string | null {
    // Chỉ lấy token nếu đang chạy trên trình duyệt
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem('jwt_token');
    }
    return null; // Trả về null nếu đang chạy trên Server
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('username');
  }
}