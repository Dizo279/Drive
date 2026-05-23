import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogComponent } from '@core/components/confirm-dialog/confirm-dialog.component';
import { PreviewModalComponent } from '@core/components/preview-modal/preview-modal.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ConfirmDialogComponent, PreviewModalComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}
