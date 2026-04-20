import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/components/login/login';
import { RegisterComponent } from './features/auth/components/register/register';
import { FileUploadComponent } from './features/files/components/file-upload/file-upload';
import { FileListComponent } from './features/files/components/file-list/file-list';
import { AccountSettingsComponent } from './features/files/components/account-settings/account-settings';


export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'files', component: FileListComponent }, // MỚI THÊM
  { path: 'files/upload', component: FileUploadComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'settings', component: AccountSettingsComponent }
];
