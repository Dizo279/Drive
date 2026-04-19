import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FileService } from '../../file';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.html',
  styleUrls: ['./file-upload.css'],
  standalone: true,
  imports: [CommonModule, RouterModule] // Dùng RouterModule cho nút Back
})
export class FileUploadComponent {
  selectedFiles: File[] = [];
  isDragging: boolean = false;
  uploading: boolean = false;
  errorMsg: string = '';

  constructor(private fileService: FileService, private router: Router) {}

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    this.errorMsg = '';
    if (event.dataTransfer?.files) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  onFileSelect(event: any): void {
    this.errorMsg = '';
    if (event.target.files) {
      this.addFiles(event.target.files);
    }
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

  upload(): void {
    if (this.selectedFiles.length === 0) return;

    this.uploading = true;
    this.errorMsg = '';

    const uploadRequests = this.selectedFiles.map(file => this.fileService.uploadFile(file, null));

    forkJoin(uploadRequests).subscribe({
      next: () => {
        this.uploading = false;
        this.router.navigate(['/files']);
      },
      error: (err) => {
        this.uploading = false;
        this.errorMsg = err.error?.message || 'Có lỗi xảy ra. Vui lòng thử lại.';
        console.error(err);
      }
    });
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}