"use client";

import { Receipt, MoreVertical, Edit, Trash } from "lucide-react";
import {
  Card,
  Avatar,
  AvatarFallback,
  AvatarImage,
  Badge,
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui";
import { formatCurrency, formatDate } from "@/lib/utils/formatters";
import type { Expense } from "@/lib/types/expense";

interface ExpenseCardProps {
  expense: Expense;
  onEdit?: (expense: Expense) => void;
  onDelete?: (expenseId: string) => void;
}

const categoryIcons: Record<string, string> = {
  FOOD: "🍔",
  TRANSPORT: "🚗",
  ACCOMMODATION: "🏠",
  ENTERTAINMENT: "🎬",
  SHOPPING: "🛍️",
  UTILITIES: "💡",
  HEALTHCARE: "🏥",
  OTHER: "📝",
};

export function ExpenseCard({ expense, onEdit, onDelete }: ExpenseCardProps) {
  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Card className="p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted text-xl">
            {categoryIcons[expense.category] || "📝"}
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h4 className="font-medium">{expense.description}</h4>
              <Badge variant="outline" className="text-xs">
                {expense.category}
              </Badge>
            </div>
            <div className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
              <Avatar className="h-5 w-5">
                <AvatarImage src={expense.paidBy.avatarUrl} />
                <AvatarFallback className="text-[10px]">
                  {getInitials(expense.paidBy.displayName)}
                </AvatarFallback>
              </Avatar>
              <span>Paid by {expense.paidBy.displayName}</span>
              <span>•</span>
              <span>{formatDate(new Date(expense.date))}</span>
            </div>
            <div className="mt-2 flex flex-wrap gap-1">
              {expense.splits.slice(0, 3).map((split) => (
                <Badge key={split.userId} variant="secondary" className="text-xs">
                  {split.displayName}: {formatCurrency(split.amount, expense.currency)}
                </Badge>
              ))}
              {expense.splits.length > 3 && (
                <Badge variant="secondary" className="text-xs">
                  +{expense.splits.length - 3} more
                </Badge>
              )}
            </div>
          </div>
        </div>
        <div className="flex items-start gap-2">
          <span className="font-semibold">
            {formatCurrency(expense.amount, expense.currency)}
          </span>
          {(onEdit || onDelete) && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                {onEdit && (
                  <DropdownMenuItem onClick={() => onEdit(expense)}>
                    <Edit className="mr-2 h-4 w-4" />
                    Edit
                  </DropdownMenuItem>
                )}
                {onDelete && (
                  <DropdownMenuItem
                    className="text-destructive"
                    onClick={() => onDelete(expense.id)}
                  >
                    <Trash className="mr-2 h-4 w-4" />
                    Delete
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </div>
    </Card>
  );
}
