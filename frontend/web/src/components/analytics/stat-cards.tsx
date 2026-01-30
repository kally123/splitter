"use client";

import { TrendingDown, TrendingUp, DollarSign, Users, Receipt, Clock } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn, formatCurrency } from "@/lib/utils";

interface StatCardProps {
  title: string;
  value: string | number;
  description?: string;
  icon?: React.ReactNode;
  trend?: {
    value: number;
    label?: string;
  };
  className?: string;
}

export function StatCard({
  title,
  value,
  description,
  icon,
  trend,
  className,
}: StatCardProps) {
  const TrendIcon = trend && trend.value >= 0 ? TrendingUp : TrendingDown;
  const trendColor = trend && trend.value >= 0 ? "text-green-600" : "text-red-600";

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        {icon && (
          <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
            {icon}
          </div>
        )}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {(description || trend) && (
          <div className="flex items-center gap-2 mt-1">
            {trend && (
              <span className={cn("flex items-center text-xs", trendColor)}>
                <TrendIcon className="h-3 w-3 mr-1" />
                {Math.abs(trend.value).toFixed(1)}%
                {trend.label && ` ${trend.label}`}
              </span>
            )}
            {description && (
              <span className="text-xs text-muted-foreground">{description}</span>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface SpendingSummaryCardsProps {
  summary: {
    totalSpent: number;
    totalOwed: number;
    totalOwedToYou: number;
    netBalance: number;
    expenseCount: number;
    groupCount: number;
    settledCount: number;
    averageExpense: number;
  };
  currency?: string;
  period?: string;
  className?: string;
}

export function SpendingSummaryCards({
  summary,
  currency = "USD",
  period = "this month",
  className,
}: SpendingSummaryCardsProps) {
  return (
    <div className={cn("grid gap-4 md:grid-cols-2 lg:grid-cols-4", className)}>
      <StatCard
        title="Total Spent"
        value={formatCurrency(summary.totalSpent, currency)}
        description={period}
        icon={<DollarSign className="h-4 w-4 text-primary" />}
      />
      <StatCard
        title="You Owe"
        value={formatCurrency(summary.totalOwed, currency)}
        description="pending payments"
        icon={<TrendingDown className="h-4 w-4 text-red-500" />}
      />
      <StatCard
        title="Owed to You"
        value={formatCurrency(summary.totalOwedToYou, currency)}
        description="to be received"
        icon={<TrendingUp className="h-4 w-4 text-green-500" />}
      />
      <StatCard
        title="Net Balance"
        value={formatCurrency(Math.abs(summary.netBalance), currency)}
        description={summary.netBalance >= 0 ? "in your favor" : "you owe"}
        icon={
          <DollarSign
            className={cn(
              "h-4 w-4",
              summary.netBalance >= 0 ? "text-green-500" : "text-red-500"
            )}
          />
        }
      />
    </div>
  );
}

interface QuickStatsProps {
  stats: {
    expenseCount: number;
    groupCount: number;
    settledCount: number;
    averageExpense: number;
  };
  currency?: string;
  className?: string;
}

export function QuickStats({ stats, currency = "USD", className }: QuickStatsProps) {
  return (
    <div className={cn("grid gap-4 md:grid-cols-2 lg:grid-cols-4", className)}>
      <StatCard
        title="Expenses"
        value={stats.expenseCount}
        description="total transactions"
        icon={<Receipt className="h-4 w-4 text-primary" />}
      />
      <StatCard
        title="Groups"
        value={stats.groupCount}
        description="active groups"
        icon={<Users className="h-4 w-4 text-primary" />}
      />
      <StatCard
        title="Settled"
        value={stats.settledCount}
        description="completed payments"
        icon={<Clock className="h-4 w-4 text-primary" />}
      />
      <StatCard
        title="Avg. Expense"
        value={formatCurrency(stats.averageExpense, currency)}
        description="per transaction"
        icon={<DollarSign className="h-4 w-4 text-primary" />}
      />
    </div>
  );
}
