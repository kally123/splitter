export type SettlementStatus = "PENDING" | "CONFIRMED" | "REJECTED" | "CANCELLED";

export type PaymentMethod = 
  | "CASH"
  | "BANK_TRANSFER"
  | "VENMO"
  | "PAYPAL"
  | "ZELLE"
  | "CREDIT_CARD"
  | "CHECK"
  | "OTHER";

export interface Settlement {
  id: string;
  fromUserId: string;
  fromUserName: string;
  toUserId: string;
  toUserName: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: SettlementStatus;
  groupId?: string;
  groupName?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSettlementRequest {
  fromUserId: string;
  toUserId: string;
  amount: number;
  currency?: string;
  paymentMethod: PaymentMethod;
  groupId?: string;
  notes?: string;
}

export const PAYMENT_METHOD_CONFIG: Record<PaymentMethod, { label: string; icon: string }> = {
  CASH: { label: "Cash", icon: "💵" },
  BANK_TRANSFER: { label: "Bank Transfer", icon: "🏦" },
  VENMO: { label: "Venmo", icon: "📱" },
  PAYPAL: { label: "PayPal", icon: "💳" },
  ZELLE: { label: "Zelle", icon: "⚡" },
  CREDIT_CARD: { label: "Credit Card", icon: "💳" },
  CHECK: { label: "Check", icon: "📝" },
  OTHER: { label: "Other", icon: "💰" },
};
