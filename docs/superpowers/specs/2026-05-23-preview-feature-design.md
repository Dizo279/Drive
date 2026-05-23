# Image/Video Preview Feature - Design Document

**Date:** 2026-05-23  
**Author:** Claude (Brainstorming Session)  
**Status:** Approved  

---

## Overview

This document describes the design for adding an image/video preview feature to the File Manager application. When users double-click on image or video files, a modal overlay will display the media content with options to close or download.

---

## Requirements Summary

### User Requirements
- **Trigger:** Double-click on image/video files opens preview modal
- **Display:** Modal overlay with dark backdrop (similar to existing share modal)
- **Controls:** Close button and Download button only
- **Interaction:** Only Close button works (no ESC key or backdrop click)
- **Video Behavior:** Videos autoplay when modal opens
- **File Types:** Only `image/*` and `video/*` MIME types (no PDFs, text files, etc.)
- **File Info:** No file name or metadata displayed in preview (clean display)
- **Loading/Error:** Show loading spinner while fetching, display error messages on failure

### Technical Requirements
- Reusable architecture following existing patterns (ConfirmDialogService)
- Memory-safe (proper cleanup of object URLs)
- Works in FileListComponent and potentially other components (SharedList, TrashList)
- Maintains existing folder navigation behavior (double-click folders still navigates)

---

## Architecture Overview

The preview system consists of **3 main components**:

### 1. PreviewService (`src/app/core/services/preview.service.ts`)
- Manages preview modal state using Angular signals
- Provides `open(file)` method to trigger preview
- Provides `close()` method to dismiss preview
- Provides `download()` method to download current file
- Automatically fetches file blob from FileService when opened

### 2. PreviewModalComponent (`src/app/core/components/preview-modal/`)
- Standalone Angular component
- Subscribes to PreviewService state
- Renders `<img>` for images or `<video>` for videos
- Displays loading spinner and error messages
- Handles Close and Download button clicks

### 3. Integration with FileListComponent
- Injects PreviewService
- Modifies double-click handler to check file type
- Calls `previewService.open(item)` for images/videos
- Maintains existing folder navigation for folders

**Pattern:** This follows the exact same pattern as the existing `ConfirmDialogService` in the codebase, making it familiar and maintainable.

---

## Component Structure Details

### PreviewService

**State Interface:**
```typescript
interface PreviewState {
  isOpen: boolean;
  fileId: number | null;
  fileName: string;
  mimeType: string;
  fileBlob: Blob | null;
  loading: boolean;
  error: string | null;
}
```

**Methods:**

**`open(file: { id: number, name: string, mimeType: string })`**
- Sets `isOpen = true`, `loading = true`
- Stores file metadata (id, name, mimeType)
- Calls `FileService.downloadFile(id)` to fetch blob
- On success: sets `fileBlob`, `loading = false`
- On error: sets `error` message, `loading = false`

**`close()`**
- Revokes object URL if exists (prevents memory leak)
- Resets state to initial values
- Sets `isOpen = false`

**`download()`**
- Creates download link from current `fileBlob`
- Triggers browser download using `<a>` element
- Uses same logic as existing download functionality in FileListComponent

**State Management:**
- Uses `signal<PreviewState>` for reactive state
- Follows the same pattern as `ConfirmDialogService`

---

### PreviewModalComponent

**Template Structure:**
```html
<div class="modal-overlay" *ngIf="state().isOpen">
  <div class="modal-content">
    <!-- Close button (top-right corner) -->
    <button class="btn-close" (click)="onClose()">×</button>
    
    <!-- Loading state -->
    <div class="loading-container" *ngIf="state().loading">
      <div class="spinner"></div>
      <p>Đang tải...</p>
    </div>
    
    <!-- Error state -->
    <div class="error-container" *ngIf="state().error">
      <svg><!-- Error icon --></svg>
      <p>{{ state().error }}</p>
    </div>
    
    <!-- Image preview -->
    <img *ngIf="isImage()" [src]="objectUrl()" alt="Preview">
    
    <!-- Video preview -->
    <video *ngIf="isVideo()" [src]="objectUrl()" autoplay controls></video>
    
    <!-- Download button (bottom) -->
    <button class="btn-download" (click)="onDownload()" *ngIf="!state().loading && !state().error">
      Download
    </button>
  </div>
</div>
```

