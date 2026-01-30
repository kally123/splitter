"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { settlementsApi } from "@/lib/api";
import { useToast } from "@/components/ui/use-toast";
import type { CreateSettlementRequest } from "@/lib/types/settlement";

export function useSettlements(groupId?: string) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: groupId ? ["settlements", groupId] : ["settlements"],
    queryFn: () => (groupId ? settlementsApi.getByGroup(groupId) : settlementsApi.getAll()),
  });

  return {
    settlements: data,
    isLoading,
    error,
    refetch,
  };
}

export function useCreateSettlement() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (data: CreateSettlementRequest) => settlementsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["settlements"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      toast({
        title: "Settlement recorded!",
        description: "The payment has been recorded.",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to record settlement",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    createSettlement: mutation.mutate,
    isCreating: mutation.isPending,
    error: mutation.error,
  };
}

export function useConfirmSettlement() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (settlementId: string) => settlementsApi.confirm(settlementId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["settlements"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      toast({
        title: "Settlement confirmed!",
        description: "The payment has been confirmed.",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to confirm settlement",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    confirmSettlement: mutation.mutate,
    isConfirming: mutation.isPending,
    error: mutation.error,
  };
}
