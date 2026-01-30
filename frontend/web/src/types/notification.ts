export type NotificationType =
  | "EXPENSE_ADDED"
  | "EXPENSE_UPDATED"
  | "EXPENSE_DELETED"
  | "SETTLEMENT_REQUESTED"
  | "SETTLEMENT_CONFIRMED"
  | "SETTLEMENT_REJECTED"
  | "GROUP_INVITATION"
  | "GROUP_MEMBER_JOINED"
  | "GROUP_MEMBER_LEFT"
  | "REMINDER"
  | "SYSTEM";

export type NotificationChannel = "IN_APP" | "EMAIL" | "PUSH";

export interface Notification {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  data?: Record<string, unknown>;
  createdAt: string;
}

export interface NotificationPreference {
  userId: string;
  channel: NotificationChannel;
  enabled: boolean;
  notificationType?: NotificationType;
}

export interface UpdatePreferencesRequest {
  preferences: {
    channel: NotificationChannel;
    enabled: boolean;
    notificationType?: NotificationType;
  }[];
}
