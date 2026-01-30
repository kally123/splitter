"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Button,
  Input,
  Label,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui";
import { useToast } from "@/components/ui/use-toast";
import { createSettlementSchema, type CreateSettlementFormData } from "@/lib/utils/validators";
import { settlementsApi } from "@/lib/api";
import { formatCurrency } from "@/lib/utils/formatters";
import type { Balance } from "@/lib/types/balance";

interface SettleUpModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  balance: Balance | null;
  groupId: string;
  onSuccess?: () => void;
}

const paymentMethods = [
  { value: "CASH", label: "Cash" },
  { value: "BANK_TRANSFER", label: "Bank Transfer" },
  { value: "VENMO", label: "Venmo" },
  { value: "PAYPAL", label: "PayPal" },
  { value: "ZELLE", label: "Zelle" },
  { value: "OTHER", label: "Other" },
];

export function SettleUpModal({
  open,
  onOpenChange,
  balance,
  groupId,
  onSuccess,
}: SettleUpModalProps) {
  const { toast } = useToast();
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm<CreateSettlementFormData>({
    resolver: zodResolver(createSettlementSchema),
    defaultValues: {
      amount: balance?.amount || 0,
      paymentMethod: "CASH",
      note: "",
    },
  });

  const onSubmit = async (data: CreateSettlementFormData) => {
    if (!balance) return;

    try {
      setIsLoading(true);
      await settlementsApi.create({
        groupId,
        fromUserId: balance.fromUserId,
        toUserId: balance.toUserId,
        amount: data.amount,
        paymentMethod: data.paymentMethod,
        note: data.note,
      });

      toast({
        title: "Settlement recorded!",
        description: `Payment of ${formatCurrency(data.amount, "USD")} has been recorded.`,
      });

      reset();
      onOpenChange(false);
      onSuccess?.();
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Failed to record settlement",
        description: error.response?.data?.message || "Something went wrong",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Settle Up</DialogTitle>
          <DialogDescription>
            Record a payment from {balance?.fromUser?.displayName} to{" "}
            {balance?.toUser?.displayName}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="amount">Amount</Label>
              <Input
                id="amount"
                type="number"
                step="0.01"
                min="0"
                max={balance?.amount || 0}
                disabled={isLoading}
                {...register("amount", { valueAsNumber: true })}
              />
              {errors.amount && (
                <p className="text-sm text-destructive">{errors.amount.message}</p>
              )}
              <p className="text-xs text-muted-foreground">
                Total owed: {formatCurrency(balance?.amount || 0, "USD")}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="paymentMethod">Payment Method</Label>
              <Select
                defaultValue="CASH"
                onValueChange={(value) => setValue("paymentMethod", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select payment method" />
                </SelectTrigger>
                <SelectContent>
                  {paymentMethods.map((method) => (
                    <SelectItem key={method.value} value={method.value}>
                      {method.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="note">Note (optional)</Label>
              <Input
                id="note"
                placeholder="Add a note..."
                disabled={isLoading}
                {...register("note")}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Recording..." : "Record Payment"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
