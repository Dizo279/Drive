// src/app/core/services/preview.service.ts
import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface PreviewState {
  isOpen: boolean;
  fileId: number | null;
  fileName: string;
  mimeType: string;
  fileBlob: Blob | null;
  loading: boolean;
  error: string | null;
}

const initialState: PreviewState = {
  isOpen: false,
  fileId: null,
  fileName: '',
  mimeType: '',
  fileBlob: null,
  loading: false,
  error: null
};

@Injectable({
  providedIn: 'root'
})
export class PreviewService {
  private _state = signal<PreviewState>(initialState);
  private http = inject(HttpClient);

  readonly state = this._state.asReadonly();

  open(file: { id: number; name: string; mimeType: string }): void {
    this._state.update(state => ({
      ...state,
      isOpen: true,
      loading: true,
      fileId: file.id,
      fileName: file.name,
      mimeType: file.mimeType,
      error: null,
      fileBlob: null
    }));

    this.http.get(`http://localhost:8080/api/files/${file.id}/download`, {
      responseType: 'blob'
    }).subscribe({
      next: (blob: Blob) => {
        this._state.update(state => ({
          ...state,
          fileBlob: blob,
          loading: false
        }));
      },
      error: (err) => {
        let errorMessage = 'Không thể hiển thị file. Vui lòng thử lại.';

        if (err.status === 404) {
          errorMessage = 'File không tồn tại hoặc đã bị xóa.';
        } else if (err.status === 403) {
          errorMessage = 'Bạn không có quyền xem file này.';
        } else if (err.status === 0) {
          errorMessage = 'Không thể tải file. Vui lòng kiểm tra kết nối.';
        }

        this._state.update(state => ({
          ...state,
          loading: false,
          error: errorMessage
        }));
      }
    });
  }

  close(): void {
    this._state.set(initialState);
  }
}
