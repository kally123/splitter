"use client";

import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { BalanceSummary } from "@/components/balances";
import { Spinner } from "@/components/ui/spinner";
import { useBalances } from "@/lib/hooks";

export default function BalancesPage() {
  const { balances, isLoading } = useBalances();

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Balances</h1>
            <p className="text-muted-foreground">
              View your balances across all groups.
            </p>
          </div>

          {isLoading ? (
            <div className="flex h-48 items-center justify-center">
              <Spinner size="lg" />
            </div>
          ) : (
            <BalanceSummary balances={balances || []} />
          )}
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
