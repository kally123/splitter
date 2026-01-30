"use client";

import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { CreateGroupForm } from "@/components/groups";

export default function NewGroupPage() {
  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="flex justify-center">
          <CreateGroupForm />
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
