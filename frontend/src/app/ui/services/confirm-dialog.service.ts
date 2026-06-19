import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

export interface DialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'danger' | 'warning' | 'info' | 'success';
  /** If true, only shows an OK button (no Cancel) — behaves like alert() */
  alertOnly?: boolean;
  isPrompt?: boolean;
  promptValue?: string;
  promptPlaceholder?: string;
}

export interface DialogState {
  isOpen: boolean;
  config: DialogConfig;
  resolve: ((value: any) => void) | null;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly state = signal<DialogState>({
    isOpen: false,
    config: { title: '', message: '' },
    resolve: null
  });

  /**
   * Hiển thị hộp thoại nhập liệu (thay thế prompt()).
   * @returns Promise<string | null> — string nếu người dùng nhập và Xác nhận, null nếu Hủy.
   */
  prompt(config: DialogConfig): Promise<string | null> {
    return new Promise<string | null>((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          confirmText: 'Xác nhận',
          cancelText: 'Hủy',
          type: 'info',
          alertOnly: false,
          isPrompt: true,
          promptValue: '',
          promptPlaceholder: 'Nhập nội dung...',
          ...config
        },
        resolve
      });
    });
  }

  /**
   * Hiển thị hộp thoại xác nhận (thay thế confirm()).
   * @returns Promise<boolean> — true nếu người dùng nhấn Xác nhận, false nếu Hủy.
   */
  confirm(config: DialogConfig): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          confirmText: 'Xác nhận',
          cancelText: 'Hủy',
          type: 'danger',
          alertOnly: false,
          ...config
        },
        resolve
      });
    });
  }

  /**
   * Hiển thị hộp thoại thông báo (thay thế alert()).
   * @returns Promise<void>
   */
  alert(config: Omit<DialogConfig, 'alertOnly'>): Promise<void> {
    return new Promise<void>((resolve) => {
      this.state.set({
        isOpen: true,
        config: {
          confirmText: 'Đã hiểu',
          type: 'info',
          alertOnly: true,
          ...config
        },
        resolve: () => resolve()
      });
    });
  }

  /** Gọi nội bộ bởi component khi người dùng nhấn nút */
  respond(value: any): void {
    const current = this.state();
    if (current.resolve) {
      current.resolve(value);
    }
    this.state.set({ isOpen: false, config: { title: '', message: '' }, resolve: null });
  }
}
