import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Notification } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = environment.apiUrl + '/notifications';
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  private sseEventSource: EventSource | null = null;

  constructor(private http: HttpClient, private zone: NgZone) {}

  loadNotifications(): void {
    this.http.get<Notification[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.notificationsSubject.next(data);
        this.updateUnreadCount(data);
      },
      error: (err) => console.error('Lỗi tải thông báo:', err)
    });
  }

  markAsRead(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/read`, {});
  }

  deleteNotification(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  markAllAsRead(): void {
    const notifications = this.notificationsSubject.value;
    notifications.filter(n => !n.read).forEach(n => {
      this.markAsRead(n.id).subscribe(() => {
        n.read = true;
        this.notificationsSubject.next([...notifications]);
        this.updateUnreadCount(notifications);
      });
    });
  }

  connectSSE(): void {
    if (typeof window === 'undefined' || typeof localStorage === 'undefined') return;
    const token = localStorage.getItem('jwt_token');
    if (!token) return;

    if (typeof EventSource === 'undefined') return;
    this.sseEventSource = new EventSource(`${this.apiUrl}/stream?token=${token}`);

    this.sseEventSource.addEventListener('notification', (event) => {
      this.zone.run(() => {
        try {
          const notification: Notification = JSON.parse(event.data);
          const current = this.notificationsSubject.value;
          this.notificationsSubject.next([notification, ...current]);
          this.updateUnreadCount([notification, ...current]);
        } catch (e) {
          console.error('Lỗi parse SSE notification:', e);
        }
      });
    });

    this.sseEventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };
  }

  disconnectSSE(): void {
    if (this.sseEventSource) {
      this.sseEventSource.close();
      this.sseEventSource = null;
    }
  }

  private updateUnreadCount(notifications: Notification[]): void {
    const count = notifications.filter(n => !n.read).length;
    this.unreadCountSubject.next(count);
  }
}
