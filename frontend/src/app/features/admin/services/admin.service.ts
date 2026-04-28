import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) { }

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }

  updateRole(userId: number, role: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${userId}/role`, { role });
  }

  updateTier(userId: number, tier: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${userId}/tier`, { tier });
  }

  deleteUser(userId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/users/${userId}`);
  }

  getUpgradeRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/upgrade-requests`);
  }

  processUpgradeRequest(requestId: number, action: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/upgrade-requests/${requestId}/process`, { action });
  }
}
