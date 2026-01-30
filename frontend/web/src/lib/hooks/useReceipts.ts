import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

interface LineItem {
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

interface ParsedReceipt {
  merchantName?: string;
  merchantAddress?: string;
  date?: string;
  time?: string;
  subtotal?: number;
  tax?: number;
  tip?: number;
  total?: number;
  currency?: string;
  items?: LineItem[];
  paymentMethod?: string;
  confidence?: number;
}

interface Receipt {
  id: string;
  userId: string;
  expenseId?: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: "UPLOADED" | "PROCESSING" | "PARSED" | "FAILED";
  parsedData?: ParsedReceipt;
  errorMessage?: string;
  uploadedAt: string;
  processedAt?: string;
}

interface UploadResponse {
  id: string;
  status: string;
  uploadedAt: string;
}

// Query keys
export const receiptKeys = {
  all: ["receipts"] as const,
  list: () => [...receiptKeys.all, "list"] as const,
  detail: (id: string) => [...receiptKeys.all, "detail", id] as const,
  parsed: (id: string) => [...receiptKeys.all, "parsed", id] as const,
};

// Get all user receipts
export function useReceipts() {
  return useQuery({
    queryKey: receiptKeys.list(),
    queryFn: async () => {
      const response = await api.get<Receipt[]>("/receipts");
      return response.data;
    },
  });
}

// Get single receipt
export function useReceipt(id: string) {
  return useQuery({
    queryKey: receiptKeys.detail(id),
    queryFn: async () => {
      const response = await api.get<Receipt>(`/receipts/${id}`);
      return response.data;
    },
    enabled: !!id,
    refetchInterval: (query) => {
      // Poll for updates while processing
      const data = query.state.data;
      if (data?.status === "UPLOADED" || data?.status === "PROCESSING") {
        return 2000; // Poll every 2 seconds
      }
      return false;
    },
  });
}

// Get parsed receipt data
export function useParsedReceipt(id: string) {
  return useQuery({
    queryKey: receiptKeys.parsed(id),
    queryFn: async () => {
      const response = await api.get<ParsedReceipt>(`/receipts/${id}/parsed`);
      return response.data;
    },
    enabled: !!id,
  });
}

// Upload receipt
export function useUploadReceipt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      const response = await api.post<UploadResponse>("/receipts", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: receiptKeys.list() });
    },
  });
}

// Delete receipt
export function useDeleteReceipt() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/receipts/${id}`);
      return id;
    },
    onSuccess: (id) => {
      queryClient.invalidateQueries({ queryKey: receiptKeys.list() });
      queryClient.removeQueries({ queryKey: receiptKeys.detail(id) });
    },
  });
}

// Link receipt to expense
export function useLinkReceiptToExpense() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ receiptId, expenseId }: { receiptId: string; expenseId: string }) => {
      const response = await api.post<Receipt>(`/receipts/${receiptId}/link`, { expenseId });
      return response.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: receiptKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: receiptKeys.list() });
    },
  });
}

// Get download URL for receipt
export async function getReceiptDownloadUrl(id: string, expirationMinutes = 15): Promise<string> {
  const response = await api.get<string>(
    `/receipts/${id}/download-url?expirationMinutes=${expirationMinutes}`
  );
  return response.data;
}

// Helper to convert parsed receipt to expense data
export function receiptToExpenseData(parsed: ParsedReceipt) {
  return {
    description: parsed.merchantName || "Receipt expense",
    amount: parsed.total || 0,
    currency: parsed.currency || "USD",
    date: parsed.date || new Date().toISOString().split("T")[0],
    // Category could be inferred from merchant name in the future
  };
}
