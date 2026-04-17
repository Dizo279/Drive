import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = `${environment.apiUrl}/files`;

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  downloadFile(id: number): Observable<Blob> {
    // Gọi API kèm theo responseType: 'blob' để Angular hiểu đây là luồng nhị phân, không phải JSON
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  getFiles(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getFileDetail(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  getQuota(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/users/quota`);
  }

  deleteFile(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  shareFile(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/share`, {});
  }
}