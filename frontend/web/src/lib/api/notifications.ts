import { apiClient } from "./client";
import type {
  Notification,
  NotificationPreference,
  UpdatePreferencesRequest,
} from "@/types/notification";

export const notificationsApi = {
  getAll: async (page = 0, size = 20): Promise<{ content: Notification[]; totalElements: number }> => {
    const response = await apiClient.get(
      `/api/v1/notifications?page=${page}&size=${size}`
    );
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await apiClient.get<{ count: number }>(
      "/api/v1/notifications/unread/count"
    );
    return response.data.count;
  },

  markAsRead: async (id: string): Promise<void> => {
    await apiClient.post(`/api/v1/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.post("/api/v1/notifications/read-all");
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/notifications/${id}`);
  },

  // Preferences
  getPreferences: async (): Promise<NotificationPreference[]> => {
    const response = await apiClient.get<NotificationPreference[]>(
      "/api/v1/notifications/preferences"
    );
    return response.data;
  },

  updatePreferences: async (data: UpdatePreferencesRequest): Promise<void> => {
    await apiClient.put("/api/v1/notifications/preferences", data);
  },
};
