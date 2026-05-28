# JWT Refresh Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai access token 30 phút + refresh token 7 ngày (stateless), refresh token qua HTTP-only Secure cookie, tự refresh khi access hết hạn.

**Architecture:** Backend cấp 2 loại JWT với claim `type` và endpoint `/auth/refresh` riêng. Frontend interceptor xử lý 401 bằng 1 luồng refresh đồng bộ, retry đúng 1 lần, fail thì logout. Không lưu refresh token ở DB, logout chỉ clear cookie phía client.

**Tech Stack:** Spring Boot + Jersey (JAX-RS), jjwt, Angular 21, RxJS, HttpInterceptor.

---

### Task 1: Add dual-token support in JwtUtil

**Files:**
- Modify: `backend/src/main/java/com/filemanager/security/JwtUtil.java`
- Modify: `backend/src/main/resources/application.properties`
- Test: `backend/src/test/java/com/filemanager/security/JwtUtilTest.java`

- [ ] **Step 1: Write failing JwtUtil tests for token type and TTL**

```java
@Test
void shouldGenerateAccessTokenWithAccessTypeClaim() { /* assert type=access */ }

@Test
void shouldGenerateRefreshTokenWithRefreshTypeClaim() { /* assert type=refresh */ }

@Test
void shouldRejectRefreshValidationForAccessToken() { /* expect exception */ }
```

- [ ] **Step 2: Run test to verify failure**

Run: `cd backend && mvn test -Dtest=JwtUtilTest`
Expected: FAIL vì chưa có method/access-refresh split.

- [ ] **Step 3: Implement minimal JwtUtil changes**

```java
@Value("${app.jwt.access-expiration-ms}")
private long accessExpirationMs;

@Value("${app.jwt.refresh-expiration-ms}")
private long refreshExpirationMs;

public String generateAccessToken(String username, Long userId) { /* claim type=access */ }
public String generateRefreshToken(String username, Long userId) { /* claim type=refresh */ }
public Claims validateTokenAndGetClaims(String token) { /* existing */ }
public Claims validateAccessTokenAndGetClaims(String token) { /* validate + type */ }
public Claims validateRefreshTokenAndGetClaims(String token) { /* validate + type */ }
```

- [ ] **Step 4: Add config values**

```properties
app.jwt.access-expiration-ms=1800000
app.jwt.refresh-expiration-ms=604800000
```

- [ ] **Step 5: Run tests to verify pass**

Run: `cd backend && mvn test -Dtest=JwtUtilTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/filemanager/security/JwtUtil.java backend/src/main/resources/application.properties backend/src/test/java/com/filemanager/security/JwtUtilTest.java
git commit -m "feat: add access and refresh token generation in JwtUtil"
```

### Task 2: Update auth endpoints for refresh/logout cookie flow

**Files:**
- Modify: `backend/src/main/java/com/filemanager/resource/AuthResource.java`
- Test: `backend/src/test/java/com/filemanager/resource/AuthResourceTest.java`

- [ ] **Step 1: Write failing endpoint tests**

```java
@Test
void loginShouldReturnAccessTokenAndSetRefreshCookie() {}

@Test
void refreshShouldReturnNewAccessTokenWhenCookieValid() {}

@Test
void refreshShouldReturn401WhenCookieMissing() {}

@Test
void logoutShouldClearRefreshCookie() {}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `cd backend && mvn test -Dtest=AuthResourceTest`
Expected: FAIL vì chưa có `/auth/refresh` và `/auth/logout`.

- [ ] **Step 3: Implement login response + cookie set**

```java
@POST
@Path("/login")
public Response login(User loginRequest) {
  // verify user
  String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId());
  String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
  NewCookie refreshCookie = new NewCookie.Builder("refresh_token")
      .value(refreshToken).httpOnly(true).secure(true)
      .sameSite(NewCookie.SameSite.NONE)
      .path("/api/auth").maxAge(7 * 24 * 60 * 60).build();
  return Response.ok(Map.of("accessToken", accessToken, "username", user.getUsername()))
      .cookie(refreshCookie).build();
}
```

- [ ] **Step 4: Implement refresh and logout endpoints**

```java
@POST @Path("/refresh")
public Response refresh(@CookieParam("refresh_token") Cookie refreshCookie) { /* validate refresh token + return accessToken */ }

