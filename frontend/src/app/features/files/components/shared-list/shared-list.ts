import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ConfirmDialogService } from '@core/services/confirm-dialog.service';

@Component({
  selector: 'app-shared-list',
  templateUrl: 'shared-list.html',
  styleUrls: ['shared-list.css'],
  standalone: true,
  imports: [CommonModule, RouterLink]
})
export class SharedListComponent implements OnInit {
  activeTab: 'by-me' | 'with-me' = 'by-me';
  sharedItems: any[] = [];
  loading: boolean = false;
  
  private apiUrl = 'http://localhost:8080/api/files';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private dialogService: ConfirmDialogService) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.loadData();
    }
  }


  setTab(tab: 'by-me' | 'with-me') {
    this.activeTab = tab;
    this.loadData();
  }

  loadData() {
    this.loading = true;
    const endpoint = this.activeTab === 'by-me' ? `${this.apiUrl}/list/shared-by-me` : `${this.apiUrl}/list/shared-with-me`;
    
    // Đã gỡ bỏ this.getHeaders()
    this.http.get(endpoint).subscribe({
      next: (data: any) => {
        this.sharedItems = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => this.loading = false
    });
  }

  async revokeAccess(shareId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Thu hồi quyền truy cập',
      message: 'Bạn có chắc chắn muốn thu hồi quyền truy cập này không?',
      confirmText: 'Thu hồi',
      type: 'warning'
    });
    if (!confirmed) return;
    this.http.delete(`${this.apiUrl}/revoke-share/${shareId}`).subscribe({
      next: () => this.loadData(),
      error: () => this.dialogService.alert({ title: 'Lỗi', message: 'Lỗi khi thu hồi quyền truy cập.', type: 'danger' })
    });
  }

  async deleteAccess(shareId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Xóa chia sẻ',
      message: 'Bạn có chắc chắn muốn xóa mục chia sẻ đã hết hạn này không?',
      confirmText: 'Xóa',
      type: 'danger'
    });
    if (!confirmed) return;
    this.http.delete(`${this.apiUrl}/revoke-share/${shareId}`).subscribe({
      next: () => this.loadData(),
      error: () => this.dialogService.alert({ title: 'Lỗi', message: 'Lỗi khi xóa mục chia sẻ.', type: 'danger' })
    });
  }

  copyLink(token: string) {
    const link = `http://localhost:8080/api/files/shared/${token}`;
    if (navigator.clipboard) {
        navigator.clipboard.writeText(link).then(() => {
            this.dialogService.alert({ title: 'Thành công', message: 'Đã sao chép liên kết vào clipboard', type: 'success' });
        });
    } else {
        // Fallback for older browsers or non-secure contexts if needed
        const textArea = document.createElement("textarea");
        textArea.value = link;
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand('copy');
            this.dialogService.alert({ title: 'Thành công', message: 'Đã sao chép liên kết vào clipboard', type: 'success' });
        } catch (err) {
            this.dialogService.alert({ title: 'Lỗi', message: 'Không thể sao chép liên kết.', type: 'danger' });
        }
        document.body.removeChild(textArea);
    }
  }

  downloadShared(token: string) {
    // Tạo thẻ <a> ẩn để ép trình duyệt tải file mà không mở tab lỗi rác
    const link = document.createElement('a');
    link.href = `http://localhost:8080/api/files/shared/${token}`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  isExpired(expiresAt: string): boolean {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  }
}