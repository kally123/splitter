import { apiClient } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

export const authApi = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>("/api/v1/auth/login", data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>("/api/v1/auth/register", data);
    return response.data;
  },

  refresh: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>("/api/v1/auth/refresh", {
      refreshToken,
    });
    return response.data;
  },

  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post("/api/v1/auth/logout", { refreshToken });
  },

  logoutAll: async (): Promise<void> => {
    await apiClient.post("/api/v1/auth/logout-all");
  },

  validate: async (): Promise<{ valid: boolean; userId: string; email: string }> => {
    const response = await apiClient.get("/api/v1/auth/validate");
    return response.data;
  },
};
