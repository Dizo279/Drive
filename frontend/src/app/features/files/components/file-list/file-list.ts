import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { HttpEventType, HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileService } from '../../file';
import { AuthService } from '../../../auth/auth';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-file-list',
  templateUrl: './file-list.html',
  styleUrls: ['./file-list.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink]
})
export class FileListComponent implements OnInit {
  allFiles: any[] = [];
  items: any[] = [];
  currentParentId: number | null = null;
  folderHistory: any[] = [];
  
  loading: boolean = true;
  currentUser: any = null;

  isDragging: boolean = false;
  isUploading: boolean = false;
  uploadProgress: number = 0;
  totalUploadSize: number = 0;
  totalLoadedSize: number = 0;
  uploadingCount: number = 0;

  searchQuery: string = '';
  sortBy: string = 'date';

  usedBytes: number = 0;
  quotaBytes: number = 1073741824;
  quotaPercent: number = 0;

  constructor(
    private fileService: FileService,
    private authService: AuthService,
    private router: Router,
    public cdr: ChangeDetectorRef,
    private zone: NgZone,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
  if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
    const token = localStorage.getItem('jwt_token');
    if (token) {
        // Gọi API để lấy dữ liệu Full từ Database
        const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
        this.http.get('http://localhost:8080/api/users/me', { headers }).subscribe({
          next: (data: any) => {
            this.currentUser = data; // Gán toàn bộ data (bao gồm fullName, avatarUrl)
            this.cdr.detectChanges();
          },
          error: () => {
            // Fallback nếu API lỗi (giải mã từ token như cũ)
            const payload = JSON.parse(atob(token.split('.')[1]));
            this.currentUser = { username: payload.sub };
          }
        });
    }
    this.loadData();
  }
}

  loadData(): void {
    this.loading = true;
    this.cdr.detectChanges();

    this.fileService.getFiles(this.currentParentId).subscribe({
      next: (data) => {
        this.zone.run(() => {
          try {
            const safeData = Array.isArray(data) ? data : [];
            this.allFiles = safeData.map(f => ({
                id: f.id,
                name: f.fileName || 'Không tên',
                sizeBytes: f.fileSize || 0,
                createdAt: f.createdAt,
                mimeType: f.isFolder ? '' : (f.mimeType || this.guessMimeType(f.fileName || '')),
                isFolder: f.isFolder,
                category: f.isFolder ? 'folder' : this.getCategory(f.mimeType)
            }));
            this.applyFiltersAndSort();
            this.calculateUsedQuota();
          } catch (error) {
            console.error('Lỗi khi map dữ liệu:', error);
          } finally {
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        this.zone.run(() => {
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  calculateUsedQuota(): void {
    this.fileService.getQuota().subscribe({
        next: (data) => {
            this.zone.run(() => {
              if(data && data.usedQuota !== undefined) {
                  this.usedBytes = data.usedQuota;
                  this.quotaBytes = data.maxQuota;
                  this.quotaPercent = data.percentage;
                  this.cdr.detectChanges();
              }
            });
        },
        error: (err) => console.log("Lỗi tải Quota.")
    });
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
    
    result.sort((a, b) => {
      // 1. ƯU TIÊN SỐ 1: Folder luôn luôn đứng trước File
      if (a.isFolder && !b.isFolder) return -1;
      if (!a.isFolder && b.isFolder) return 1;

      // 2. ƯU TIÊN SỐ 2: Sắp xếp theo tiêu chí người dùng chọn
      if (this.sortBy === 'name') return a.name.localeCompare(b.name);
      if (this.sortBy === 'size') return b.sizeBytes - a.sizeBytes;
      
      const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return timeB - timeA; // Mặc định xếp theo ngày mới nhất
    });

    this.zone.run(() => {
        this.items = result;
        this.cdr.detectChanges();
    });
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  openFolder(folder: any): void {
    if (folder.isFolder) {
      // Ghi nhận trực tiếp ID và Tên của thư mục vừa click vào lịch sử
      this.folderHistory.push({ id: folder.id, name: folder.name });
      this.currentParentId = folder.id;
      this.loadData();
    }
  }

  navigateToBreadcrumb(index: number | null): void {
    if (index === null) {
      // Về trang chủ (Drive)
      this.currentParentId = null;
      this.folderHistory = [];
    } else {
      // Quay lại một thư mục cụ thể trong lịch sử
      const target = this.folderHistory[index];
      this.currentParentId = target.id;
      // Cắt bỏ các thư mục phía sau thư mục được click
      this.folderHistory = this.folderHistory.slice(0, index + 1);
    }
    this.loadData();
  }

  createFolder(): void {
    if (typeof window !== 'undefined') {
      const folderName = prompt('Nhập tên thư mục mới:');
      if (folderName && folderName.trim() !== '') {
        this.fileService.createFolder(folderName, this.currentParentId).subscribe({
          next: () => this.loadData(),
          error: (err) => alert('Tạo thư mục thất bại!')
        });
      }
    }
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
    if (confirm(`Bạn có chắc muốn xóa "${file.name}"?`)) {
      this.fileService.deleteFile(file.id).subscribe({
        next: () => {
          this.zone.run(() => {
            this.allFiles = this.allFiles.filter(f => f.id !== file.id);
            this.applyFiltersAndSort();
            this.calculateUsedQuota();
            this.cdr.detectChanges();
          });
        },
        error: (err) => alert('Xóa file thất bại.')
      });
    }
  }

  logout(): void {
    if (confirm('Bạn có muốn đăng xuất?')) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files && files.length > 0) this.processMultipleUploads(files);
    event.target.value = null;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) this.processMultipleUploads(files);
  }

  processMultipleUploads(files: FileList) {
    this.isUploading = true;
    this.uploadingCount = files.length;
    this.uploadProgress = 0;
    this.totalUploadSize = 0;
    this.totalLoadedSize = 0;
    this.cdr.detectChanges();

    const fileArray = Array.from(files);
    this.totalUploadSize = fileArray.reduce((sum, file) => sum + file.size, 0);
    let loadedSizes = new Array(fileArray.length).fill(0);
    let completedCount = 0;

    fileArray.forEach((file, index) => {
      this.fileService.uploadFile(file, this.currentParentId).subscribe({
        next: (event: any) => {
          if (event.type === HttpEventType.UploadProgress) {
            loadedSizes[index] = event.loaded;
            this.totalLoadedSize = loadedSizes.reduce((a, b) => a + b, 0);
            this.uploadProgress = Math.round(100 * this.totalLoadedSize / this.totalUploadSize);
            this.cdr.detectChanges();
          }
          else if (event.type === HttpEventType.Response) {
            completedCount++;
            if (completedCount === fileArray.length) {
              setTimeout(() => {
                this.isUploading = false;
                this.loadData();
                this.calculateUsedQuota();
              }, 500);
            }
          }
        },
        error: (err) => {
          completedCount++;
          if (completedCount === fileArray.length) {
            this.isUploading = false;
            this.loadData();
          }
        }
      });
    });
  }

  onDragOver(event: DragEvent) { event.preventDefault(); event.stopPropagation(); this.isDragging = true; }
  onDragLeave(event: DragEvent) { event.preventDefault(); event.stopPropagation(); this.isDragging = false; }

  getCategory(mimeType: string): string {
    if (!mimeType) return 'other';
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.startsWith('video/') || mimeType.startsWith('audio/')) return 'media';
    if (mimeType.includes('zip') || mimeType.includes('rar')) return 'archive';
    if (mimeType.includes('pdf') || mimeType.includes('word') || mimeType.includes('excel') || mimeType.includes('powerpoint') || mimeType.includes('officedocument') || mimeType.includes('text/plain')) return 'document';
    return 'other';
  }

  guessMimeType(fileName: string): string {
      const ext = fileName.split('.').pop()?.toLowerCase();
      switch(ext) {
          case 'jpg': case 'jpeg': case 'png': case 'gif': case 'webp': case 'svg': return 'image/jpeg';
          case 'mp4': case 'mov': case 'avi': case 'mkv': return 'video/mp4';
          case 'mp3': case 'wav': case 'flac': return 'audio/mpeg';
          case 'zip': case 'rar': case '7z': case 'tar': return 'application/zip';
          case 'pdf': return 'application/pdf';
          case 'doc': case 'docx': return 'application/msword';
          case 'xls': case 'xlsx': return 'application/vnd.ms-excel';
          case 'ppt': case 'pptx': return 'application/vnd.ms-powerpoint';
          case 'txt': case 'md': return 'text/plain';
          default: return 'application/octet-stream';
      }
  }

  highlightMatch(text: string, query: string): string {
    if (!query) return text;
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<span class="text-highlight">$1</span>');
  }

  getIconPath(category: string): string {
    switch(category) {
        case 'folder': return 'M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z';
        case 'image': return 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z';
        case 'document': return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
        case 'media': return 'M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z';
        case 'archive': return 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4';
        default: return 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z';
    }
  }
}