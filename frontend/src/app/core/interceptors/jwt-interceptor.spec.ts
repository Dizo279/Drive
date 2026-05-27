import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptorsFromDi,
  HTTP_INTERCEPTORS
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';

import { JwtInterceptor } from './jwt-interceptor';
import { AuthService } from '@features/auth/services/auth.service';

describe('JwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        {
          provide: HTTP_INTERCEPTORS,
          useClass: JwtInterceptor,
          multi: true
        }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    localStorage.clear();
    localStorage.setItem('jwt_token', 'old-access-token');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should retry request after successful refresh', async () => {
    const requestPromise = firstValueFrom(http.get('http://localhost:8080/api/files/list'));

    const firstReq = httpMock.expectOne('http://localhost:8080/api/files/list');
    expect(firstReq.request.headers.get('Authorization')).toBe('Bearer old-access-token');
    firstReq.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    refreshReq.flush({ accessToken: 'new-access-token' });

    const retriedReq = httpMock.expectOne('http://localhost:8080/api/files/list');
    expect(retriedReq.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    retriedReq.flush({ ok: true });

    const response = await requestPromise;
    expect(response).toEqual({ ok: true });
  });

  it('should logout when refresh fails', async () => {
    const logoutSpy = vi.spyOn(authService, 'logout');

    const requestPromise = firstValueFrom(http.get('http://localhost:8080/api/files/list'));

    const firstReq = httpMock.expectOne('http://localhost:8080/api/files/list');
    firstReq.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    refreshReq.flush('refresh-fail', { status: 401, statusText: 'Unauthorized' });

    await expect(requestPromise).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(logoutSpy).toHaveBeenCalled();
  });

  it('should not refresh auth endpoints', async () => {
    const requestPromise = firstValueFrom(http.post('http://localhost:8080/api/auth/login', {}));

    const loginReq = httpMock.expectOne('http://localhost:8080/api/auth/login');
    loginReq.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    await expect(requestPromise).rejects.toBeInstanceOf(HttpErrorResponse);
    httpMock.expectNone('http://localhost:8080/api/auth/refresh');
  });

  it('should perform one refresh for concurrent 401 responses', async () => {
    const p1 = firstValueFrom(http.get('http://localhost:8080/api/files/a'));
    const p2 = firstValueFrom(http.get('http://localhost:8080/api/files/b'));

    const reqA1 = httpMock.expectOne('http://localhost:8080/api/files/a');
    const reqB1 = httpMock.expectOne('http://localhost:8080/api/files/b');
    reqA1.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });
    reqB1.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    refreshReq.flush({ accessToken: 'new-access-token' });

    const reqA2 = httpMock.expectOne('http://localhost:8080/api/files/a');
    const reqB2 = httpMock.expectOne('http://localhost:8080/api/files/b');
    reqA2.flush({ id: 'a' });
    reqB2.flush({ id: 'b' });

    await expect(p1).resolves.toEqual({ id: 'a' });
    await expect(p2).resolves.toEqual({ id: 'b' });
  });
});
