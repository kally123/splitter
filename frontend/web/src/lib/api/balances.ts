import { apiClient } from "./client";
import type { BalanceSummary, GroupBalance, SimplifiedDebt, UserBalance } from "@/types/balance";

export const balancesApi = {
  getUserBalances: async (): Promise<UserBalance[]> => {
    const response = await apiClient.get<UserBalance[]>("/api/v1/balances/user");
    return response.data;
  },

  getGroupBalances: async (groupId: string): Promise<GroupBalance> => {
    const response = await apiClient.get<GroupBalance>(
      `/api/v1/balances/group/${groupId}`
    );
    return response.data;
  },

  getSimplifiedDebts: async (groupId: string): Promise<SimplifiedDebt[]> => {
    const response = await apiClient.get<SimplifiedDebt[]>(
      `/api/v1/balances/group/${groupId}/summary`
    );
    return response.data;
  },

  getBalanceSummary: async (): Promise<BalanceSummary> => {
    const response = await apiClient.get<BalanceSummary>("/api/v1/balances/summary");
    return response.data;
  },

  getBalanceBetweenUsers: async (
    userId1: string,
    userId2: string
  ): Promise<{ amount: number; currency: string }> => {
    const response = await apiClient.get(
      `/api/v1/balances/between/${userId1}/${userId2}`
    );
    return response.data;
  },
};
