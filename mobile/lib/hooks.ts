import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "./api";

// Types
export interface Group {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  memberCount: number;
  balance: number;
}

export interface Expense {
  id: string;
  description: string;
  amount: number;
  currency: string;
  category: string;
  paidBy: string;
  paidByName: string;
  groupId: string;
  groupName: string;
  splitType: "equal" | "exact" | "percentage";
  createdAt: string;
  yourShare: number;
}

export interface Balance {
  userId: string;
  userName: string;
  amount: number;
}

// Groups
export function useGroups() {
  return useQuery({
    queryKey: ["groups"],
    queryFn: async () => {
      const { data } = await api.get<Group[]>("/groups");
      return data;
    },
  });
}

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: ["groups", groupId],
    queryFn: async () => {
      const { data } = await api.get<Group>(`/groups/${groupId}`);
      return data;
    },
    enabled: !!groupId,
  });
}

export function useCreateGroup() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (group: { name: string; description?: string }) => {
      const { data } = await api.post<Group>("/groups", group);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
}

// Expenses
export function useExpenses(groupId?: string) {
  return useQuery({
    queryKey: ["expenses", { groupId }],
    queryFn: async () => {
      const params = groupId ? { groupId } : {};
      const { data } = await api.get<Expense[]>("/expenses", { params });
      return data;
    },
  });
}

export function useExpense(expenseId: string) {
  return useQuery({
    queryKey: ["expenses", expenseId],
    queryFn: async () => {
      const { data } = await api.get<Expense>(`/expenses/${expenseId}`);
      return data;
    },
    enabled: !!expenseId,
  });
}

export function useCreateExpense() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (expense: {
      description: string;
      amount: number;
      currency?: string;
      category: string;
      groupId: string;
      splitType: string;
      participants?: { userId: string; share: number }[];
    }) => {
      const { data } = await api.post<Expense>("/expenses", expense);
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      queryClient.invalidateQueries({ queryKey: ["groups", variables.groupId] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
    },
  });
}

// Balances
export function useUserBalances() {
  return useQuery({
    queryKey: ["balances", "user"],
    queryFn: async () => {
      const { data } = await api.get<{
        totalOwed: number;
        totalOwedToYou: number;
        byGroup: { groupId: string; groupName: string; balance: number }[];
      }>("/balances/me");
      return data;
    },
  });
}

export function useGroupBalances(groupId: string) {
  return useQuery({
    queryKey: ["balances", "group", groupId],
    queryFn: async () => {
      const { data } = await api.get<Balance[]>(`/groups/${groupId}/balances`);
      return data;
    },
    enabled: !!groupId,
  });
}

// Settlements
export function useSettlementSuggestions(groupId: string) {
  return useQuery({
    queryKey: ["settlements", "suggestions", groupId],
    queryFn: async () => {
      const { data } = await api.get<{
        from: string;
        fromName: string;
        to: string;
        toName: string;
        amount: number;
      }[]>(`/groups/${groupId}/settlement-suggestions`);
      return data;
    },
    enabled: !!groupId,
  });
}

export function useRecordSettlement() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (settlement: {
      groupId: string;
      toUserId: string;
      amount: number;
    }) => {
      const { data } = await api.post("/settlements", settlement);
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      queryClient.invalidateQueries({ queryKey: ["groups", variables.groupId] });
      queryClient.invalidateQueries({ queryKey: ["settlements"] });
    },
  });
}

// Activity
export function useActivity() {
  return useQuery({
    queryKey: ["activity"],
    queryFn: async () => {
      const { data } = await api.get<{
        id: string;
        type: "expense" | "payment" | "group";
        title: string;
        description: string;
        timestamp: string;
        groupId?: string;
        groupName?: string;
      }[]>("/activity");
      return data;
    },
  });
}

// Receipt Upload
export function useUploadReceipt() {
  return useMutation({
    mutationFn: async (imageUri: string) => {
      const formData = new FormData();
      formData.append("file", {
        uri: imageUri,
        type: "image/jpeg",
        name: "receipt.jpg",
      } as any);
      
      const { data } = await api.post("/receipts/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      return data;
    },
  });
}
