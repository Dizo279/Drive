import { Routes } from '@angular/router';
import { LoginComponent } from '@features/auth/components/login/login';
import { RegisterComponent } from '@features/auth/components/register/register';
import { FileUploadComponent } from '@features/files/components/file-upload/file-upload';
import { FileListComponent } from '@features/files/components/file-list/file-list';
import { AccountSettingsComponent } from '@features/account/components/account-settings/account-settings';
import { SharedListComponent } from '@features/files/components/shared-list/shared-list';
import { AdminDashboardComponent } from '@features/admin/components/admin-dashboard/admin-dashboard';
import { TrashListComponent } from '@features/files/components/trash-list/trash-list';
import { authGuard, noAuthGuard } from '@core/guards/auth.guard';
import { adminGuard } from '@core/guards/admin.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [noAuthGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [noAuthGuard] },
  { path: 'files', component: FileListComponent, canActivate: [authGuard] },
  { path: 'files/upload', component: FileUploadComponent, canActivate: [authGuard] },
  { path: 'settings', component: AccountSettingsComponent, canActivate: [authGuard] },
  { path: 'shared', component: SharedListComponent, canActivate: [authGuard] },
  { path: 'trash', component: TrashListComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [adminGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];
