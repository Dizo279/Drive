import { Component, OnInit, ChangeDetectorRef, NgZone, HostListener } from '@angular/core';
import { HttpEventType, HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CommonModule} from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileService } from '../../services/file.service';
import { AuthService } from '@features/auth/services/auth.service';
import { Router, RouterLink, RouterModule } from '@angular/router';
import { NotificationBellComponent } from '@notification/components/notification-bell/notification-bell';
import { ConfirmDialogService } from '@core/services/confirm-dialog.service';
import { PreviewService } from '@core/services/preview.service';

@Component({
  selector: 'app-file-list',
  templateUrl: './file-list.html',
  styleUrls: ['./file-list.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterModule, NotificationBellComponent]
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
  private dragCounter: number = 0;

  searchQuery: string = '';
  sortBy: string = 'date';

  usedBytes: number = 0;
  quotaBytes: number = 1073741824;
  quotaPercent: number = 0;

  showToast: boolean = false;
  toastMessage: string = '';
  toastTimeout: any;

  isShareModalOpen: boolean = false;
  shareLink: string = '';
  selectedFileForShare: any = null;
  shareEmailsInput: string = '';
  expireDays: number | null = null;
  isSharing: boolean = false;

  isAdmin: boolean = false;
  isUpgradePending: boolean = false;

  selectedItemIds: Set<number> = new Set<number>();
  bulkProcessing: boolean = false;
  bulkProcessMessage: string = '';
  bulkProgress: number = 0;
  bulkProcessType: 'download' | 'delete' | null = null;

  contextMenuVisible: boolean = false;
  contextMenuX: number = 0;
  contextMenuY: number = 0;
  contextMenuItem: any = null;
  contextMenuHasPasteTarget: boolean = false;
  contextMenuTargetParentId: number | null = null;

  clipboardItem: any = null;
  clipboardAction: 'copy' | 'cut' | null = null;
  focusedItem: any = null;

  constructor(
    private fileService: FileService,
    private authService: AuthService,
    private router: Router,
    public cdr: ChangeDetectorRef,
    private zone: NgZone,
    private http: HttpClient,
    private dialogService: ConfirmDialogService,
    private previewService: PreviewService
  ) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      const token = localStorage.getItem('jwt_token');
      if (token) {
          // Gọi API để lấy dữ liệu Full từ Database
          const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
          this.http.get('http://localhost:8080/api/users/me', { headers }).subscribe({
            next: (data: any) => {
              this.currentUser = data;
              
              // BỔ SUNG LỆNH NÀY: Kiểm tra role để hiện nút Admin
              this.isAdmin = (data.role && data.role.toUpperCase() === 'ADMIN');
              
              this.cdr.detectChanges();
            },
            error: () => {
              // Fallback nếu API lỗi
              const payload = JSON.parse(atob(token.split('.')[1]));
              this.currentUser = { username: payload.sub };
              
              // Bổ sung kiểm tra role từ token (nếu token có chứa role)
              this.isAdmin = (payload.role && payload.role.toUpperCase() === 'ADMIN');
            }
          });
      }
      this.loadData();
    }
  }

    loadData(): void {
    this.loading = true;
    this.selectedItemIds.clear();
    this.bulkProcessing = false;
    this.bulkProcessMessage = '';
    this.bulkProgress = 0;
    this.cdr.detectChanges();

    // Reset trạng thái pending khi load lại
    this.isUpgradePending = false;

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

  onContentContextMenu(event: MouseEvent): void {
    event.preventDefault();
    this.openContextMenu(event, null, this.currentParentId);
  }

  onRowContextMenu(event: MouseEvent, item: any): void {
    event.preventDefault();
    event.stopPropagation();

    this.focusedItem = item;
    this.openContextMenu(event, item, item.isFolder ? item.id : null);
  }

  private openContextMenu(event: MouseEvent, item: any, targetParentId: number | null): void {
    this.contextMenuItem = item;
    this.contextMenuTargetParentId = targetParentId;
    this.contextMenuHasPasteTarget = item === null || !!item?.isFolder;
    this.contextMenuVisible = true;

    const menuWidth = 220;
    const menuHeight = 190;
    const margin = 12;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let x = event.clientX;
    let y = event.clientY;

    if (x + menuWidth + margin > viewportWidth) {
      x = viewportWidth - menuWidth - margin;
    }
    if (y + menuHeight + margin > viewportHeight) {
      y = viewportHeight - menuHeight - margin;
    }
    if (x < margin) x = margin;
    if (y < margin) y = margin;

    this.contextMenuX = x;
    this.contextMenuY = y;
    this.cdr.detectChanges();
  }

  closeContextMenu(): void {
    this.contextMenuVisible = false;
    this.contextMenuItem = null;
    this.contextMenuHasPasteTarget = false;
    this.contextMenuTargetParentId = null;
  }

  copySelectedItem(item: any = this.contextMenuItem): void {
    if (!item) return;
    this.contextMenuItem = item;
    this.clipboardItem = item;
    this.clipboardAction = 'copy';
    this.displayToast(`Đã copy "${this.contextMenuItem.name}"`);
    this.closeContextMenu();
  }

  cutSelectedItem(item: any = this.contextMenuItem): void {
    if (!item) return;
    this.contextMenuItem = item;
    this.clipboardItem = item;
    this.clipboardAction = 'cut';
    this.displayToast(`Đã cut "${this.contextMenuItem.name}"`);
    this.closeContextMenu();
  }

  async renameSelectedItem(): Promise<void> {
    if (!this.contextMenuItem) return;
    const item = this.contextMenuItem;
    this.closeContextMenu();

    const newName = await this.dialogService.prompt({
      title: 'Đổi tên',
      message: `Nhập tên mới cho "${item.name}"`,
      promptPlaceholder: 'Tên mới...',
      confirmText: 'Lưu',
      type: 'info'
    });

    if (!newName || !newName.trim() || newName.trim() === item.name) {
      return;
    }

    this.fileService.renameItem(item.id, newName.trim()).subscribe({
      next: () => this.loadData(),
      error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Đổi tên thất bại!', type: 'danger' })
    });
  }

  pasteClipboardItem(targetParentId: number | null = this.contextMenuTargetParentId): void {
    if (!this.clipboardItem || !this.clipboardAction) {
      this.displayToast('Clipboard trống');
      this.closeContextMenu();
      return;
    }

    if (!this.contextMenuHasPasteTarget) {
      this.displayToast('Chỉ có thể paste vào thư mục');
      this.closeContextMenu();
      return;
    }

    const sourceItem = this.clipboardItem;
    const action = this.clipboardAction;

    if (sourceItem.id === targetParentId) {
      this.displayToast('Cannot paste into itself');
      this.closeContextMenu();
      return;
    }

    const request$ = action === 'copy'
      ? this.fileService.copyItem(sourceItem.id, targetParentId)
      : this.fileService.moveItem(sourceItem.id, targetParentId);

    request$.subscribe({
      next: () => {
        this.displayToast(action === 'copy' ? 'Copy thành công' : 'Move thành công');
        if (action === 'cut') {
          this.clipboardItem = null;
          this.clipboardAction = null;
        }
        this.loadData();
      },
      error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Paste thất bại!', type: 'danger' })
    });

    this.closeContextMenu();
  }

  canPasteToCurrentContext(): boolean {
    return !!this.clipboardItem
      && !!this.clipboardAction
      && this.contextMenuHasPasteTarget
      && this.clipboardItem.id !== this.contextMenuTargetParentId;
  }

  getContextMenuTitle(): string {
    if (this.contextMenuItem) {
      return this.contextMenuItem.name;
    }
    return this.folderHistory.length > 0 ? this.folderHistory[this.folderHistory.length - 1].name : 'My Drive';
  }

  getPasteMenuLabel(): string {
    return this.contextMenuItem?.isFolder ? 'Paste into folder' : 'Paste here';
  }

  onRowClick(item: any): void {
    this.focusedItem = item;
  }

  private getKeyboardTargetItem(): any {
    if (this.contextMenuItem) {
      return this.contextMenuItem;
    }
    if (this.selectedItemIds.size > 0) {
      const selectedId = Array.from(this.selectedItemIds)[0];
      return this.items.find(item => item.id === selectedId) || null;
    }
    return this.focusedItem;
  }

  private isTypingTarget(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement | null;
    if (!target) return false;
    const tagName = target.tagName.toLowerCase();
    return tagName === 'input' || tagName === 'textarea' || tagName === 'select' || target.isContentEditable;
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcuts(event: KeyboardEvent): void {
    if (this.isTypingTarget(event) || this.isShareModalOpen) {
      return;
    }

    const key = event.key.toLowerCase();
    const hasModifier = event.ctrlKey || event.metaKey;

    if (hasModifier && key === 'c') {
      const item = this.getKeyboardTargetItem();
      if (item) {
        event.preventDefault();
        this.copySelectedItem(item);
      }
      return;
    }

    if (hasModifier && key === 'x') {
      const item = this.getKeyboardTargetItem();
      if (item) {
        event.preventDefault();
        this.cutSelectedItem(item);
      }
      return;
    }

    if (hasModifier && key === 'v') {
      event.preventDefault();
      this.contextMenuHasPasteTarget = true;
      this.pasteClipboardItem(this.currentParentId);
      return;
    }

    if ((event.key === ' ' || event.key === 'Enter') && this.contextMenuVisible && this.canPasteToCurrentContext()) {
      event.preventDefault();
      this.pasteClipboardItem();
      return;
    }

    if (event.key === 'Escape' && this.contextMenuVisible) {
      event.preventDefault();
      this.closeContextMenu();
      return;
    }

    if (event.key === 'F2') {
      const item = this.getKeyboardTargetItem();
      if (item) {
        event.preventDefault();
        this.contextMenuItem = item;
        this.renameSelectedItem();
      }
    }
  }

  onItemDoubleClick(item: any): void {
    if (item.isFolder) {
      // Navigate into folder (existing behavior)
      this.folderHistory.push({ id: item.id, name: item.name });
      this.currentParentId = item.id;
      this.loadData();
    } else if (item.mimeType?.startsWith('image/') ||
               item.mimeType?.startsWith('video/')) {
      // Open preview for images and videos
      this.previewService.open({
        id: item.id,
        name: item.name,
        mimeType: item.mimeType
      });
    }
    // Other files: no action (could add download logic here if needed)
  }

  toggleSelection(item: any, event: Event): void {
    event.stopPropagation();
    if (this.selectedItemIds.has(item.id)) {
      this.selectedItemIds.delete(item.id);
    } else {
      this.selectedItemIds.add(item.id);
    }
  }

  selectAllVisible(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    if (checkbox.checked) {
      this.items.forEach(item => this.selectedItemIds.add(item.id));
    } else {
      this.items.forEach(item => this.selectedItemIds.delete(item.id));
    }
  }

  get isAllSelected(): boolean {
    return this.items.length > 0 && this.items.every(item => this.selectedItemIds.has(item.id));
  }

  get isIndeterminate(): boolean {
    return this.selectedItemIds.size > 0 && !this.isAllSelected;
  }

  getSelectedItemsOrdered(): any[] {
    const selected = this.allFiles.filter(item => this.selectedItemIds.has(item.id));
    return selected.sort((a, b) => {
      const indexA = this.items.findIndex(item => item.id === a.id);
      const indexB = this.items.findIndex(item => item.id === b.id);
      const orderA = indexA === -1 ? Number.MAX_SAFE_INTEGER : indexA;
      const orderB = indexB === -1 ? Number.MAX_SAFE_INTEGER : indexB;
      return orderA - orderB;
    });
  }

  clearSelection(): void {
    this.selectedItemIds.clear();
  }

  async downloadSelected(): Promise<void> {
    if (this.selectedItemIds.size === 0 || this.bulkProcessing) {
      return;
    }
    const selectedItems = this.getSelectedItemsOrdered();
    if (selectedItems.length === 0) {
      return;
    }

    this.bulkProcessing = true;
    this.bulkProcessType = 'download';
    this.bulkProgress = 0;

    for (let i = 0; i < selectedItems.length; i++) {
      const item = selectedItems[i];
      this.bulkProcessMessage = `Đang tải xuống ${item.name} (${i + 1}/${selectedItems.length})`;
      try {
        await this.downloadItem(item);
      } catch (error) {
        console.error('Lỗi tải xuống mục:', item, error);
      }
      this.bulkProgress = Math.round(((i + 1) / selectedItems.length) * 100);
      this.cdr.detectChanges();
    }

    this.bulkProcessMessage = 'Hoàn tất tải xuống đã chọn';
    setTimeout(() => {
      this.bulkProcessing = false;
      this.bulkProcessMessage = '';
      this.bulkProgress = 0;
      this.bulkProcessType = null;
      this.cdr.detectChanges();
    }, 1200);
  }

  async deleteSelected(): Promise<void> {
    if (this.selectedItemIds.size === 0 || this.bulkProcessing) {
      return;
    }

    const confirmed = await this.dialogService.confirm({
      title: 'Xóa mục đã chọn',
      message: `Bạn có chắc muốn xóa ${this.selectedItemIds.size} mục đã chọn?`,
      confirmText: 'Xóa',
      type: 'danger'
    });
    if (!confirmed) {
      return;
    }

    const selectedItems = this.getSelectedItemsOrdered();
    if (selectedItems.length === 0) {
      return;
    }

    this.bulkProcessing = true;
    this.bulkProcessType = 'delete';
    this.bulkProgress = 0;

    for (let i = 0; i < selectedItems.length; i++) {
      const item = selectedItems[i];
      this.bulkProcessMessage = `Đang xóa ${item.name} (${i + 1}/${selectedItems.length})`;
      try {
        await firstValueFrom(this.fileService.deleteFile(item.id));
        this.allFiles = this.allFiles.filter(file => file.id !== item.id);
        this.selectedItemIds.delete(item.id);
        this.applyFiltersAndSort();
      } catch (error) {
        console.error('Lỗi xóa mục:', item, error);
      }
      this.bulkProgress = Math.round(((i + 1) / selectedItems.length) * 100);
      this.cdr.detectChanges();
    }

    this.bulkProcessing = false;
    this.bulkProcessMessage = 'Hoàn tất xóa đã chọn';
    this.bulkProgress = 100;
    this.calculateUsedQuota();
    setTimeout(() => {
      this.bulkProcessMessage = '';
      this.bulkProgress = 0;
      this.bulkProcessType = null;
      this.cdr.detectChanges();
    }, 1200);
  }

  private async downloadItem(item: any): Promise<void> {
    const blob = await firstValueFrom(item.isFolder ? this.fileService.downloadFolder(item.id) : this.fileService.downloadFile(item.id));
    this.saveBlob(blob, item);
  }

  private saveBlob(blob: Blob, item: any): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = item.isFolder ? `${item.name}.zip` : item.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
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

  async createFolder(): Promise<void> {
    if (typeof window !== 'undefined') {
      const folderName = await this.dialogService.prompt({
        title: 'Thư mục mới',
        message: 'Vui lòng nhập tên cho thư mục mới:',
        promptPlaceholder: 'Nhập tên thư mục...',
        confirmText: 'Tạo mới',
        type: 'info'
      });
      
      if (folderName && folderName.trim() !== '') {
        this.fileService.createFolder(folderName.trim(), this.currentParentId).subscribe({
          next: () => this.loadData(),
          error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Tạo thư mục thất bại!', type: 'danger' })
        });
      }
    }
  }

  download(file: any): void {
    // Kiểm tra xem có phải folder không
    if (file.isFolder) {
      // Download folder as ZIP
      this.fileService.downloadFolder(file.id).subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = file.name + '.zip';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Tải folder thất bại!', type: 'danger' })
      });
    } else {
      // Download file
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
        error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Tải file thất bại!', type: 'danger' })
      });
    }
  }

  async delete(file: any): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Xóa file',
      message: `Bạn có chắc muốn xóa "${file.name}"?`,
      confirmText: 'Xóa',
      type: 'danger'
    });
    if (!confirmed) return;
    this.fileService.deleteFile(file.id).subscribe({
      next: () => {
        this.zone.run(() => {
          this.allFiles = this.allFiles.filter(f => f.id !== file.id);
          this.applyFiltersAndSort();
          this.calculateUsedQuota();
          this.cdr.detectChanges();
        });
      },
      error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Xóa file thất bại.', type: 'danger' })
    });
  }

  async logout(): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Đăng xuất',
      message: 'Bạn có muốn đăng xuất khỏi hệ thống?',
      confirmText: 'Đăng xuất',
      type: 'warning'
    });
    if (!confirmed) return;
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files && files.length > 0) this.processMultipleUploads(files);
    event.target.value = null;
  }

  async onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    this.dragCounter = 0;

    const items = event.dataTransfer?.items;
    if (items && items.length > 0) {
      // Kiểm tra xem có folder không qua FileSystemEntry API
      const entries: any[] = [];
      let hasDirectory = false;
      for (let i = 0; i < items.length; i++) {
        if (items[i].kind === 'file') {
          const entry = items[i].webkitGetAsEntry?.() || (items[i] as any).getAsEntry?.();
          if (entry) {
            entries.push(entry);
            if (entry.isDirectory) hasDirectory = true;
          }
        }
      }

      if (hasDirectory && entries.length > 0) {
        // Có folder -> đọc đệ quy rồi upload folder
        await this.processDroppedEntries(entries);
        return;
      }
    }

    // Fallback: upload file thường
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) this.processMultipleUploads(files);
  }

  /** Đọc đệ quy các FileSystemEntry (folder) rồi upload */
  private async processDroppedEntries(entries: any[]): Promise<void> {
    const collectedFiles: File[] = [];
    const relativePaths: string[] = [];

    const readEntry = (entry: any, basePath: string): Promise<void> => {
      if (entry.isFile) {
        return new Promise((resolve) => {
          entry.file((file: File) => {
            const relPath = basePath ? `${basePath}/${entry.name}` : entry.name;
            collectedFiles.push(file);
            relativePaths.push(relPath);
            resolve();
          }, () => resolve());
        });
      } else if (entry.isDirectory) {
        return new Promise((resolve) => {
          const dirPath = basePath ? `${basePath}/${entry.name}` : entry.name;
          const reader = entry.createReader();
          const allEntries: any[] = [];
          const readBatch = () => {
            reader.readEntries(async (batch: any[]) => {
              if (batch.length === 0) {
                await Promise.all(allEntries.map((e: any) => readEntry(e, dirPath)));
                resolve();
              } else {
                allEntries.push(...batch);
                readBatch();
              }
            }, () => resolve());
          };
          readBatch();
        });
      }
      return Promise.resolve();
    };

    await Promise.all(entries.map(e => readEntry(e, '')));

    if (collectedFiles.length === 0) return;

    // Upload folder qua API
    this.isUploading = true;
    this.uploadingCount = collectedFiles.length;
    this.uploadProgress = 0;
    this.totalUploadSize = collectedFiles.reduce((s, f) => s + f.size, 0);
    this.totalLoadedSize = 0;
    this.cdr.detectChanges();

    this.fileService.uploadFolder(collectedFiles, relativePaths, this.currentParentId).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.totalLoadedSize = event.loaded;
          this.uploadProgress = Math.round(100 * event.loaded / event.total);
          this.cdr.detectChanges();
        } else if (event.type === HttpEventType.Response) {
          setTimeout(() => {
            this.isUploading = false;
            this.loadData();
            this.calculateUsedQuota();
          }, 500);
        }
      },
      error: () => {
        this.isUploading = false;
        this.loadData();
      }
    });
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

  onDragOver(event: DragEvent) { event.preventDefault(); event.stopPropagation(); }
  onDragEnter(event: DragEvent) { event.preventDefault(); event.stopPropagation(); this.dragCounter++; this.isDragging = true; }
  onDragLeave(event: DragEvent) { event.preventDefault(); event.stopPropagation(); this.dragCounter--; if (this.dragCounter <= 0) { this.dragCounter = 0; this.isDragging = false; } }

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

  openShareModal(file: any) {
    if (event) event.stopPropagation();
    this.selectedFileForShare = file;
    this.shareLink = ''; // Xóa link cũ nếu có
    this.shareEmailsInput = '';
    this.expireDays = null;
    this.isShareModalOpen = true;
  }

  generatePublicLink() {
    this.isSharing = true;
    const payload = { emails: [], expireDays: this.expireDays };

    this.fileService.shareFile(this.selectedFileForShare.id, payload).subscribe({
      next: (res: any) => {
        this.isSharing = false;
        const token = res.shareToken || res.token || res;
        // Trỏ trực tiếp vào API Backend để tải luôn khi bấm vào link
        this.shareLink = `http://localhost:8080/api/files/shared/${token}`;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSharing = false;
        this.displayToast('Lỗi: ' + (err.error?.error || 'Không thể tạo link.'));
      }
    });
  }

  // --- CHIA SẺ QUA EMAIL ---
  submitEmailShare() {
    if (!this.shareEmailsInput.trim()) {
      this.displayToast('Vui lòng nhập ít nhất 1 địa chỉ email!');
      return;
    }

    this.isSharing = true;
    // Tách email bằng dấu phẩy, loại bỏ khoảng trắng thừa
    const emailList = this.shareEmailsInput.split(',').map(e => e.trim()).filter(e => e !== '');
    const payload = { emails: emailList, expireDays: this.expireDays };

    this.fileService.shareFile(this.selectedFileForShare.id, payload).subscribe({
      next: (res: any) => {
        this.isSharing = false;
        this.displayToast(res.message || 'Đã cấp quyền truy cập thành công!');
        this.shareEmailsInput = ''; // Xóa ô nhập sau khi thành công
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isSharing = false;
        this.displayToast('Lỗi: ' + (err.error?.error || 'Không thể chia sẻ.'));
      }
    });
  }

  // --- COPY LINK ---
  copyLink(): void {
    if (typeof window !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(this.shareLink).then(() => {
        this.displayToast('Đã copy liên kết chia sẻ!');
      });
    }
  }

  closeShareModal(): void {
    this.isShareModalOpen = false;
    this.selectedFileForShare = null;
  }

  displayToast(message: string): void {
    this.toastMessage = message;
    this.showToast = true;
    
    // Xóa bộ đếm cũ nếu người dùng bấm liên tục
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    
    this.toastTimeout = setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges(); // Ép Angular cập nhật UI
    }, 3000);
  }

  async requestUpgrade(): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Nâng cấp PREMIUM',
      message: 'Bạn có muốn gửi yêu cầu nâng cấp lên PREMIUM (100GB) không?',
      confirmText: 'Gửi yêu cầu',
      type: 'info'
    });
    if (!confirmed) return;
    const token = localStorage.getItem('jwt_token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    this.http.post('http://localhost:8080/api/users/upgrade-request', {}, { headers }).subscribe({
      next: (res: any) => {
        this.displayToast(res.message);
        this.isUpgradePending = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.displayToast(err.error?.error || 'Không thể gửi yêu cầu.');
      }
    });
  }

}
