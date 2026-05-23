import { TestBed } from '@angular/core/testing';
import { FileService } from '@features/files/services/file.service';
import { Observable, of, throwError } from 'rxjs';
import { PreviewService } from './preview.service';

describe('PreviewService', () => {
  let service: PreviewService;
  let fileService: { downloadFile: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    fileService = {
      downloadFile: vi.fn(() => of(new Blob(['test'], { type: 'image/jpeg' })))
    };

    TestBed.configureTestingModule({
      providers: [
        PreviewService,
        { provide: FileService, useValue: fileService }
      ]
    });

    service = TestBed.inject(PreviewService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have initial state with isOpen false', () => {
    const state = service.state();

    expect(state.isOpen).toBe(false);
    expect(state.fileId).toBeNull();
    expect(state.fileName).toBe('');
    expect(state.mimeType).toBe('');
    expect(state.fileBlob).toBeNull();
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('should fetch file blob and update state on success', () => {
    const mockBlob = new Blob(['image'], { type: 'image/jpeg' });
    fileService.downloadFile.mockReturnValue(of(mockBlob));

    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });

    expect(fileService.downloadFile).toHaveBeenCalledWith(1);
    expect(service.state()).toEqual({
      isOpen: true,
      fileId: 1,
      fileName: 'test.jpg',
      mimeType: 'image/jpeg',
      fileBlob: mockBlob,
      loading: false,
      error: null
    });
  });

  it('should set loading true while the file is being fetched', () => {
    fileService.downloadFile.mockReturnValue(new Observable<Blob>());

    service.open({ id: 2, name: 'video.mp4', mimeType: 'video/mp4' });

    expect(service.state()).toMatchObject({
      isOpen: true,
      fileId: 2,
      fileName: 'video.mp4',
      mimeType: 'video/mp4',
      fileBlob: null,
      loading: true,
      error: null
    });
  });

  it('should set a friendly error on fetch failure', () => {
    fileService.downloadFile.mockReturnValue(throwError(() => ({ status: 404 })));

    service.open({ id: 1, name: 'missing.jpg', mimeType: 'image/jpeg' });

    expect(service.state().loading).toBe(false);
    expect(service.state().error).toBe('File không tồn tại hoặc đã bị xóa.');
  });

  it('should reset state and cancel pending work when close() is called', () => {
    fileService.downloadFile.mockReturnValue(new Observable<Blob>());
    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });

    service.close();

    const state = service.state();
    expect(state.isOpen).toBe(false);
    expect(state.fileId).toBeNull();
    expect(state.fileName).toBe('');
    expect(state.fileBlob).toBeNull();
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });
});
