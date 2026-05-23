import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FileService } from '../../services/file.service';
import { HttpEventType } from '@angular/common/http';
import { forkJoin } from 'rxjs';

/** Đại diện cho một node trong cây thư mục hiển thị */
interface TreeNode {
  name: string;
  isFolder: boolean;
  children: TreeNode[];
  file?: File;
  relativePath?: string;
}

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.html',
  styleUrls: ['./file-upload.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class FileUploadComponent {
  // Chế độ upload: 'files' hoặc 'folder'
  mode: 'files' | 'folder' = 'files';

  // Dữ liệu upload file thường
  selectedFiles: File[] = [];

  // Dữ liệu upload folder
  folderFiles: File[] = [];
  relativePaths: string[] = [];
  folderTree: TreeNode[] = [];    // Cây thư mục để hiển thị UI
  folderName: string = '';        // Tên thư mục gốc

  isDragging = false;
  uploading = false;
  uploadProgress = 0;
  errorMsg = '';

  /**
   * Bộ đếm dragEnter/dragLeave để xử lý hiện tượng flickering
   * khi kéo file qua các phần tử con bên trong dropzone.
   * Mỗi lần dragenter vào child -> +1, dragleave ra child -> -1.
   * Chỉ khi counter = 0 mới thực sự thoát khỏi dropzone.
   */
  private dragCounter = 0;

  constructor(private fileService: FileService, private router: Router) {}

  // ─── SWITCH MODE ─────────────────────────────────────────────────────────────

  setMode(m: 'files' | 'folder'): void {
    this.mode = m;
    this.reset();
  }

  // ─── DRAG & DROP (ĐÃ SỬA) ───────────────────────────────────────────────────

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onDragEnter(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragCounter++;
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragCounter--;
    // Chỉ tắt trạng thái kéo khi thực sự rời khỏi toàn bộ vùng dropzone
    if (this.dragCounter <= 0) {
      this.dragCounter = 0;
      this.isDragging = false;
    }
  }

  async onDrop(event: DragEvent): Promise<void> {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    this.dragCounter = 0;
    this.errorMsg = '';

    const items = event.dataTransfer?.items;
    if (!items || items.length === 0) return;

    // Phân tích nội dung: kiểm tra xem có folder nào trong danh sách không
    const entries: any[] = [];
    let hasDirectory = false;

    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.kind === 'file') {
        const entry = item.webkitGetAsEntry?.() || (item as any).getAsEntry?.();
        if (entry) {
          entries.push(entry);
          if (entry.isDirectory) {
            hasDirectory = true;
          }
        }
      }
    }

    if (entries.length === 0) {
      // Fallback: nếu trình duyệt không hỗ trợ FileSystemEntry API
      if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
        if (this.mode === 'folder') {
          this.errorMsg = 'Trình duyệt không hỗ trợ kéo thả folder. Hãy dùng nút "Chọn Folder".';
        } else {
          this.addFiles(event.dataTransfer.files);
        }
      }
      return;
    }

    // Auto-detect: Nếu có folder thì tự chuyển sang mode folder
    if (hasDirectory) {
      this.mode = 'folder';
      await this.processEntries(entries);
    } else {
      // Tất cả đều là file
      if (this.mode === 'folder') {
        // Đang ở chế độ folder nhưng thả file thường -> thông báo
        this.errorMsg = 'Bạn đang thả file thường. Chuyển sang chế độ "Upload File" hoặc kéo thả một folder.';
        // Tự chuyển mode và thêm file
        this.mode = 'files';
        this.addFilesFromEntries(entries);
      } else {
        this.addFilesFromEntries(entries);
      }
    }
  }

  /**
   * Thêm file từ FileSystemEntry (không phải folder)
   */
  private addFilesFromEntries(entries: any[]): void {
    for (const entry of entries) {
      if (entry.isFile) {
        entry.file((file: File) => {
          this.selectedFiles.push(file);
        });
      }
    }
  }

  /**
   * Xử lý các entry được thả (hỗ trợ cả file và folder lẫn lộn).
   * Sử dụng FileSystemEntry API để đọc đệ quy folder.
   */
  private async processEntries(entries: any[]): Promise<void> {
    this.folderFiles = [];
    this.relativePaths = [];
    this.folderTree = [];
    this.folderName = '';

    const promises: Promise<void>[] = [];

    for (const entry of entries) {
      promises.push(this.readEntry(entry, ''));
    }

    try {
      await Promise.all(promises);
    } catch (err) {
      console.error('Lỗi khi đọc file/folder:', err);
      this.errorMsg = 'Có lỗi khi đọc nội dung. Vui lòng thử lại.';
      return;
    }

    // Rebuild cây sau khi đọc xong
    if (this.relativePaths.length > 0) {
      this.folderTree = this.buildTree(this.relativePaths, this.folderFiles);
      // Lấy tên thư mục gốc từ đường dẫn đầu tiên
      const firstParts = this.relativePaths[0].split('/');
      this.folderName = firstParts.length > 1 ? firstParts[0] : 'Dropped Files';
    }
  }

  /** Đọc đệ quy FileSystemEntry (file hoặc directory) */
  private readEntry(entry: any, basePath: string): Promise<void> {
    if (entry.isFile) {
      return new Promise((resolve, reject) => {
        entry.file(
          (file: File) => {
            const relPath = basePath ? `${basePath}/${entry.name}` : entry.name;
            this.folderFiles.push(file);
            this.relativePaths.push(relPath);
            resolve();
          },
          (err: any) => {
            console.warn(`Không thể đọc file: ${entry.name}`, err);
            resolve(); // Bỏ qua file lỗi, không chặn toàn bộ quá trình
          }
        );
      });
    } else if (entry.isDirectory) {
      return new Promise((resolve, reject) => {
        const dirPath = basePath ? `${basePath}/${entry.name}` : entry.name;
        const reader = entry.createReader();

        // readEntries() chỉ trả tối đa 100 entries/lần (Chrome).
        // Phải gọi lặp lại cho đến khi trả mảng rỗng.
        const allEntries: any[] = [];

        const readBatch = () => {
          reader.readEntries(
            async (batch: any[]) => {
              if (batch.length === 0) {
                // Đã đọc hết tất cả entries trong directory
                try {
                  await Promise.all(
                    allEntries.map((e: any) => this.readEntry(e, dirPath))
                  );
                  resolve();
                } catch (err) {
                  reject(err);
                }
              } else {
                allEntries.push(...batch);
                readBatch(); // Đọc batch tiếp theo
              }
            },
            (err: any) => {
              console.warn(`Không thể đọc thư mục: ${entry.name}`, err);
              resolve(); // Bỏ qua thư mục lỗi
            }
          );
        };

        readBatch();
      });
    }
    return Promise.resolve();
  }

  // ─── FILE INPUT (input[type=file]) ───────────────────────────────────────────

  onFileSelect(event: any): void {
    this.errorMsg = '';
    if (event.target.files) {
      this.addFiles(event.target.files);
    }
    // Reset input để có thể chọn lại cùng file
    event.target.value = '';
  }

  onFolderSelect(event: any): void {
    this.errorMsg = '';
    const fileList: FileList = event.target.files;
    if (!fileList || fileList.length === 0) return;

    this.folderFiles = [];
    this.relativePaths = [];

    for (let i = 0; i < fileList.length; i++) {
      const file = fileList[i];
      // webkitRelativePath có dạng: "FolderName/sub/file.txt"
      const relPath = (file as any).webkitRelativePath || file.name;
      this.folderFiles.push(file);
      this.relativePaths.push(relPath);
    }

    this.folderTree = this.buildTree(this.relativePaths, this.folderFiles);
    this.folderName = this.relativePaths.length > 0
      ? this.relativePaths[0].split('/')[0]
      : 'Folder';

    // Reset input để có thể chọn lại cùng folder
    event.target.value = '';
  }

  addFiles(files: FileList): void {
    for (let i = 0; i < files.length; i++) {
      this.selectedFiles.push(files[i]);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.errorMsg = '';
  }

  // ─── TREE BUILDER ────────────────────────────────────────────────────────────

  /**
   * Xây dựng cấu trúc cây từ danh sách đường dẫn tương đối.
   * Ví dụ: ["MyProject/src/App.java", "MyProject/README.md"]
   * -> TreeNode{ name:"MyProject", children:[...] }
   */
  buildTree(paths: string[], files: File[]): TreeNode[] {
    const root: TreeNode[] = [];
    const nodeMap = new Map<string, TreeNode>();

    paths.forEach((path, idx) => {
      const parts = path.replace(/\\/g, '/').split('/');
      let currentLevel = root;
      let currentPath = '';

      parts.forEach((part, i) => {
        currentPath = currentPath ? `${currentPath}/${part}` : part;
        const isLast = i === parts.length - 1;

        if (!nodeMap.has(currentPath)) {
          const node: TreeNode = {
            name: part,
            isFolder: !isLast,
            children: [],
            file: isLast ? files[idx] : undefined,
            relativePath: isLast ? path : undefined
          };
          nodeMap.set(currentPath, node);
          currentLevel.push(node);
        }
        currentLevel = nodeMap.get(currentPath)!.children;
      });
    });

    return root;
  }

  // ─── UPLOAD ──────────────────────────────────────────────────────────────────

  upload(): void {
    if (this.mode === 'files') {
      this.uploadFiles();
    } else {
      this.uploadFolderFiles();
    }
  }

  private uploadFiles(): void {
    if (this.selectedFiles.length === 0) return;
    this.uploading = true;
    this.uploadProgress = 0;
    this.errorMsg = '';

    const total = this.selectedFiles.length;
    let done = 0;

    const uploadRequests = this.selectedFiles.map(file =>
      this.fileService.uploadFile(file, null)
    );

    forkJoin(uploadRequests).subscribe({
      next: () => {
        this.uploading = false;
        this.router.navigate(['/files']);
      },
      error: (err) => {
        this.uploading = false;
        this.errorMsg = err.error?.message || 'Có lỗi xảy ra. Vui lòng thử lại.';
      }
    });
  }

  private uploadFolderFiles(): void {
    if (this.folderFiles.length === 0) return;
    this.uploading = true;
    this.uploadProgress = 0;
    this.errorMsg = '';

    this.fileService.uploadFolder(this.folderFiles, this.relativePaths, null).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress = Math.round(100 * event.loaded / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.uploading = false;
          this.uploadProgress = 100;
          this.router.navigate(['/files']);
        }
      },
      error: (err) => {
        this.uploading = false;
        this.errorMsg = err.error?.message || err.error?.error || 'Có lỗi xảy ra. Vui lòng thử lại.';
      }
    });
  }

  // ─── RESET & HELPERS ─────────────────────────────────────────────────────────

  reset(): void {
    this.selectedFiles = [];
    this.folderFiles = [];
    this.relativePaths = [];
    this.folderTree = [];
    this.folderName = '';
    this.errorMsg = '';
    this.uploadProgress = 0;
  }

  get totalFolderSize(): number {
    return this.folderFiles.reduce((acc, f) => acc + f.size, 0);
  }

  get hasItems(): boolean {
    return this.mode === 'files'
      ? this.selectedFiles.length > 0
      : this.folderFiles.length > 0;
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}