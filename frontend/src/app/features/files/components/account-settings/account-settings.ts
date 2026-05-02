import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ConfirmDialogService } from '@core/services/confirm-dialog.service';

@Component({
  selector: 'app-account-settings',
  templateUrl: './account-settings.html',
  styleUrls: ['./account-settings.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink]
})
export class AccountSettingsComponent implements OnInit {
  user: any = {
    fullName: '',
    avatarUrl: '',
    username: '',
    email: '',
    tier: '',
    usedQuota: 0,
    maxQuota: 1073741824
  };

  securityData = {
    username: '',
    email: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  
  loading = false;
  successMsg = '';
  errorMsg = '';

  private apiUrl = 'http://localhost:8080/api/users';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private dialogService: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.fetchProfile();
    }
  }

  // Hàm tiện ích để tự động lấy JWT Token từ LocalStorage
  private getHeaders() {
    let token = '';
    // Kẹp điều kiện kiểm tra môi trường trình duyệt
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      token = localStorage.getItem('jwt_token') || '';
    }
    return { headers: new HttpHeaders().set('Authorization', `Bearer ${token}`) };
  }

  fetchProfile() {
    this.http.get(`${this.apiUrl}/me`, this.getHeaders()).subscribe({
      next: (data: any) => {
        this.user = data;
        this.securityData.username = data.username;
        this.securityData.email = data.email;
        this.cdr.detectChanges(); // 3. Ép giao diện cập nhật thông tin từ DB
      },
      error: () => this.errorMsg = 'Không thể tải dữ liệu.'
    });
  }

  onAvatarSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.user.avatarUrl = e.target.result;
        this.cdr.detectChanges(); // 4. Ép giao diện hiển thị ảnh preview ngay lập tức
      };
      reader.readAsDataURL(file);
    }
  }

  onUpdateProfile() {
    this.loading = true;
    this.successMsg = '';
    this.errorMsg = '';

    // Kiểm tra mật khẩu xác nhận
    if (this.securityData.newPassword && this.securityData.newPassword !== this.securityData.confirmPassword) {
      this.errorMsg = 'Mật khẩu xác nhận không khớp!';
      this.loading = false;
      return;
    }

    // Đóng gói dữ liệu gửi lên Backend
    const updateData = {
      fullName: this.user.fullName,
      avatarUrl: this.user.avatarUrl,
      username: this.securityData.username,
      email: this.securityData.email,
      currentPassword: this.securityData.currentPassword,
      newPassword: this.securityData.newPassword
    };

    this.http.put(`${this.apiUrl}/profile`, updateData, this.getHeaders()).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.successMsg = 'Hồ sơ đã được lưu thành công!';
        this.user = res;
        this.securityData.currentPassword = '';
        this.securityData.newPassword = '';
        this.securityData.confirmPassword = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        
        // Bắt đầu bóc tách lỗi thông minh
        if (err.status === 400) {
          // Trường hợp 1: Backend gửi về object JSON có chứa trường "error"
          if (err.error && err.error.error) {
            this.errorMsg = err.error.error;
          }
          // Trường hợp 2: Backend gửi về một chuỗi Text thuần túy
          else if (typeof err.error === 'string') {
            try {
              const parsed = JSON.parse(err.error);
              this.errorMsg = parsed.error || 'Mật khẩu hiện tại không đúng!';
            } catch (e) {
              this.errorMsg = err.error; // Lấy luôn chuỗi text đó
            }
          }
          // Trường hợp 3: Fallback mặc định
          else {
            this.errorMsg = 'Mật khẩu hiện tại không đúng hoặc bị bỏ trống!';
          }
        } else {
          // Bắt các lỗi khác như 500 (sập server) hoặc 404
          this.errorMsg = 'Đã có lỗi xảy ra từ máy chủ. Vui lòng thử lại sau.';
        }
        
        this.cdr.detectChanges(); // Ép giao diện hiển thị dòng chữ đỏ ngay lập tức
      }
    });
  }


  formatSize(bytes: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  async requestUpgrade(): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Nâng cấp PREMIUM',
      message: 'Bạn có muốn gửi yêu cầu nâng cấp lên PREMIUM (100GB)?',
      confirmText: 'Gửi yêu cầu',
      type: 'info'
    });
    if (!confirmed) return;
    this.http.post(`${this.apiUrl}/upgrade-request`, {}, this.getHeaders()).subscribe({
      next: (res: any) => {
        this.successMsg = res.message || 'Yêu cầu nâng cấp đã được gửi!';
        this.errorMsg = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMsg = err.error?.error || 'Không thể gửi yêu cầu nâng cấp.';
        this.successMsg = '';
        this.cdr.detectChanges();
      }
    });
  }
}
