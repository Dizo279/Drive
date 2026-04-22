import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';

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

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

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

  revokeAccess(shareId: number) {
    if(confirm('Bạn có chắc chắn muốn thu hồi quyền truy cập này không?')) {
      // Đã gỡ bỏ this.getHeaders()
      this.http.delete(`${this.apiUrl}/revoke-share/${shareId}`).subscribe({
        next: () => this.loadData(),
        error: () => alert('Lỗi khi thu hồi quyền truy cập.')
      });
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