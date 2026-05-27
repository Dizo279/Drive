import { Routes } from '@angular/router';
import { LoginComponent } from '@features/auth/components/login/login';
import { RegisterComponent } from '@features/auth/components/register/register';
import { FileUploadComponent } from '@features/files/components/file-upload/file-upload';
import { FileListComponent } from '@features/files/components/file-list/file-list';
import { AccountSettingsComponent } from '@features/account/components/account-settings/account-settings';
import { SharedListComponent } from '@features/files/components/shared-list/shared-list';
import { AdminDashboardComponent } from '@features/admin/components/admin-dashboard/admin-dashboard';
import { TrashListComponent } from '@features/files/components/trash-list/trash-list';



export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'files', component: FileListComponent }, // MỚI THÊM
  { path: 'files/upload', component: FileUploadComponent },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'settings', component: AccountSettingsComponent },
  { path: 'shared', component: SharedListComponent },
  { path: 'trash', component: TrashListComponent },
  { path: 'admin', component: AdminDashboardComponent }

];
