// src/app/core/components/preview-modal/preview-modal.component.ts
import { Component, computed, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PreviewService } from '../../services/preview.service';

@Component({
  selector: 'app-preview-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './preview-modal.component.html',
  styleUrls: ['./preview-modal.component.css']
})
export class PreviewModalComponent implements OnDestroy {
  private previewService = inject(PreviewService);

  state = computed(() => this.previewService.state());
  objectUrl = computed(() => {
    const blob = this.state().fileBlob;
    return blob ? URL.createObjectURL(blob) : null;
  });

  private currentObjectUrl: string | null = null;

  ngOnDestroy(): void {
    this.revokeObjectUrl();
  }

  isImage(): boolean {
    return this.state().mimeType.startsWith('image/');
  }

  isVideo(): boolean {
    return this.state().mimeType.startsWith('video/');
  }

  onClose(): void {
    this.revokeObjectUrl();
    this.previewService.close();
  }

  onDownload(): void {
    const state = this.state();
    const url = this.objectUrl();

    if (!url || !state.fileBlob) return;

    const a = document.createElement('a');
    a.href = url;
    a.download = state.fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  private revokeObjectUrl(): void {
    if (this.currentObjectUrl) {
      URL.revokeObjectURL(this.currentObjectUrl);
      this.currentObjectUrl = null;
    }
  }
}
