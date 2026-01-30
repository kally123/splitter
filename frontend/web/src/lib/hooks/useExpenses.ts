"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { expensesApi } from "@/lib/api";
import { useToast } from "@/components/ui/use-toast";
import type { CreateExpenseRequest, UpdateExpenseRequest } from "@/lib/types/expense";

export function useExpenses(groupId?: string) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const queryKey = groupId ? ["expenses", groupId] : ["expenses"];

  const { data, isLoading, error, refetch } = useQuery({
    queryKey,
    queryFn: () => (groupId ? expensesApi.getByGroup(groupId) : expensesApi.getAll()),
  });

  const deleteMutation = useMutation({
    mutationFn: (expenseId: string) => expensesApi.delete(expenseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      toast({
        title: "Expense deleted",
        description: "The expense has been deleted.",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to delete expense",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    expenses: data,
    isLoading,
    error,
    refetch,
    deleteExpense: deleteMutation.mutateAsync,
    isDeleting: deleteMutation.isPending,
  };
}

export function useExpense(expenseId: string) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["expenses", "detail", expenseId],
    queryFn: () => expensesApi.getById(expenseId),
    enabled: !!expenseId,
  });

  return {
    expense: data,
    isLoading,
    error,
    refetch,
  };
}

export function useCreateExpense() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (data: CreateExpenseRequest) => expensesApi.create(data),
    onSuccess: (expense) => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      toast({
        title: "Expense added!",
        description: `${expense.description} has been added.`,
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to add expense",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    createExpense: mutation.mutate,
    isCreating: mutation.isPending,
    error: mutation.error,
  };
}

export function useUpdateExpense(expenseId: string) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (data: UpdateExpenseRequest) => expensesApi.update(expenseId, data),
    onSuccess: (expense) => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] });
      queryClient.invalidateQueries({ queryKey: ["balances"] });
      toast({
        title: "Expense updated!",
        description: `${expense.description} has been updated.`,
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to update expense",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    updateExpense: mutation.mutate,
    isUpdating: mutation.isPending,
    error: mutation.error,
  };
}
