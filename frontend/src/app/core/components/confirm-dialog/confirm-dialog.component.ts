import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfirmDialogService } from '@core/services/confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state().isOpen) {
      <div class="cd-backdrop" (click)="onBackdropClick($event)">
        <div class="cd-panel" [class]="'cd-panel--' + state().config.type" role="dialog" aria-modal="true">
          <!-- Icon -->
          <div class="cd-icon-wrap">
            <div class="cd-icon" [class]="'cd-icon--' + state().config.type">
              @switch (state().config.type) {
                @case ('danger') {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
                    <path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
                  </svg>
                }
                @case ('warning') {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                    <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                  </svg>
                }
                @case ('success') {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                }
                @default {
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
                  </svg>
                }
              }
            </div>
          </div>

          <!-- Content -->
          <div class="cd-content">
            <h3 class="cd-title">{{ state().config.title }}</h3>
            <p class="cd-message">{{ state().config.message }}</p>
            @if (state().config.isPrompt) {
              <div class="cd-prompt-container">
                <input type="text" 
                       class="cd-prompt-input" 
                       [(ngModel)]="promptValue" 
                       [placeholder]="state().config.promptPlaceholder || ''"
                       (keyup.enter)="onConfirm()"
                       autofocus>
              </div>
            }
          </div>

          <!-- Actions -->
          <div class="cd-actions" [class.cd-actions--single]="state().config.alertOnly">
            @if (!state().config.alertOnly) {
              <button class="cd-btn cd-btn--cancel" (click)="onCancel()">
                {{ state().config.cancelText }}
              </button>
            }
            <button class="cd-btn" [class]="'cd-btn--' + state().config.type" (click)="onConfirm()">
              {{ state().config.confirmText }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styleUrls: ['./confirm-dialog.component.css']
})
export class ConfirmDialogComponent {
  promptValue: string = '';
  get state() { return this.dialogService.state; }

  constructor(private dialogService: ConfirmDialogService) {}

  onConfirm(): void {
    if (this.state().config.isPrompt) {
      this.dialogService.respond(this.promptValue);
    } else {
      this.dialogService.respond(true);
    }
    this.promptValue = '';
  }

  onCancel(): void {
    if (this.state().config.isPrompt) {
      this.dialogService.respond(null);
    } else {
      this.dialogService.respond(false);
    }
    this.promptValue = '';
  }

  /** Nhấn backdrop chỉ đóng khi là alert-only, không đóng khi confirm */
  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('cd-backdrop')) {
      if (this.state().config.alertOnly) {
        this.dialogService.respond(false);
      }
    }
  }
}