@POST @Path("/logout")
public Response logout() { /* set refresh_token empty + Max-Age 0 */ }
```

- [ ] **Step 5: Run tests to verify pass**

Run: `cd backend && mvn test -Dtest=AuthResourceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/filemanager/resource/AuthResource.java backend/src/test/java/com/filemanager/resource/AuthResourceTest.java
git commit -m "feat: add refresh and logout auth endpoints"
```

### Task 3: Keep JwtAuthFilter strict for access token only

**Files:**
- Modify: `backend/src/main/java/com/filemanager/filter/JwtAuthFilter.java`
- Test: `backend/src/test/java/com/filemanager/filter/JwtAuthFilterTest.java`

- [ ] **Step 1: Write failing filter test for refresh-token rejection**

```java
@Test
void shouldRejectProtectedRequestWhenBearerTokenTypeIsRefresh() {}
```

- [ ] **Step 2: Run test to verify failure**

Run: `cd backend && mvn test -Dtest=JwtAuthFilterTest`
Expected: FAIL vì filter đang validate token chung.

- [ ] **Step 3: Implement filter validation using access-specific method**

```java
Claims claims = jwtUtil.validateAccessTokenAndGetClaims(token);
```

- [ ] **Step 4: Run test to verify pass**

Run: `cd backend && mvn test -Dtest=JwtAuthFilterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/filemanager/filter/JwtAuthFilter.java backend/src/test/java/com/filemanager/filter/JwtAuthFilterTest.java
git commit -m "fix: enforce access-token type in auth filter"
```

### Task 4: Enable credentialed cross-origin requests

**Files:**
- Modify: `backend/src/main/java/com/filemanager/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/filemanager/filter/CorsFilter.java`
- Test: `backend/src/test/java/com/filemanager/config/CorsConfigTest.java`

- [ ] **Step 1: Write failing CORS credential test**

```java
@Test
void shouldAllowCredentialsAndFrontendOrigin() {}
```

- [ ] **Step 2: Run test to verify failure**

Run: `cd backend && mvn test -Dtest=CorsConfigTest`
Expected: FAIL nếu chưa bật credentials.

- [ ] **Step 3: Implement credentials support**

```java
// Access-Control-Allow-Credentials: true
// Access-Control-Allow-Origin: http://localhost:4200
```

- [ ] **Step 4: Run test to verify pass**

Run: `cd backend && mvn test -Dtest=CorsConfigTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/filemanager/config/SecurityConfig.java backend/src/main/java/com/filemanager/filter/CorsFilter.java backend/src/test/java/com/filemanager/config/CorsConfigTest.java
git commit -m "fix: allow credentialed CORS for auth cookie flow"
```

### Task 5: Add refresh/logout methods in Angular AuthService

**Files:**
- Modify: `frontend/src/app/features/auth/services/auth.service.ts`
- Modify: `frontend/src/app/features/auth/services/auth.service.spec.ts`

- [ ] **Step 1: Write failing AuthService tests**

```ts
it('should send withCredentials on login');
it('should call refresh endpoint withCredentials');
it('should call logout endpoint withCredentials');
```

- [ ] **Step 2: Run tests to verify failure**

Run: `cd frontend && npm run test -- auth.service.spec.ts`
Expected: FAIL vì chưa có refresh/logout API methods.

- [ ] **Step 3: Implement AuthService methods**

```ts
login(credentials: LoginRequest): Observable<LoginResponse> {
  return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials, { withCredentials: true }).pipe(...);
}

refreshToken(): Observable<{ accessToken: string }> {
  return this.http.post<{ accessToken: string }>(`${this.apiUrl}/refresh`, {}, { withCredentials: true });
}

