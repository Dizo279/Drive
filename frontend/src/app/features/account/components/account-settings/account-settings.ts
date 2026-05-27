import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

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

  // HÃ m tiá»‡n Ã­ch Ä‘á»ƒ tá»± Ä‘á»™ng láº¥y JWT Token tá»« LocalStorage
  private getHeaders() {
    let token = '';
    // Káº¹p Ä‘iá»u kiá»‡n kiá»ƒm tra mÃ´i trÆ°á»ng trÃ¬nh duyá»‡t
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
        this.cdr.detectChanges(); // 3. Ã‰p giao diá»‡n cáº­p nháº­t thÃ´ng tin tá»« DB
      },
      error: () => this.errorMsg = 'KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u.'
    });
  }

  onAvatarSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.user.avatarUrl = e.target.result;
        this.cdr.detectChanges(); // 4. Ã‰p giao diá»‡n hiá»ƒn thá»‹ áº£nh preview ngay láº­p tá»©c
      };
      reader.readAsDataURL(file);
    }
  }

  onUpdateProfile() {
    this.loading = true;
    this.successMsg = '';
    this.errorMsg = '';

    // Kiá»ƒm tra máº­t kháº©u xÃ¡c nháº­n
    if (this.securityData.newPassword && this.securityData.newPassword !== this.securityData.confirmPassword) {
      this.errorMsg = 'Máº­t kháº©u xÃ¡c nháº­n khÃ´ng khá»›p!';
      this.loading = false;
      return;
    }

    // ÄÃ³ng gÃ³i dá»¯ liá»‡u gá»­i lÃªn Backend
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
        this.successMsg = 'Há»“ sÆ¡ Ä‘Ã£ Ä‘Æ°á»£c lÆ°u thÃ nh cÃ´ng!';
        this.user = res;
        this.securityData.currentPassword = '';
        this.securityData.newPassword = '';
        this.securityData.confirmPassword = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        
        // Báº¯t Ä‘áº§u bÃ³c tÃ¡ch lá»—i thÃ´ng minh
        if (err.status === 400) {
          // TrÆ°á»ng há»£p 1: Backend gá»­i vá» object JSON cÃ³ chá»©a trÆ°á»ng "error"
          if (err.error && err.error.error) {
            this.errorMsg = err.error.error;
          }
          // TrÆ°á»ng há»£p 2: Backend gá»­i vá» má»™t chuá»—i Text thuáº§n tÃºy
          else if (typeof err.error === 'string') {
            try {
              const parsed = JSON.parse(err.error);
              this.errorMsg = parsed.error || 'Máº­t kháº©u hiá»‡n táº¡i khÃ´ng Ä‘Ãºng!';
            } catch (e) {
              this.errorMsg = err.error; // Láº¥y luÃ´n chuá»—i text Ä‘Ã³
            }
          }
          // TrÆ°á»ng há»£p 3: Fallback máº·c Ä‘á»‹nh
          else {
            this.errorMsg = 'Máº­t kháº©u hiá»‡n táº¡i khÃ´ng Ä‘Ãºng hoáº·c bá»‹ bá» trá»‘ng!';
          }
        } else {
          // Báº¯t cÃ¡c lá»—i khÃ¡c nhÆ° 500 (sáº­p server) hoáº·c 404
          this.errorMsg = 'ÄÃ£ cÃ³ lá»—i xáº£y ra tá»« mÃ¡y chá»§. Vui lÃ²ng thá»­ láº¡i sau.';
        }
        
        this.cdr.detectChanges(); // Ã‰p giao diá»‡n hiá»ƒn thá»‹ dÃ²ng chá»¯ Ä‘á» ngay láº­p tá»©c
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
      title: 'NÃ¢ng cáº¥p PREMIUM',
      message: 'Báº¡n cÃ³ muá»‘n gá»­i yÃªu cáº§u nÃ¢ng cáº¥p lÃªn PREMIUM (100GB)?',
      confirmText: 'Gá»­i yÃªu cáº§u',
      type: 'info'
    });
    if (!confirmed) return;
    this.http.post(`${this.apiUrl}/upgrade-request`, {}, this.getHeaders()).subscribe({
      next: (res: any) => {
        this.successMsg = res.message || 'YÃªu cáº§u nÃ¢ng cáº¥p Ä‘Ã£ Ä‘Æ°á»£c gá»­i!';
        this.errorMsg = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMsg = err.error?.error || 'KhÃ´ng thá»ƒ gá»­i yÃªu cáº§u nÃ¢ng cáº¥p.';
        this.successMsg = '';
        this.cdr.detectChanges();
      }
    });
  }
}

