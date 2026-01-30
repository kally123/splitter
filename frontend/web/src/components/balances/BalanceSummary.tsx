"use client";

import { ArrowRight, TrendingUp, TrendingDown } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, Avatar, AvatarFallback, AvatarImage, Button } from "@/components/ui";
import { formatCurrency } from "@/lib/utils/formatters";
import type { Balance } from "@/lib/types/balance";

interface BalanceSummaryProps {
  balances: Balance[];
  currency?: string;
  onSettleUp?: (balance: Balance) => void;
}

export function BalanceSummary({ balances, currency = "USD", onSettleUp }: BalanceSummaryProps) {
  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const totalOwed = balances
    .filter((b) => b.amount > 0)
    .reduce((sum, b) => sum + b.amount, 0);

  const totalOwe = balances
    .filter((b) => b.amount < 0)
    .reduce((sum, b) => sum + Math.abs(b.amount), 0);

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              You are owed
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-green-500" />
              <span className="text-2xl font-bold text-green-600">
                {formatCurrency(totalOwed, currency)}
              </span>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              You owe
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <TrendingDown className="h-5 w-5 text-red-500" />
              <span className="text-2xl font-bold text-red-600">
                {formatCurrency(totalOwe, currency)}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {balances.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Balance Details</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {balances.map((balance) => (
                <div
                  key={`${balance.fromUserId}-${balance.toUserId}`}
                  className="flex items-center justify-between gap-4"
                >
                  <div className="flex items-center gap-3">
                    <Avatar className="h-8 w-8">
                      <AvatarImage src={balance.fromUser?.avatarUrl} />
                      <AvatarFallback className="text-xs">
                        {getInitials(balance.fromUser?.displayName || "U")}
                      </AvatarFallback>
                    </Avatar>
                    <ArrowRight className="h-4 w-4 text-muted-foreground" />
                    <Avatar className="h-8 w-8">
                      <AvatarImage src={balance.toUser?.avatarUrl} />
                      <AvatarFallback className="text-xs">
                        {getInitials(balance.toUser?.displayName || "U")}
                      </AvatarFallback>
                    </Avatar>
                    <div className="text-sm">
                      <span className="font-medium">{balance.fromUser?.displayName}</span>
                      <span className="text-muted-foreground"> owes </span>
                      <span className="font-medium">{balance.toUser?.displayName}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-semibold">
                      {formatCurrency(Math.abs(balance.amount), currency)}
                    </span>
                    {onSettleUp && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onSettleUp(balance)}
                      >
                        Settle
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
