import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; 
import { Router, RouterLink } from '@angular/router'; 
import { AuthService } from '../../auth';

@Component({
  selector: 'app-register',
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, CommonModule, RouterLink] 
})
export class RegisterComponent {
  registerForm: FormGroup;
  
  // Các biến điều khiển UI
  errorMsg: string = ''; 
  hidePassword: boolean = true; 
  loading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.loading = true;
      this.errorMsg = '';

      this.authService.register(this.registerForm.value).subscribe({
        next: () => {
          this.loading = false;
          alert('Đăng ký thành công! Bạn có thể đăng nhập ngay.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          this.errorMsg = err.error?.message || err.error || 'Lỗi đăng ký. Có thể username đã tồn tại.';
        }
      });
    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}