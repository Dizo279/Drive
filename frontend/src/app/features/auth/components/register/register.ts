import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ConfirmDialogService } from '@core/services/confirm-dialog.service';

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
  hideConfirmPassword: boolean = true;
  loading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private dialogService: ConfirmDialogService
  ) {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.loading = true;
      this.errorMsg = '';

      const registerData = {
        fullName: this.registerForm.value.fullName,
        email: this.registerForm.value.email,
        username: this.registerForm.value.username,
        password: this.registerForm.value.password
      };

      this.authService.register(registerData).subscribe({
        next: async () => {
          this.loading = false;
          await this.dialogService.alert({
            title: 'Đăng ký thành công!',
            message: 'Tài khoản của bạn đã được tạo. Bạn có thể đăng nhập ngay.',
            type: 'success'
          });
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          this.errorMsg = err.error || 'Lỗi đăng ký. Email hoặc username có thể đã tồn tại.';
        }
      });
    } else {
      this.registerForm.markAllAsTouched();
    }
  }

  passwordMatchValidator(form: FormGroup) {
    return form.get('password')?.value === form.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  togglePassword() {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPassword() {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }
}