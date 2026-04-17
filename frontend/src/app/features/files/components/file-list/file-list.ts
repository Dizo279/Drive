import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { FileService } from '../../file';
import { AuthService } from '../../../auth/auth';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-file-list',
  templateUrl: './file-list.html',
  styleUrls: ['./file-list.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink]
})
export class FileListComponent implements OnInit {
  // Core Data
  allFiles: any[] = []; // Holds the original, unfiltered list
  files: any[] = [];    // Holds the currently displayed (filtered/sorted/searched) list
  loading: boolean = true;
  currentUser: any = null;

  // Search & Filter State
  searchQuery: string = '';
  activeFilter: string = 'all';
  sortBy: string = 'date';

  // Quota Information
  usedBytes: number = 0;
  quotaBytes: number = 1073741824; // Default to 1GB
  quotaPercent: number = 0;

  // Notification State (Mocked for now as we don't have a real notif service yet)
  showNotifPanel: boolean = false;
  unreadCount: number = 0;
  notifications: any[] = [];
  notifService = {
      markAllRead: () => { this.unreadCount = 0; this.notifications.forEach(n => n.read = true); },
      markRead: (id: number) => {
          const n = this.notifications.find(n => n.id === id);
          if(n && !n.read) { n.read = true; this.unreadCount--; }
      }
  };

  // --- TRẠNG THÁI MODAL CHIA SẺ ---
  isShareModalOpen: boolean = false;
  shareLink: string = '';

  // UI Configuration Constants
  filters = [
    { key: 'all', label: 'Tất cả' },
    { key: 'image', label: 'Hình ảnh' },
    { key: 'document', label: 'Tài liệu' },
    { key: 'media', label: 'Video' },
    { key: 'other', label: 'Khác' }
  ];

