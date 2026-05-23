import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PreviewService } from '../../services/preview.service';
import { PreviewModalComponent } from './preview-modal.component';

describe('PreviewModalComponent', () => {
  let component: PreviewModalComponent;
  let fixture: ComponentFixture<PreviewModalComponent>;
  let mockPreviewService: {
    state: ReturnType<typeof signal<any>>;
    close: ReturnType<typeof vi.fn>;
    download: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:preview-url'),
      revokeObjectURL: vi.fn()
    });

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
      close: vi.fn(),
      download: vi.fn()
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

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not display modal when isOpen is false', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.preview-backdrop')).toBeNull();
  });

  it('should display modal when isOpen is true', () => {
    mockPreviewService.state.set({
      ...mockPreviewService.state(),
      isOpen: true,
      mimeType: 'image/jpeg'
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.preview-backdrop')).toBeTruthy();
  });

  it('should render an image preview from a blob', () => {
    mockPreviewService.state.set({
      isOpen: true,
      fileId: 1,
      fileName: 'test.jpg',
      mimeType: 'image/jpeg',
      fileBlob: new Blob(['image'], { type: 'image/jpeg' }),
      loading: false,
      error: null
    });
    fixture.detectChanges();

    const image = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(image).toBeTruthy();
    expect(image.src).toContain('blob:preview-url');
  });

  it('should delegate close and download actions to the service', () => {
    mockPreviewService.state.set({
      isOpen: true,
      fileId: 1,
      fileName: 'test.jpg',
      mimeType: 'image/jpeg',
      fileBlob: new Blob(['image'], { type: 'image/jpeg' }),
      loading: false,
      error: null
    });
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.preview-download') as HTMLButtonElement).click();
    (fixture.nativeElement.querySelector('.preview-close') as HTMLButtonElement).click();

    expect(mockPreviewService.download).toHaveBeenCalled();
    expect(mockPreviewService.close).toHaveBeenCalled();
  });
});
