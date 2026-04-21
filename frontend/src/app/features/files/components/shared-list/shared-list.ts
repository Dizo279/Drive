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
  
  private apiUrl = 'http://localhost:8080/api/files/shared';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.loadData();
    }
  }

  private getHeaders() {
    // Tự động tìm token dù bạn có lưu dưới tên 'token' hay 'jwt_token'
    const token = localStorage.getItem('token') || localStorage.getItem('jwt_token') || '';
    
    return {
      headers: new HttpHeaders().set('Authorization', `Bearer ${token}`)
    };
  }

  setTab(tab: 'by-me' | 'with-me') {
    this.activeTab = tab;
    this.loadData();
  }

  loadData() {
    this.loading = true;
    const endpoint = this.activeTab === 'by-me' ? `${this.apiUrl}/by-me` : `${this.apiUrl}/with-me`;
    
    this.http.get(endpoint, this.getHeaders()).subscribe({
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
      this.http.delete(`${this.apiUrl}/${shareId}`, this.getHeaders()).subscribe({
        next: () => this.loadData(), // Tải lại danh sách sau khi xóa
        error: () => alert('Lỗi khi thu hồi quyền truy cập.')
      });
    }
  }

  downloadShared(token: string) {
    window.open(`http://localhost:8080/api/files/shared/${token}`, '_blank');
  }

  isExpired(expiresAt: string): boolean {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  }
}