// src/app/core/services/preview.service.spec.ts
import { TestBed } from '@angular/core/testing';
import { PreviewService } from './preview.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('PreviewService', () => {
  let service: PreviewService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(PreviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
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

  it('should set loading true when open() is called', () => {
    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });

    const state = service.state();
    expect(state.isOpen).toBe(true);
    expect(state.loading).toBe(true);
    expect(state.fileId).toBe(1);
    expect(state.fileName).toBe('test.jpg');
    expect(state.mimeType).toBe('image/jpeg');
  });

  it('should fetch file blob and update state on success', (done) => {
    const mockBlob = new Blob(['test'], { type: 'image/jpeg' });

    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });

    const req = httpMock.expectOne('http://localhost:8080/api/files/1/download');
    expect(req.request.method).toBe('GET');
    req.flush(mockBlob);

    setTimeout(() => {
      const state = service.state();
      expect(state.loading).toBe(false);
      expect(state.fileBlob).toBeTruthy();
      expect(state.error).toBeNull();
      done();
    }, 100);
  });

  it('should set error on fetch failure', (done) => {
    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });

    const req = httpMock.expectOne('http://localhost:8080/api/files/1/download');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });

    setTimeout(() => {
      const state = service.state();
      expect(state.loading).toBe(false);
      expect(state.error).toBe('File không tồn tại hoặc đã bị xóa.');
      done();
    }, 100);
  });

  it('should reset state when close() is called', () => {
    // First open a preview
    service.open({ id: 1, name: 'test.jpg', mimeType: 'image/jpeg' });
    expect(service.state().isOpen).toBe(true);

    // Then close it
    service.close();

    const state = service.state();
    expect(state.isOpen).toBe(false);
    expect(state.fileId).toBeNull();
    expect(state.fileName).toBe('');
    expect(state.fileBlob).toBeNull();
  });
});
