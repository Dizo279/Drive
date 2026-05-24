import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpEvent } from '@angular/common/http';
import { environment } from '@env/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private readonly apiUrl = `${environment.apiUrl}/files`;

  constructor(private readonly http: HttpClient) {}

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

  /**
   * Tải lên toàn bộ folder, giữ nguyên cấu trúc thư mục.
   * @param files     Danh sách File objects
   * @param relativePaths Đường dẫn tương đối của từng file trong folder gốc
   *                  (ví dụ: "MyProject/src/App.java")
   * @param parentId  ID thư mục cha trên cloud (nếu có)
   */
  uploadFolder(files: File[], relativePaths: string[], parentId: number | null): Observable<HttpEvent<any>> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file, file.name);
    });
    relativePaths.forEach((path) => {
      formData.append('relativePaths', path);
    });
    if (parentId !== null && parentId !== undefined) {
      formData.append('parentId', parentId.toString());
    }
    return this.http.post<any>(`${this.apiUrl}/upload-folder`, formData, {
      reportProgress: true,
      observe: 'events'
    });
  }

  downloadFile(id: number): Observable<Blob> {
    // Gọi API kèm theo responseType: 'blob' để Angular hiểu đây là luồng nhị phân, không phải JSON
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  downloadFolder(id: number): Observable<Blob> {
    // Gọi API download-folder kèm theo responseType: 'blob'
    return this.http.get(`${this.apiUrl}/${id}/download-folder`, { responseType: 'blob' });
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

  renameItem(id: number, newName: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/rename`, { fileName: newName });
  }

  moveItem(id: number, targetParentId: number | null): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/move`, { targetParentId });
  }

  copyItem(id: number, targetParentId: number | null): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/copy`, { targetParentId });
  }

  getTrash(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/trash`);
  }

  restoreFromTrash(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/restore`, {});
  }

  permanentlyDelete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}/permanent`);
  }

  emptyTrash(): Observable<any> {
    return this.http.delete(`${this.apiUrl}/trash/empty`);
  }

  shareFile(id: number, payload: any = {}): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/share`, payload);
  }

  getAllGlobalFiles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/list/all`);
  }
}