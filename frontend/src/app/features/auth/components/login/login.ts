import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; // Cung cấp *ngIf
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; // Cung cấp routerLink
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  standalone: true,
  // Import đầy đủ các module cần thiết cho HTML
  imports: [ReactiveFormsModule, FormsModule, CommonModule, RouterLink]
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  
  // Khai báo các biến trạng thái mới cho giao diện
  errorMsg: string = '';
  hidePassword: boolean = true;
  rememberMe: boolean = false;
  loading: boolean = false;
  returnUrl: string = '/files';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Get return url from route parameters or default to '/files'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/files';
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.loading = true; // Bật hiệu ứng loading
      this.errorMsg = '';  // Xóa lỗi cũ

      this.authService.login(this.loginForm.value).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigateByUrl(this.returnUrl);
        },
        error: (err) => {
          this.loading = false; // Tắt loading khi có lỗi
          // Lấy tin nhắn lỗi từ Backend, nếu không có thì dùng câu mặc định
          this.errorMsg = err.error?.message || err.error || 'Sai tài khoản hoặc mật khẩu';
        }
      });
    } else {
      // Đánh dấu toàn bộ form là đã chạm vào để hiện màu đỏ nếu người dùng bấm submit khi form trống
      this.loginForm.markAllAsTouched();
    }
  }
}