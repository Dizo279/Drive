import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit {
  allUsers: any[] = [];
  filteredUsers: any[] = [];
  loading = false;
  
  // Controls
  searchTerm: string = '';
  sortBy: string = 'name';

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadUsers();
    }
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

  toggleRole(user: any) {
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    if (confirm(`Change ${user.username}'s role to ${newRole}?`)) {
      this.adminService.updateRole(user.id, newRole).subscribe(() => this.loadUsers());
    }
  }

  toggleTier(user: any) {
    const newTier = user.tier === 'PRO' ? 'FREE' : 'PRO';
    if (confirm(`Change ${user.username}'s tier to ${newTier}?`)) {
      this.adminService.updateTier(user.id, newTier).subscribe(() => this.loadUsers());
    }
  }

  deleteUser(user: any) {
    if (confirm(`Permanently delete account: ${user.username}?`)) {
      this.adminService.deleteUser(user.id).subscribe(() => this.loadUsers());
    }
  }
}