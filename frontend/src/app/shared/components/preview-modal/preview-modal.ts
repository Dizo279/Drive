import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, PLATFORM_ID, computed, effect, inject, signal } from '@angular/core';
import { PreviewService } from '../../services/preview.service';

@Component({
  selector: 'app-preview-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './preview-modal.component.html',
  styleUrls: ['./preview-modal.component.css']
})
export class PreviewModalComponent implements OnDestroy {
  private readonly previewService = inject(PreviewService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  private currentObjectUrl: string | null = null;

  readonly state = computed(() => this.previewService.state());
  readonly objectUrl = signal<string | null>(null);

  constructor() {
    effect(() => {
      const blob = this.state().fileBlob;
      this.revokeObjectUrl();

      if (blob && this.isBrowser) {
        this.currentObjectUrl = URL.createObjectURL(blob);
        this.objectUrl.set(this.currentObjectUrl);
      } else {
        this.objectUrl.set(null);
      }
    });
  }

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
    this.previewService.close();
  }

  onDownload(): void {
    this.previewService.download();
  }

  private revokeObjectUrl(): void {
    if (this.currentObjectUrl && this.isBrowser) {
      URL.revokeObjectURL(this.currentObjectUrl);
      this.currentObjectUrl = null;
    }
    this.objectUrl.set(null);
  }
}

