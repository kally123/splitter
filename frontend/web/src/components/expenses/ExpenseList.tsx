"use client";

import { Receipt } from "lucide-react";
import { Card, Button } from "@/components/ui";
import { ExpenseCard } from "./ExpenseCard";
import { Spinner } from "@/components/ui/spinner";
import { useExpenses } from "@/lib/hooks/useExpenses";
import type { Expense } from "@/lib/types/expense";

interface ExpenseListProps {
  groupId?: string;
}

export function ExpenseList({ groupId }: ExpenseListProps) {
  const { expenses, isLoading, error, deleteExpense } = useExpenses(groupId);

  const handleDelete = async (expenseId: string) => {
    if (confirm("Are you sure you want to delete this expense?")) {
      await deleteExpense(expenseId);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-48 flex-col items-center justify-center gap-2 text-center">
        <p className="text-muted-foreground">Failed to load expenses</p>
        <Button variant="outline" onClick={() => window.location.reload()}>
          Retry
        </Button>
      </div>
    );
  }

  if (!expenses || expenses.length === 0) {
    return (
      <Card className="flex h-48 flex-col items-center justify-center gap-4 p-6 text-center">
        <Receipt className="h-12 w-12 text-muted-foreground" />
        <div>
          <h3 className="font-semibold">No expenses yet</h3>
          <p className="text-sm text-muted-foreground">
            Add your first expense to get started
          </p>
        </div>
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      {expenses.map((expense) => (
        <ExpenseCard
          key={expense.id}
          expense={expense}
          onDelete={handleDelete}
        />
      ))}
    </div>
  );
}
