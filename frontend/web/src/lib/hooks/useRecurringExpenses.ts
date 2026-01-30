import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { toast } from 'sonner';

export interface RecurringExpense {
  id: string;
  groupId: string;
  createdBy: string;
  description: string;
  amount: number;
  currency: string;
  category?: string;
  splitType: string;
  frequency: 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'YEARLY';
  intervalValue: number;
  dayOfWeek?: number;
  dayOfMonth?: number;
  monthOfYear?: number;
  startDate: string;
  endDate?: string;
  nextOccurrence: string;
  lastGenerated?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRecurringExpenseInput {
  groupId: string;
  description: string;
  amount: number;
  currency: string;
  category?: string;
  splitType?: string;
  frequency: string;
  intervalValue?: number;
  dayOfWeek?: number;
  dayOfMonth?: number;
  monthOfYear?: number;
  startDate: string;
  endDate?: string;
}

export interface UpdateRecurringExpenseInput {
  description?: string;
  amount?: number;
  currency?: string;
  category?: string;
  splitType?: string;
  frequency?: string;
  intervalValue?: number;
  dayOfWeek?: number;
  dayOfMonth?: number;
  endDate?: string;
}

export function useRecurringExpenses(groupId?: string) {
  return useQuery<RecurringExpense[]>({
    queryKey: ['recurring-expenses', groupId],
    queryFn: async () => {
      const params = groupId ? `?groupId=${groupId}` : '/my';
      const response = await api.get(`/api/v1/recurring-expenses${params}`);
      return response.data;
    },
    enabled: true,
  });
}

export function useRecurringExpense(id: string) {
  return useQuery<RecurringExpense>({
    queryKey: ['recurring-expenses', id],
    queryFn: async () => {
      const response = await api.get(`/api/v1/recurring-expenses/${id}`);
      return response.data;
    },
    enabled: !!id,
  });
}

export function useCreateRecurringExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (input: CreateRecurringExpenseInput) => {
      const response = await api.post('/api/v1/recurring-expenses', input);
      return response.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['recurring-expenses'] });
      toast.success('Recurring expense created successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create recurring expense');
    },
  });
}

export function useUpdateRecurringExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, ...input }: UpdateRecurringExpenseInput & { id: string }) => {
      const response = await api.put(`/api/v1/recurring-expenses/${id}`, input);
      return response.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['recurring-expenses'] });
      toast.success('Recurring expense updated successfully');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to update recurring expense');
    },
  });
}

export function usePauseRecurringExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post(`/api/v1/recurring-expenses/${id}/pause`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recurring-expenses'] });
      toast.success('Recurring expense paused');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to pause recurring expense');
    },
  });
}

export function useResumeRecurringExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post(`/api/v1/recurring-expenses/${id}/resume`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recurring-expenses'] });
      toast.success('Recurring expense resumed');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to resume recurring expense');
    },
  });
}

export function useDeleteRecurringExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/recurring-expenses/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recurring-expenses'] });
      toast.success('Recurring expense deleted');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to delete recurring expense');
    },
  });
}

// Helper function to format frequency for display
export function formatFrequency(expense: RecurringExpense): string {
  const { frequency, intervalValue, dayOfWeek, dayOfMonth } = expense;
  
  const days = ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
  
  switch (frequency) {
    case 'DAILY':
      return intervalValue === 1 ? 'Daily' : `Every ${intervalValue} days`;
    case 'WEEKLY':
      const day = dayOfWeek ? ` on ${days[dayOfWeek]}` : '';
      return intervalValue === 1 ? `Weekly${day}` : `Every ${intervalValue} weeks${day}`;
    case 'BIWEEKLY':
      return 'Every 2 weeks';
    case 'MONTHLY':
      const dayStr = dayOfMonth ? ` on the ${dayOfMonth}${getOrdinalSuffix(dayOfMonth)}` : '';
      return intervalValue === 1 ? `Monthly${dayStr}` : `Every ${intervalValue} months${dayStr}`;
    case 'YEARLY':
      return intervalValue === 1 ? 'Yearly' : `Every ${intervalValue} years`;
    default:
      return frequency;
  }
}

function getOrdinalSuffix(n: number): string {
  const s = ['th', 'st', 'nd', 'rd'];
  const v = n % 100;
  return s[(v - 20) % 10] || s[v] || s[0];
}
