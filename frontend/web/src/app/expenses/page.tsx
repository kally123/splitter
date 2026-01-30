"use client";

import Link from "next/link";
import { Plus } from "lucide-react";
import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { Button } from "@/components/ui";
import { ExpenseList } from "@/components/expenses";

export default function ExpensesPage() {
  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Expenses</h1>
              <p className="text-muted-foreground">
                View and manage all your expenses.
              </p>
            </div>
            <Button asChild>
              <Link href="/expenses/new">
                <Plus className="mr-2 h-4 w-4" />
                Add Expense
              </Link>
            </Button>
          </div>

          <ExpenseList />
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
