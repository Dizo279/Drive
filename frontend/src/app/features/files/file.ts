import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpEvent } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = `${environment.apiUrl}/files`;

  constructor(private http: HttpClient) {}

  uploadFile(file: File, parentId: number | null): Observable<HttpEvent<any>> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (parentId !== null && parentId !== undefined) {
      formData.append('parentId', parentId.toString());
    }
    
    // Thêm config reportProgress và observe 'events'
    return this.http.post<any>(`${this.apiUrl}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  downloadFile(id: number): Observable<Blob> {
    // Gọi API kèm theo responseType: 'blob' để Angular hiểu đây là luồng nhị phân, không phải JSON
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  getFiles(parentId?: number | null): Observable<any[]> {
    let params = new HttpParams();
    if (parentId) {
      params = params.set('parentId', parentId.toString());
    }
    return this.http.get<any[]>(this.apiUrl, { params });
  }

  createFolder(folderName: string, parentId?: number | null): Observable<any> {
    const body = {
      fileName: folderName,
      parentId: parentId || null
    };
    return this.http.post(`${this.apiUrl}/folder`, body);
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

  shareFile(id: number, payload: any = {}): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/share`, payload);
  }

  getAllGlobalFiles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/list/all`);
  }
}