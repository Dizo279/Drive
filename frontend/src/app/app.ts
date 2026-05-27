import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog';
import { PreviewModalComponent } from '@shared/components/preview-modal/preview-modal';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ConfirmDialogComponent, PreviewModalComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}

