import { Injectable, inject, signal } from '@angular/core';
import { FileService } from '@features/files/services/file.service';
import { Subscription } from 'rxjs';

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
  private readonly _state = signal<PreviewState>(initialState);
  private readonly fileService = inject(FileService);
  private downloadSubscription: Subscription | null = null;
  private requestVersion = 0;

  readonly state = this._state.asReadonly();

  open(file: { id: number; name: string; mimeType: string }): void {
    const requestVersion = ++this.requestVersion;
    this.downloadSubscription?.unsubscribe();

    this._state.set({
      isOpen: true,
      loading: true,
      fileId: file.id,
      fileName: file.name,
      mimeType: file.mimeType,
      fileBlob: null,
      error: null
    });

    this.downloadSubscription = this.fileService.downloadFile(file.id).subscribe({
      next: (blob: Blob) => {
        if (requestVersion !== this.requestVersion) return;

        this._state.update(state => ({
          ...state,
          fileBlob: blob,
          loading: false
        }));
      },
      error: (err) => {
        if (requestVersion !== this.requestVersion) return;

        this._state.update(state => ({
          ...state,
          loading: false,
          error: this.getErrorMessage(err?.status)
        }));
      }
    });
  }

  close(): void {
    this.requestVersion++;
    this.downloadSubscription?.unsubscribe();
    this.downloadSubscription = null;
    this._state.set(initialState);
  }

  download(): void {
    const state = this._state();
    if (!state.fileBlob || typeof document === 'undefined') return;

    const url = globalThis.URL.createObjectURL(state.fileBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = state.fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    globalThis.URL.revokeObjectURL(url);
  }

  private getErrorMessage(status?: number): string {
    switch (status) {
      case 404:
        return 'File không tồn tại hoặc đã bị xóa.';
      case 403:
        return 'Bạn không có quyền xem file này.';
      case 0:
        return 'Không thể tải file. Vui lòng kiểm tra kết nối.';
      case 413:
      case 504:
        return 'File quá lớn để preview. Vui lòng tải xuống.';
      default:
        return 'Không thể hiển thị file. Vui lòng thử lại.';
    }
  }
}
