import { Component, ChangeDetectorRef, OnInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FileService } from '../../services/file.service';

@Component({
  selector: 'app-trash-list',
  templateUrl: './trash-list.html',
  styleUrls: ['./trash-list.css'],
  standalone: true,
  imports: [CommonModule, RouterLink]
})
export class TrashListComponent implements OnInit {
  items: any[] = [];
  loading = true;
  clearing = false;

  constructor(
    private readonly fileService: FileService,
    private readonly cdr: ChangeDetectorRef,
    private readonly zone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadTrash();
  }

  loadTrash(): void {
    this.loading = true;
    this.fileService.getTrash().subscribe({
      next: (data) => {
        this.zone.run(() => {
          this.items = (Array.isArray(data) ? data : []).map((f: any) => ({
            id: f.id,
            name: f.fileName,
            isFolder: !!f.isFolder,
            sizeBytes: f.fileSize || 0,
            deletedAt: f.deletedAt
          }));
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  restore(item: any): void {
    if (!confirm(`Khôi phục "${item.name}"?`)) return;
    this.fileService.restoreFromTrash(item.id).subscribe({
      next: () => {
        this.zone.run(() => {
          this.items = this.items.filter((x) => x.id !== item.id);
          this.cdr.detectChanges();
        });
      },
      error: () => alert('Khôi phục thất bại.')
    });
  }

  permanentlyDelete(item: any): void {
    if (!confirm(`Xóa vĩnh viễn "${item.name}"? Hành động này không thể hoàn tác.`)) return;
    this.fileService.permanentlyDelete(item.id).subscribe({
      next: () => {
        this.zone.run(() => {
          this.items = this.items.filter((x) => x.id !== item.id);
          this.cdr.detectChanges();
        });
      },
      error: () => alert('Xóa vĩnh viễn thất bại.')
    });
  }

  emptyTrash(): void {
    if (!this.items.length) return;
    if (!confirm('Xóa vĩnh viễn toàn bộ Trash? Hành động này không thể hoàn tác.')) return;
    this.clearing = true;
    this.fileService.emptyTrash().subscribe({
      next: () => {
        this.zone.run(() => {
          this.items = [];
          this.clearing = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.zone.run(() => {
          this.clearing = false;
          this.cdr.detectChanges();
        });
        alert('Dọn Trash thất bại.');
      }
    });
  }

  daysLeft(deletedAt: string | null | undefined): number | null {
    if (!deletedAt) return null;
    const deleted = new Date(deletedAt).getTime();
    if (Number.isNaN(deleted)) return null;
    const expires = deleted + 30 * 24 * 60 * 60 * 1000;
    const diffMs = expires - Date.now();
    return Math.max(0, Math.ceil(diffMs / (24 * 60 * 60 * 1000)));
  }

  formatSize(bytes: number): string {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}

