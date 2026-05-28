import { Component, ChangeDetectorRef, OnInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FileService } from '../../services/file.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

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
    private readonly zone: NgZone,
    private readonly dialogService: ConfirmDialogService
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

  async restore(item: any): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Khôi phục',
      message: `Khôi phục "${item.name}" về Drive?`,
      confirmText: 'Khôi phục',
      type: 'success'
    });
    if (!confirmed) return;
    this.fileService.restoreFromTrash(item.id).subscribe({
      next: () => {
        this.zone.run(() => {
          this.items = this.items.filter((x) => x.id !== item.id);
          this.cdr.detectChanges();
        });
      },
      error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Khôi phục thất bại.', type: 'danger' })
    });
  }

  async permanentlyDelete(item: any): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Xóa vĩnh viễn',
      message: `Xóa vĩnh viễn "${item.name}"? Hành động này không thể hoàn tác.`,
      confirmText: 'Xóa',
      type: 'danger'
    });
    if (!confirmed) return;
    this.fileService.permanentlyDelete(item.id).subscribe({
      next: () => {
        this.zone.run(() => {
          this.items = this.items.filter((x) => x.id !== item.id);
          this.cdr.detectChanges();
        });
      },
      error: () => this.dialogService.alert({ title: 'Thất bại', message: 'Xóa vĩnh viễn thất bại.', type: 'danger' })
    });
  }

  async emptyTrash(): Promise<void> {
    if (!this.items.length) return;
    const confirmed = await this.dialogService.confirm({
      title: 'Dọn sạch Trash',
      message: 'Xóa vĩnh viễn toàn bộ Trash? Hành động này không thể hoàn tác.',
      confirmText: 'Dọn sạch',
      type: 'danger'
    });
    if (!confirmed) return;
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
        this.dialogService.alert({ title: 'Thất bại', message: 'Dọn Trash thất bại.', type: 'danger' });
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


