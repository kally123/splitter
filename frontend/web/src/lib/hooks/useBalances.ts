"use client";

import { useQuery } from "@tanstack/react-query";
import { balancesApi } from "@/lib/api";

export function useBalances(groupId?: string) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: groupId ? ["balances", groupId] : ["balances"],
    queryFn: () => (groupId ? balancesApi.getByGroup(groupId) : balancesApi.getAll()),
  });

  return {
    balances: data,
    isLoading,
    error,
    refetch,
  };
}

export function useUserBalance() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["balances", "summary"],
    queryFn: () => balancesApi.getSummary(),
  });

  return {
    summary: data,
    isLoading,
    error,
    refetch,
  };
}