**Component Logic:**
- Injects `PreviewService`
- `state = computed(() => previewService.state())`
- `objectUrl = computed(() => URL.createObjectURL(state().fileBlob))` when blob exists
- `isImage()` - checks if mimeType starts with `image/`
- `isVideo()` - checks if mimeType starts with `video/`
- `onClose()` - calls `previewService.close()`
- `onDownload()` - calls `previewService.download()`
- `ngOnDestroy()` - revokes object URLs to prevent memory leaks

**Styling:**
- Reuses CSS patterns from existing share modal
- `.modal-overlay` - dark backdrop covering viewport
- `.modal-content` - centered white container
- Images/videos: `object-fit: contain`, `max-width: 90vw`, `max-height: 90vh`
- Responsive: 95% width on mobile, 90% on desktop

---

## Data Flow

### Scenario: User double-clicks an image file

**Step 1: User Action**
- User double-clicks on a file row in FileListComponent
- Event handler `onItemDoubleClick(item)` is triggered

**Step 2: File Type Check**
```typescript
onItemDoubleClick(item: any) {
  if (item.isFolder) {
    // Navigate into folder (existing behavior)
    this.folderHistory.push({ id: item.id, name: item.name });
    this.currentParentId = item.id;
    this.loadData();
  } else if (item.mimeType?.startsWith('image/') || 
             item.mimeType?.startsWith('video/')) {
    // Open preview (new behavior)
    this.previewService.open(item);
  }
  // Other files: no action
}
```

**Step 3: PreviewService.open()**
- Sets state: `isOpen = true`, `loading = true`, stores file metadata
- Calls `this.fileService.downloadFile(item.id).subscribe(...)`

**Step 4: Fetch File from Backend**
- FileService makes HTTP request: `GET /api/files/{id}/download`
- Backend returns file blob
- Observable emits blob data

**Step 5: PreviewService Receives Blob**
- Sets state: `fileBlob = blob`, `loading = false`
- PreviewModalComponent automatically re-renders due to signal change

**Step 6: PreviewModalComponent Displays Content**
- Creates object URL: `URL.createObjectURL(blob)`
- Renders `<img [src]="objectUrl">` for images
- Renders `<video [src]="objectUrl" autoplay>` for videos
- Video automatically plays due to `autoplay` attribute

**Step 7: User Closes Modal**
- User clicks Close button
- Calls `previewService.close()`
- Revokes object URL: `URL.revokeObjectURL(objectUrl)`
- Resets state: `isOpen = false`, `fileBlob = null`

---

## Error Handling & Loading States

### Loading State

**When Displayed:**
- From the moment `previewService.open()` is called until blob is received
- While blob is being processed

**UI:**
```html
<div class="loading-container">
  <div class="spinner"></div>
  <p>Đang tải...</p>
</div>
```

**Styling:** Reuses existing `.spinner` class from FileListComponent

---

### Error Scenarios

**1. Network Error** - Cannot connect to backend
- Error message: "Không thể tải file. Vui lòng kiểm tra kết nối."

**2. 404 Not Found** - File doesn't exist
- Error message: "File không tồn tại hoặc đã bị xóa."

**3. 403 Forbidden** - No access permission
- Error message: "Bạn không có quyền xem file này."

**4. File Too Large / Timeout**
- Error message: "File quá lớn để preview. Vui lòng tải xuống."

**5. Blob Creation Failed**
- Error message: "Không thể hiển thị file. Vui lòng thử lại."

**Error UI:**
```html
<div class="error-container">
  <svg><!-- Error icon --></svg>
  <p>{{ state().error }}</p>
  <button (click)="onClose()">Đóng</button>
</div>
```

**Error Recovery:**
- User can only close the modal when error occurs
- No "Retry" button (keeps implementation simple)
- To retry, user must double-click the file again

---

### Memory Management

**Critical:** Must revoke object URLs to prevent memory leaks

```typescript
ngOnDestroy() {
  // Revoke URL when component is destroyed
  if (this.objectUrl) {
    URL.revokeObjectURL(this.objectUrl);
  }
}

close() {
  // Revoke URL when closing modal
  if (this.objectUrl) {
    URL.revokeObjectURL(this.objectUrl);
  }
  // Reset state
  this.state.set(initialState);
}
```

---

## Integration & File Changes

### Files to CREATE

1. **`src/app/core/services/preview.service.ts`**
   - PreviewService with signal-based state management
   - Methods: `open()`, `close()`, `download()`
   - Injectable with `providedIn: 'root'`

