export type SplitType = "EQUAL" | "EXACT" | "PERCENTAGE" | "SHARES";

export type ExpenseCategory =
  | "FOOD_AND_DRINK"
  | "GROCERIES"
  | "SHOPPING"
  | "ENTERTAINMENT"
  | "TRANSPORTATION"
  | "UTILITIES"
  | "RENT"
  | "HEALTHCARE"
  | "EDUCATION"
  | "TRAVEL"
  | "SPORTS"
  | "GIFTS"
  | "OTHER";

export interface ExpenseShare {
  userId: string;
  displayName: string;
  shareAmount: number;
  sharePercentage?: number;
  shareUnits?: number;
  isPaid: boolean;
}

export interface Expense {
  id: string;
  groupId?: string;
  groupName?: string;
  description: string;
  amount: number;
  currency: string;
  category: ExpenseCategory;
  paidBy: string;
  paidByName: string;
  splitType: SplitType;
  shares: ExpenseShare[];
  expenseDate: string;
  receiptUrl?: string;
  notes?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpenseRequest {
  groupId?: string;
  description: string;
  amount: number;
  currency?: string;
  category: ExpenseCategory;
  paidBy: string;
  splitType: SplitType;
  participants: ParticipantShare[];
  expenseDate: string;
  notes?: string;
}

export interface ParticipantShare {
  userId: string;
  amount?: number;
  percentage?: number;
  shares?: number;
}

export interface UpdateExpenseRequest {
  description?: string;
  amount?: number;
  currency?: string;
  category?: ExpenseCategory;
  paidBy?: string;
  splitType?: SplitType;
  participants?: ParticipantShare[];
  expenseDate?: string;
  notes?: string;
}

export const CATEGORY_CONFIG: Record<ExpenseCategory, { label: string; icon: string; color: string }> = {
  FOOD_AND_DRINK: { label: "Food & Drink", icon: "🍔", color: "#f97316" },
  GROCERIES: { label: "Groceries", icon: "🛒", color: "#22c55e" },
  SHOPPING: { label: "Shopping", icon: "🛍️", color: "#ec4899" },
  ENTERTAINMENT: { label: "Entertainment", icon: "🎬", color: "#a855f7" },
  TRANSPORTATION: { label: "Transportation", icon: "🚗", color: "#3b82f6" },
  UTILITIES: { label: "Utilities", icon: "💡", color: "#eab308" },
  RENT: { label: "Rent", icon: "🏠", color: "#6366f1" },
  HEALTHCARE: { label: "Healthcare", icon: "🏥", color: "#ef4444" },
  EDUCATION: { label: "Education", icon: "📚", color: "#14b8a6" },
  TRAVEL: { label: "Travel", icon: "✈️", color: "#0ea5e9" },
  SPORTS: { label: "Sports", icon: "⚽", color: "#84cc16" },
  GIFTS: { label: "Gifts", icon: "🎁", color: "#f43f5e" },
  OTHER: { label: "Other", icon: "📋", color: "#64748b" },
};
