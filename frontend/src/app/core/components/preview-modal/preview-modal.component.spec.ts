// src/app/core/components/preview-modal/preview-modal.component.spec.ts
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PreviewModalComponent } from './preview-modal.component';
import { PreviewService } from '../../services/preview.service';
import { signal } from '@angular/core';

describe('PreviewModalComponent', () => {
  let component: PreviewModalComponent;
  let fixture: ComponentFixture<PreviewModalComponent>;
  let mockPreviewService: any;

  beforeEach(async () => {
    mockPreviewService = {
      state: signal({
        isOpen: false,
        fileId: null,
        fileName: '',
        mimeType: '',
        fileBlob: null,
        loading: false,
        error: null
      }),
      close: jasmine.createSpy('close')
    };

    await TestBed.configureTestingModule({
      imports: [PreviewModalComponent],
      providers: [
        { provide: PreviewService, useValue: mockPreviewService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PreviewModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not display modal when isOpen is false', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.modal-overlay')).toBeNull();
  });

  it('should display modal when isOpen is true', () => {
    mockPreviewService.state.set({
      ...mockPreviewService.state(),
      isOpen: true,
      mimeType: 'image/jpeg'
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.modal-overlay')).toBeTruthy();
  });
});
