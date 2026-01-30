"use client";

import Link from "next/link";
import { Plus } from "lucide-react";
import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { Button } from "@/components/ui";
import { GroupList } from "@/components/groups";

export default function GroupsPage() {
  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Groups</h1>
              <p className="text-muted-foreground">
                Manage your expense groups and members.
              </p>
            </div>
            <Button asChild>
              <Link href="/groups/new">
                <Plus className="mr-2 h-4 w-4" />
                Create Group
              </Link>
            </Button>
          </div>

          <GroupList />
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
