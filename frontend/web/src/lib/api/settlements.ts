import { apiClient } from "./client";
import type { Settlement, CreateSettlementRequest } from "@/types/settlement";

export interface SettlementFilters {
  groupId?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export const settlementsApi = {
  getAll: async (filters?: SettlementFilters): Promise<PagedResponse<Settlement>> => {
    const params = new URLSearchParams();
    if (filters?.groupId) params.append("groupId", filters.groupId);
    if (filters?.status) params.append("status", filters.status);
    if (filters?.page !== undefined) params.append("page", String(filters.page));
    if (filters?.size !== undefined) params.append("size", String(filters.size));

    const response = await apiClient.get<PagedResponse<Settlement>>(
      `/api/v1/settlements?${params.toString()}`
    );
    return response.data;
  },

  getById: async (id: string): Promise<Settlement> => {
    const response = await apiClient.get<Settlement>(`/api/v1/settlements/${id}`);
    return response.data;
  },

  create: async (data: CreateSettlementRequest): Promise<Settlement> => {
    const response = await apiClient.post<Settlement>("/api/v1/settlements", data);
    return response.data;
  },

  confirm: async (id: string): Promise<Settlement> => {
    const response = await apiClient.post<Settlement>(
      `/api/v1/settlements/${id}/confirm`
    );
    return response.data;
  },

  reject: async (id: string, reason?: string): Promise<Settlement> => {
    const response = await apiClient.post<Settlement>(
      `/api/v1/settlements/${id}/reject`,
      { reason }
    );
    return response.data;
  },

  cancel: async (id: string): Promise<Settlement> => {
    const response = await apiClient.post<Settlement>(
      `/api/v1/settlements/${id}/cancel`
    );
    return response.data;
  },

  getPending: async (): Promise<Settlement[]> => {
    const response = await apiClient.get<PagedResponse<Settlement>>(
      "/api/v1/settlements?status=PENDING"
    );
    return response.data.content;
  },
};
