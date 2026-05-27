export interface Notification {
  id: number;
  userId: number;
  type: 'UPGRADE_REQUEST' | 'FILE_SHARED';
  message: string;
  targetUrl: string;
  read: boolean;
  createdAt: string;
}
