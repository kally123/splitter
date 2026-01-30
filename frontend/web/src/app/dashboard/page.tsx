"use client";

import Link from "next/link";
import { Plus, TrendingUp, TrendingDown, Users, Receipt, ArrowRight } from "lucide-react";
import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { Card, CardContent, CardHeader, CardTitle, Button, Badge } from "@/components/ui";
import { Spinner } from "@/components/ui/spinner";
import { GroupCard } from "@/components/groups";
import { useGroups, useUserBalance, useExpenses, useWebSocket } from "@/lib/hooks";
import { formatCurrency, formatRelativeTime } from "@/lib/utils/formatters";

export default function DashboardPage() {
  // Initialize WebSocket connection
  useWebSocket();

  const { groups, isLoading: groupsLoading } = useGroups();
  const { summary, isLoading: balanceLoading } = useUserBalance();
  const { expenses, isLoading: expensesLoading } = useExpenses();

  const recentGroups = groups?.slice(0, 3) || [];
  const recentExpenses = expenses?.slice(0, 5) || [];

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
              <p className="text-muted-foreground">
                Welcome back! Here's your expense overview.
              </p>
            </div>
            <div className="flex gap-2">
              <Button asChild variant="outline">
                <Link href="/groups/new">
                  <Users className="mr-2 h-4 w-4" />
                  New Group
                </Link>
              </Button>
              <Button asChild>
                <Link href="/expenses/new">
                  <Plus className="mr-2 h-4 w-4" />
                  Add Expense
                </Link>
              </Button>
            </div>
          </div>

          {/* Balance Summary Cards */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Balance</CardTitle>
                <Receipt className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                {balanceLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <>
                    <div className="text-2xl font-bold">
                      {formatCurrency(summary?.totalBalance || 0)}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Across all groups
                    </p>
                  </>
                )}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">You Are Owed</CardTitle>
                <TrendingUp className="h-4 w-4 text-green-500" />
              </CardHeader>
              <CardContent>
                {balanceLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <>
                    <div className="text-2xl font-bold text-green-600">
                      {formatCurrency(summary?.totalOwed || 0)}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      From {summary?.owedByCount || 0} people
                    </p>
                  </>
                )}
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">You Owe</CardTitle>
                <TrendingDown className="h-4 w-4 text-red-500" />
              </CardHeader>
              <CardContent>
                {balanceLoading ? (
                  <Spinner size="sm" />
                ) : (
                  <>
                    <div className="text-2xl font-bold text-red-600">
                      {formatCurrency(summary?.totalOwe || 0)}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      To {summary?.oweToCount || 0} people
                    </p>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Recent Groups */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">Recent Groups</h2>
              <Button variant="ghost" asChild>
                <Link href="/groups">
                  View all
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>
            </div>
            {groupsLoading ? (
              <div className="flex h-32 items-center justify-center">
                <Spinner size="lg" />
              </div>
            ) : recentGroups.length === 0 ? (
              <Card className="flex h-32 flex-col items-center justify-center gap-2 text-center">
                <p className="text-muted-foreground">No groups yet</p>
                <Button asChild size="sm">
                  <Link href="/groups/new">Create your first group</Link>
                </Button>
              </Card>
            ) : (
              <div className="grid gap-4 md:grid-cols-3">
                {recentGroups.map((group) => (
                  <GroupCard key={group.id} group={group} />
                ))}
              </div>
            )}
          </div>

          {/* Recent Activity */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">Recent Expenses</h2>
              <Button variant="ghost" asChild>
                <Link href="/expenses">
                  View all
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>
            </div>
            {expensesLoading ? (
              <div className="flex h-32 items-center justify-center">
                <Spinner size="lg" />
              </div>
            ) : recentExpenses.length === 0 ? (
              <Card className="flex h-32 flex-col items-center justify-center gap-2 text-center">
                <p className="text-muted-foreground">No expenses yet</p>
                <Button asChild size="sm">
                  <Link href="/expenses/new">Add your first expense</Link>
                </Button>
              </Card>
            ) : (
              <Card>
                <CardContent className="p-0">
                  <div className="divide-y">
                    {recentExpenses.map((expense) => (
                      <div
                        key={expense.id}
                        className="flex items-center justify-between p-4"
                      >
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted text-xl">
                            📝
                          </div>
                          <div>
                            <p className="font-medium">{expense.description}</p>
                            <p className="text-sm text-muted-foreground">
                              {expense.paidBy?.displayName} •{" "}
                              {formatRelativeTime(new Date(expense.date))}
                            </p>
                          </div>
                        </div>
                        <span className="font-semibold">
                          {formatCurrency(expense.amount, expense.currency)}
                        </span>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
