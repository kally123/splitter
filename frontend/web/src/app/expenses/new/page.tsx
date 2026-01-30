"use client";

import { useSearchParams } from "next/navigation";
import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { CreateExpenseForm } from "@/components/expenses";
import { Spinner } from "@/components/ui/spinner";
import { useGroups } from "@/lib/hooks";

export default function NewExpensePage() {
  const searchParams = useSearchParams();
  const defaultGroupId = searchParams.get("groupId") || undefined;
  const { groups, isLoading } = useGroups();

  if (isLoading) {
    return (
      <ProtectedRoute>
        <DashboardLayout>
          <div className="flex h-[50vh] items-center justify-center">
            <Spinner size="lg" />
          </div>
        </DashboardLayout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="flex justify-center">
          <CreateExpenseForm groups={groups || []} defaultGroupId={defaultGroupId} />
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