  smartFolders = [
    { key: 'image', label: 'Hình ảnh', icon: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z' },
    { key: 'document', label: 'Tài liệu', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    { key: 'media', label: 'Video & Audio', icon: 'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z' },
    { key: 'other', label: 'Khác', icon: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4' }
  ];

  constructor(
    private fileService: FileService,
    private authService: AuthService,
    private router: Router,
    public cdr: ChangeDetectorRef // Required for forcing updates in templates sometimes
  ) {}

  ngOnInit(): void {
    // if (typeof localStorage !== 'undefined') {
    //   const token = localStorage.getItem('jwt_token');
    //   if (token) {
    //       try {
    //           const payload = JSON.parse(atob(token.split('.')[1]));
    //           this.currentUser = { username: payload.sub, role: 'USER' };
    //       } catch (e) {
    //           this.currentUser = { username: 'User', role: 'USER' };
    //       }
    //   }
    // }
    // this.loadData();
    // CHỈ CHẠY LOGIC NÀY KHI MÔI TRƯỜNG LÀ TRÌNH DUYỆT (có tồn tại window)
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      const token = localStorage.getItem('jwt_token');
      if (token) {
          try {
              const payload = JSON.parse(atob(token.split('.')[1]));
              this.currentUser = { username: payload.sub, role: 'USER' };
          } catch (e) {
              this.currentUser = { username: 'User', role: 'USER' };
          }
      }
      
      // Chuyển hàm gọi API vào đây để nó không bao giờ chạy ngầm trên Server nữa
      this.loadData();
    }
  }

  loadData(): void {
    this.loading = true;
    this.fileService.getFiles().subscribe({
      next: (data) => {
        this.allFiles = data.map(f => ({
            id: f.id,
            name: f.fileName,
            sizeBytes: f.fileSize,
            createdAt: f.createdAt,
            mimeType: f.mimeType || this.guessMimeType(f.fileName)
        }));
        this.applyFiltersAndSort();
        this.calculateUsedQuota();
        this.loading = false;
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to load files', err);
        this.loading = false;
        if (typeof window !== 'undefined') {
          alert('Lỗi khi tải danh sách file.');
        }
      }
    });

    this.fileService.getQuota().subscribe({
        next: (data) => {
            if(data && data.usedQuota !== undefined) {
                this.usedBytes = data.usedQuota;
                this.quotaBytes = data.maxQuota;
                this.quotaPercent = data.percentage;
            }
        },
        error: (err) => console.log("Could not load real quota, using fallback calculation.")
    });
  }

  // --- Filtering & Sorting Logic ---

  setFilter(filterKey: string): void {
    this.activeFilter = filterKey;
    this.applyFiltersAndSort();
  }

  setSort(sortKey: string): void {
    this.sortBy = sortKey;
    this.applyFiltersAndSort();
  }

  onSearch(): void {
    this.applyFiltersAndSort();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applyFiltersAndSort();
  }

  applyFiltersAndSort(): void {
    let result = [...this.allFiles];

    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(f => f.name.toLowerCase().includes(q));
    }

    if (this.activeFilter !== 'all') {
      result = result.filter(f => this.getCategory(f.mimeType) === this.activeFilter);
    }

    result.sort((a, b) => {
      if (this.sortBy === 'name') {
        return a.name.localeCompare(b.name);
      } else if (this.sortBy === 'size') {
        return b.sizeBytes - a.sizeBytes;
      } else {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });

    this.files = result;
    this.cdr.detectChanges();
  }

  // --- Helper Methods ---

  getCategory(mimeType: string): string {
    if (!mimeType) return 'document';
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.startsWith('video/') || mimeType.startsWith('audio/')) return 'media';
    if (mimeType.includes('zip') || mimeType.includes('rar') || mimeType.includes('tar') || mimeType.includes('gzip')) return 'archive';
    return 'document';
  }

  guessMimeType(fileName: string): string {
      const ext = fileName.split('.').pop()?.toLowerCase();
      switch(ext) {
          case 'jpg': case 'jpeg': case 'png': case 'gif': case 'webp': return 'image/jpeg';
          case 'mp4': case 'mov': case 'avi': return 'video/mp4';
          case 'mp3': case 'wav': return 'audio/mpeg';
          case 'zip': case 'rar': case '7z': return 'application/zip';
          case 'pdf': return 'application/pdf';
          default: return 'application/octet-stream';
      }
  }

  getFolderCount(categoryKey: string): number {
    return this.allFiles.filter(f => this.getCategory(f.mimeType) === categoryKey).length;
  }

  getFolderSize(categoryKey: string): string {
    const totalBytes = this.allFiles
      .filter(f => this.getCategory(f.mimeType) === categoryKey)
      .reduce((sum, f) => sum + f.sizeBytes, 0);
    return this.formatSize(totalBytes);
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  calculateUsedQuota(): void {
      this.usedBytes = this.allFiles.reduce((sum, f) => sum + f.sizeBytes, 0);
      this.quotaPercent = Math.min(100, Math.round((this.usedBytes / this.quotaBytes) * 100));
  }

  highlightMatch(text: string, query: string): string {
    if (!query) return text;
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }

  // --- Actions ---

  viewDetail(file: any): void {
    console.log('Viewing details for:', file);
  }

  download(file: any): void {
    this.fileService.downloadFile(file.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = file.name;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => alert('Tải file thất bại!')
    });
  }

  delete(file: any): void {
    if (confirm(`Bạn có chắc chắn muốn xóa file "${file.name}" vĩnh viễn không?`)) {
      this.fileService.deleteFile(file.id).subscribe({
        next: () => {
          this.allFiles = this.allFiles.filter(f => f.id !== file.id);
          this.applyFiltersAndSort();
          this.calculateUsedQuota();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Lỗi khi xóa file:', err);
          if (typeof window !== 'undefined') {
            alert('Xóa file thất bại. Có thể do lỗi mạng hoặc bạn không có quyền xóa file này.');
          }
        }
      });
    }
  }

  // --- CÁC HÀM XỬ LÝ CHIA SẺ FILE ---
  share(file: any): void {
    this.fileService.shareFile(file.id).subscribe({
      next: (res) => {
        const token = res.shareToken;
        // Tạo đường link public tải file từ Backend
        this.shareLink = `http://localhost:8080/api/files/shared/${token}`; 
        
        // Mở Modal
        this.isShareModalOpen = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Lỗi tạo link:', err);
        if (typeof window !== 'undefined') {
          alert('Không thể tạo link chia sẻ. Vui lòng thử lại.');
        }
      }
    });
  }

  closeShareModal(): void {
    this.isShareModalOpen = false;
    this.shareLink = '';
  }

  copyLink(): void {
    if (typeof window !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(this.shareLink).then(() => {
        alert('Đã copy link vào clipboard! Bạn có thể gửi link này cho bất kỳ ai.');
      }).catch(err => {
        console.error('Không thể copy text: ', err);
      });
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // --- Icon Helpers ---

  getIconClass(mimeType: string): string {
    const category = this.getCategory(mimeType);
    if (mimeType?.includes('pdf')) return 'icon-pdf';
    if (category === 'image') return 'icon-img';
    if (category === 'archive') return 'icon-zip';
    if (mimeType?.startsWith('video/')) return 'icon-video';
    if (mimeType?.startsWith('audio/')) return 'icon-audio';
    return 'icon-default';
  }

  getFileIconPath(mimeType: string): string {
    const category = this.getCategory(mimeType);
    if (mimeType?.includes('pdf')) return 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z M9 9h4 M9 13h6 M9 17h6';
    if (category === 'image') return 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z';
    if (category === 'archive') return 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4';
    if (category === 'media') return 'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z';
    return 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z'; // Default Document
  }
}