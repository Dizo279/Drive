import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-shared-list',
  templateUrl: 'shared-list.html',
  styleUrls: ['shared-list.css'],
  standalone: true,
  imports: [CommonModule, RouterLink]
})
export class SharedListComponent implements OnInit {
  activeTab: 'by-me' | 'with-me' = 'by-me';
  sharedItems: any[] = [];
  loading: boolean = false;
  
  private apiUrl = 'http://localhost:8080/api/files';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private dialogService: ConfirmDialogService) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.loadData();
    }
  }


  setTab(tab: 'by-me' | 'with-me') {
    this.activeTab = tab;
    this.loadData();
  }

  loadData() {
    this.loading = true;
    const endpoint = this.activeTab === 'by-me' ? `${this.apiUrl}/list/shared-by-me` : `${this.apiUrl}/list/shared-with-me`;
    
    // ÄÃ£ gá»¡ bá» this.getHeaders()
    this.http.get(endpoint).subscribe({
      next: (data: any) => {
        this.sharedItems = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => this.loading = false
    });
  }

  async revokeAccess(shareId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'Thu há»“i quyá»n truy cáº­p',
      message: 'Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n thu há»“i quyá»n truy cáº­p nÃ y khÃ´ng?',
      confirmText: 'Thu há»“i',
      type: 'warning'
    });
    if (!confirmed) return;
    this.http.delete(`${this.apiUrl}/revoke-share/${shareId}`).subscribe({
      next: () => this.loadData(),
      error: () => this.dialogService.alert({ title: 'Lá»—i', message: 'Lá»—i khi thu há»“i quyá»n truy cáº­p.', type: 'danger' })
    });
  }

  async deleteAccess(shareId: number): Promise<void> {
    const confirmed = await this.dialogService.confirm({
      title: 'XÃ³a chia sáº»',
      message: 'Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n xÃ³a má»¥c chia sáº» Ä‘Ã£ háº¿t háº¡n nÃ y khÃ´ng?',
      confirmText: 'XÃ³a',
      type: 'danger'
    });
    if (!confirmed) return;
    this.http.delete(`${this.apiUrl}/revoke-share/${shareId}`).subscribe({
      next: () => this.loadData(),
      error: () => this.dialogService.alert({ title: 'Lá»—i', message: 'Lá»—i khi xÃ³a má»¥c chia sáº».', type: 'danger' })
    });
  }

  copyLink(token: string) {
    const link = `http://localhost:8080/api/files/shared/${token}`;
    if (navigator.clipboard) {
        navigator.clipboard.writeText(link).then(() => {
            this.dialogService.alert({ title: 'ThÃ nh cÃ´ng', message: 'ÄÃ£ sao chÃ©p liÃªn káº¿t vÃ o clipboard', type: 'success' });
        });
    } else {
        // Fallback for older browsers or non-secure contexts if needed
        const textArea = document.createElement("textarea");
        textArea.value = link;
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand('copy');
            this.dialogService.alert({ title: 'ThÃ nh cÃ´ng', message: 'ÄÃ£ sao chÃ©p liÃªn káº¿t vÃ o clipboard', type: 'success' });
        } catch (err) {
            this.dialogService.alert({ title: 'Lá»—i', message: 'KhÃ´ng thá»ƒ sao chÃ©p liÃªn káº¿t.', type: 'danger' });
        }
        document.body.removeChild(textArea);
    }
  }

  downloadShared(token: string) {
    // Táº¡o tháº» <a> áº©n Ä‘á»ƒ Ã©p trÃ¬nh duyá»‡t táº£i file mÃ  khÃ´ng má»Ÿ tab lá»—i rÃ¡c
    const link = document.createElement('a');
    link.href = `http://localhost:8080/api/files/shared/${token}`;
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  isExpired(expiresAt: string): boolean {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  }
}
