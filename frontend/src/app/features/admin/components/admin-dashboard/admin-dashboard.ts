import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID, OnDestroy, NgZone } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  allUsers: any[] = [];
  filteredUsers: any[] = [];
  loading = false;
  
  // Controls
  searchTerm: string = '';
  sortBy: string = 'name';
  
  // Upgrade Requests
  upgradeRequests: any[] = [];
  sseEventSource: EventSource | null = null;
  hasNewNotification: boolean = false;
  activeTab: 'users' | 'requests' = 'users';

  // Edit Panel
  selectedUserForEdit: any = null;
  editRole: string = '';
  editTier: string = '';

  // Delete Panel
  userToDelete: any = null;

  // Stats
  stats = {
    totalUsers: 0,
    totalAdmins: 0,
    totalStorageUsed: 0
  };

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object,
    private zone: NgZone,
    private dialogService: ConfirmDialogService
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUsers();
      this.loadStats();
      this.loadUpgradeRequests();
      this.connectToSSE();
    }
  }

  loadStats() {
    this.adminService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Lỗi tải thống kê:', err)
    });
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: (data) => {
        this.allUsers = data;
        this.applyFilters();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onSearch() {
    this.applyFilters();
  }

  onSort() {
    this.applyFilters();
  }

  applyFilters() {
    // 1. Search Logic
    let result = this.allUsers.filter(u =>
      (u.fullName || u.username || '').toLowerCase().includes(this.searchTerm.toLowerCase()) ||
      (u.email || '').toLowerCase().includes(this.searchTerm.toLowerCase())
    );

    // 2. Sort Logic
    result.sort((a, b) => {
      const valA = (a[this.sortBy] || '').toString().toLowerCase();
      const valB = (b[this.sortBy] || '').toString().toLowerCase();
      return valA.localeCompare(valB);
    });

    this.filteredUsers = result;
  }

  // --- EDIT USER PANEL ---
  openEditPanel(user: any) {
    this.selectedUserForEdit = user;
    this.editRole = user.role || 'USER';
    this.editTier = user.tier || 'FREE';
    this.cdr.detectChanges();
  }

  closeEditPanel() {
    this.selectedUserForEdit = null;
    this.editRole = '';
    this.editTier = '';
    this.cdr.detectChanges();
  }

  saveUserChanges() {
    if (!this.selectedUserForEdit) return;
    
    const userId = this.selectedUserForEdit.id;
    const roleChanged = this.editRole !== this.selectedUserForEdit.role;
    const tierChanged = this.editTier !== this.selectedUserForEdit.tier;

    // Update role if changed
    const roleUpdate = roleChanged 
      ? this.adminService.updateRole(userId, this.editRole)
      : null;

    // Update tier if changed
    const tierUpdate = tierChanged
      ? this.adminService.updateTier(userId, this.editTier)
      : null;

    // Execute updates
    if (roleUpdate && tierUpdate) {
      roleUpdate.subscribe(() => {
        tierUpdate.subscribe(() => {
          this.closeEditPanel();
          this.loadUsers();
        });
      });
    } else if (roleUpdate) {
      roleUpdate.subscribe(() => {
        this.closeEditPanel();
        this.loadUsers();
      });
    } else if (tierUpdate) {
      tierUpdate.subscribe(() => {
        this.closeEditPanel();
        this.loadUsers();
      });
    } else {
      this.closeEditPanel();
    }
  }

  // --- DELETE USER PANEL ---
  openDeletePanel(user: any) {
    this.userToDelete = user;
    this.cdr.detectChanges();
  }

  closeDeletePanel() {
    this.userToDelete = null;
    this.cdr.detectChanges();
  }

  confirmDelete() {
    if (!this.userToDelete) return;
    this.adminService.deleteUser(this.userToDelete.id).subscribe({
      next: () => {
        this.closeDeletePanel();
        this.loadUsers();
      },
      error: () => {
        this.dialogService.alert({ title: 'Lỗi', message: 'Xóa người dùng thất bại.', type: 'danger' });
      }
    });
  }

  // --- UPGRADE REQUESTS MANAGEMENT ---

  switchTab(tab: 'users' | 'requests'): void {
    this.activeTab = tab;
    if (tab === 'requests') {
      this.hasNewNotification = false;
    }
  }

  connectToSSE(): void {
    const token = localStorage.getItem('jwt_token');
    if (!token) return;

    this.sseEventSource = new EventSource(`http://localhost:8080/api/admin/sse/notifications?token=${token}`);

    this.sseEventSource.addEventListener('upgrade-request', (event) => {
      this.zone.run(() => {
        try {
          const request = JSON.parse(event.data);
          this.upgradeRequests.unshift(request);
          this.hasNewNotification = true;
          this.cdr.detectChanges();
        } catch (e) {
          console.error('Lỗi parse SSE data:', e);
        }
      });
    });

    this.sseEventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };
  }

  loadUpgradeRequests(): void {
    this.adminService.getUpgradeRequests().subscribe({
      next: (data) => {
        this.zone.run(() => {
          this.upgradeRequests = data;
          this.cdr.detectChanges();
        });
      },
      error: (err) => console.error('Lỗi tải yêu cầu nâng cấp:', err)
    });
  }

  async approveRequest(requestId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Phê duyệt yêu cầu',
      message: 'Bạn có chắc muốn PHÊ DUYỆT yêu cầu nâng cấp này?',
      confirmText: 'Phê duyệt',
      type: 'success'
    });
    if (!confirmed) return;
    this.adminService.processUpgradeRequest(requestId, 'APPROVE').subscribe({
      next: () => {
        this.zone.run(() => {
          this.upgradeRequests = this.upgradeRequests.filter(r => r.id !== requestId);
          this.cdr.detectChanges();
        });
      },
      error: () => this.dialogService.alert({ title: 'Lỗi', message: 'Lỗi khi phê duyệt yêu cầu.', type: 'danger' })
    });
  }

  async rejectRequest(requestId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Từ chối yêu cầu',
      message: 'Bạn có chắc muốn TỪ CHỐI yêu cầu nâng cấp này?',
      confirmText: 'Từ chối',
      type: 'danger'
    });
    if (!confirmed) return;
    this.adminService.processUpgradeRequest(requestId, 'REJECT').subscribe({
      next: () => {
        this.zone.run(() => {
          this.upgradeRequests = this.upgradeRequests.filter(r => r.id !== requestId);
          this.cdr.detectChanges();
        });
      },
      error: () => this.dialogService.alert({ title: 'Lỗi', message: 'Lỗi khi từ chối yêu cầu.', type: 'danger' })
    });
  }

  ngOnDestroy(): void {
    if (this.sseEventSource) {
      this.sseEventSource.close();
      this.sseEventSource = null;
    }
  }
}