2. **`src/app/core/components/preview-modal/preview-modal.component.ts`**
   - Standalone component
   - Logic: subscribe to service, render UI, handle events

3. **`src/app/core/components/preview-modal/preview-modal.component.html`**
   - Template: modal overlay + content + buttons

4. **`src/app/core/components/preview-modal/preview-modal.component.css`**
   - Styling for modal (reuses patterns from share modal)

---

### Files to MODIFY

**1. `src/app/features/files/components/file-list/file-list.ts`**

Changes:
- Inject `PreviewService` in constructor
- Rename method `openFolder(item)` → `onItemDoubleClick(item)`
- Add logic to check mimeType and call preview service

Code:
```typescript
constructor(
  // ... existing injections
  private previewService: PreviewService  // ADD THIS
) {}

onItemDoubleClick(item: any): void {  // RENAME from openFolder
  if (item.isFolder) {
    // Existing logic - navigate into folder
    this.folderHistory.push({ id: item.id, name: item.name });
    this.currentParentId = item.id;
    this.loadData();
  } else if (item.mimeType?.startsWith('image/') || 
             item.mimeType?.startsWith('video/')) {
    // New logic - open preview
    this.previewService.open({
      id: item.id,
      name: item.name,
      mimeType: item.mimeType
    });
  }
  // Other files: no action
}
```

**2. `src/app/features/files/components/file-list/file-list.html`**

Changes:
- Change `(dblclick)="openFolder(item)"` → `(dblclick)="onItemDoubleClick(item)"`
- Add `<app-preview-modal></app-preview-modal>` at the end of template

**3. `src/app/app.ts` (Root component)**

Changes:
- Import `PreviewModalComponent`
- Add to `imports` array (like `ConfirmDialogComponent`)
- Add `<app-preview-modal></app-preview-modal>` to `app.html`

---

### CSS Styling Approach

**Reuse from Share Modal:**
- `.modal-overlay` - dark backdrop
- `.modal-content` - white centered box
- Button styling from existing buttons

**New Styles:**
- `.preview-container` - container for img/video
- `img, video` - `object-fit: contain`, `max-width: 90vw`, `max-height: 90vh`
- `.loading-container`, `.error-container` - centered content with spinner/icon

**Responsive:**
- Mobile: modal occupies 95% of screen
- Desktop: modal max-width 90vw, max-height 90vh
- Images/videos scale to fit within modal bounds

---

## Testing Checklist

After implementation, test the following scenarios:

- ✅ Double-click image → opens preview, displays correctly
- ✅ Double-click video → opens preview, autoplays
- ✅ Double-click folder → still navigates (doesn't open preview)
- ✅ Double-click PDF/document → no action
- ✅ Click Close button → closes modal
- ✅ Click Download button → downloads file
- ✅ Loading spinner displays while fetching
- ✅ Error message displays on failure
- ✅ Large files (>5MB) still preview correctly
- ✅ No memory leaks (check DevTools Memory tab)
- ✅ Video controls work (play/pause, volume, fullscreen)
- ✅ Images scale properly on different screen sizes
- ✅ Modal works on mobile devices
- ✅ Multiple open/close cycles work correctly

---

## Future Enhancements (Out of Scope)

These features are NOT included in this design but could be added later:

- Keyboard shortcuts (ESC to close, arrow keys for next/prev)
- Navigation between files (next/previous buttons)
- Zoom controls for images
- Preview for other file types (PDFs, text files)
- Backdrop click to close
- File information display (name, size, date)
- Slideshow mode
- Share button in preview modal

---

## Implementation Notes

- Follow Angular 21 best practices (standalone components, signals)
- Use `inject()` function instead of constructor injection
- Use `computed()` for derived state
- Set `changeDetection: ChangeDetectionStrategy.OnPush`
- Use native control flow (`@if`, `@for`) instead of `*ngIf`, `*ngFor`
- Follow existing code style (2-space indentation, single quotes)
- Reuse existing CSS patterns and variables
- Test on both Chrome and Firefox
- Verify SSR compatibility (check `isPlatformBrowser`)

---

## Conclusion

This design provides a clean, maintainable solution for previewing images and videos in the File Manager application. By following the existing `ConfirmDialogService` pattern, it integrates seamlessly with the current architecture while keeping the implementation simple and focused.