logoutServer(): Observable<string> {
  return this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true, responseType: 'text' });
}
```

- [ ] **Step 4: Run tests to verify pass**

Run: `cd frontend && npm run test -- auth.service.spec.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/auth/services/auth.service.ts frontend/src/app/features/auth/services/auth.service.spec.ts
git commit -m "feat: add auth refresh and logout service methods"
```

### Task 6: Implement interceptor refresh-on-401 with single-flight

**Files:**
- Modify: `frontend/src/app/core/interceptors/jwt-interceptor.ts`
- Test: `frontend/src/app/core/interceptors/jwt-interceptor.spec.ts`

- [ ] **Step 1: Write failing interceptor tests**

```ts
it('should retry request after successful refresh');
it('should logout when refresh fails');
it('should not refresh auth endpoints');
it('should perform one refresh for concurrent 401 responses');
```

- [ ] **Step 2: Run tests to verify failure**

Run: `cd frontend && npm run test -- jwt-interceptor.spec.ts`
Expected: FAIL vì chưa có refresh logic.

- [ ] **Step 3: Implement refresh queue + retry once**

```ts
private isRefreshing = false;
private refreshTokenSubject = new BehaviorSubject<string | null>(null);

// on 401:
// - skip auth endpoints
// - if not refreshing: call refreshToken(), publish token
// - if refreshing: wait subject
// - retry original request once with new token
```

- [ ] **Step 4: Ensure logout flow clears local state on refresh failure**

```ts
this.authService.logout();
return throwError(() => error);
```

- [ ] **Step 5: Run tests to verify pass**

Run: `cd frontend && npm run test -- jwt-interceptor.spec.ts`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/core/interceptors/jwt-interceptor.ts frontend/src/app/core/interceptors/jwt-interceptor.spec.ts
git commit -m "feat: auto-refresh access token on 401"
```

### Task 7: Integrate logout UI flow with server logout endpoint

**Files:**
- Modify: `frontend/src/app/features/auth/services/auth.service.ts`
- Modify: `frontend/src/app/features/account/components/account-settings/account-settings.ts`
- Test: `frontend/src/app/features/account/components/account-settings/account-settings.spec.ts`

- [ ] **Step 1: Write failing component test for logout server call**

```ts
it('should call logout endpoint then clear local session');
```

- [ ] **Step 2: Run test to verify failure**

Run: `cd frontend && npm run test -- account-settings.spec.ts`
Expected: FAIL vì logout server chưa được dùng từ UI.

- [ ] **Step 3: Implement logout orchestration**

```ts
this.authService.logoutServer().subscribe({
  next: () => this.authService.logout(),
  error: () => this.authService.logout()
});
```

- [ ] **Step 4: Run test to verify pass**

Run: `cd frontend && npm run test -- account-settings.spec.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/auth/services/auth.service.ts frontend/src/app/features/account/components/account-settings/account-settings.ts frontend/src/app/features/account/components/account-settings/account-settings.spec.ts
git commit -m "feat: call server logout before clearing local auth"
```

### Task 8: End-to-end verification for JWT security behavior

**Files:**
- Modify: `backend/src/test/java/com/filemanager/resource/AuthResourceIntegrationTest.java`
- Modify: `frontend/src/app/features/auth/services/auth.service.spec.ts`

- [ ] **Step 1: Add integration test for full login-refresh-logout lifecycle**

```java
@Test
void shouldLoginRefreshAndLogoutWithCookieLifecycle() {}
```

- [ ] **Step 2: Run backend integration test**

Run: `cd backend && mvn test -Dtest=AuthResourceIntegrationTest`
Expected: PASS.

- [ ] **Step 3: Run focused frontend auth/interceptor tests**

Run: `cd frontend && npm run test -- auth.service.spec.ts jwt-interceptor.spec.ts`
Expected: PASS.

- [ ] **Step 4: Manual verification in browser**

Run: `cd frontend && ng serve`
Run: `cd backend && mvn spring-boot:run`
Expected:
- login response sets `refresh_token` cookie with `HttpOnly/Secure/SameSite=None`
- expired access token triggers silent refresh
- missing refresh cookie causes redirect to login
- logout clears refresh cookie (`Max-Age=0`)

- [ ] **Step 5: Final commit**

```bash
git add backend/src/test/java/com/filemanager/resource/AuthResourceIntegrationTest.java frontend/src/app/features/auth/services/auth.service.spec.ts
git commit -m "test: verify stateless refresh token auth lifecycle"
```
