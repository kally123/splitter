import { apiClient } from "./client";
import type {
  Expense,
  CreateExpenseRequest,
  UpdateExpenseRequest,
} from "@/types/expense";

export interface ExpenseFilters {
  groupId?: string;
  category?: string;
  startDate?: string;
  endDate?: string;
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

export const expensesApi = {
  getAll: async (filters?: ExpenseFilters): Promise<PagedResponse<Expense>> => {
    const params = new URLSearchParams();
    if (filters?.groupId) params.append("groupId", filters.groupId);
    if (filters?.category) params.append("category", filters.category);
    if (filters?.startDate) params.append("startDate", filters.startDate);
    if (filters?.endDate) params.append("endDate", filters.endDate);
    if (filters?.page !== undefined) params.append("page", String(filters.page));
    if (filters?.size !== undefined) params.append("size", String(filters.size));

    const response = await apiClient.get<PagedResponse<Expense>>(
      `/api/v1/expenses?${params.toString()}`
    );
    return response.data;
  },

  getByGroup: async (groupId: string, page = 0, size = 20): Promise<PagedResponse<Expense>> => {
    const response = await apiClient.get<PagedResponse<Expense>>(
      `/api/v1/expenses?groupId=${groupId}&page=${page}&size=${size}`
    );
    return response.data;
  },

  getById: async (id: string): Promise<Expense> => {
    const response = await apiClient.get<Expense>(`/api/v1/expenses/${id}`);
    return response.data;
  },

  create: async (data: CreateExpenseRequest): Promise<Expense> => {
    const response = await apiClient.post<Expense>("/api/v1/expenses", data);
    return response.data;
  },

  update: async (id: string, data: UpdateExpenseRequest): Promise<Expense> => {
    const response = await apiClient.put<Expense>(`/api/v1/expenses/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/expenses/${id}`);
  },

  getRecent: async (limit = 10): Promise<Expense[]> => {
    const response = await apiClient.get<PagedResponse<Expense>>(
      `/api/v1/expenses?size=${limit}&sort=createdAt,desc`
    );
    return response.data.content;
  },
};
