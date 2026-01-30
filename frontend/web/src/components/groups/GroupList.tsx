"use client";

import { Plus } from "lucide-react";
import Link from "next/link";
import { Button, Card } from "@/components/ui";
import { GroupCard } from "./GroupCard";
import { Spinner } from "@/components/ui/spinner";
import { useGroups } from "@/lib/hooks/useGroups";

export function GroupList() {
  const { groups, isLoading, error } = useGroups();

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
        <p className="text-muted-foreground">Failed to load groups</p>
        <Button variant="outline" onClick={() => window.location.reload()}>
          Retry
        </Button>
      </div>
    );
  }

  if (!groups || groups.length === 0) {
    return (
      <Card className="flex h-48 flex-col items-center justify-center gap-4 p-6 text-center">
        <div>
          <h3 className="font-semibold">No groups yet</h3>
          <p className="text-sm text-muted-foreground">
            Create your first group to start splitting expenses
          </p>
        </div>
        <Button asChild>
          <Link href="/groups/new">
            <Plus className="mr-2 h-4 w-4" />
            Create Group
          </Link>
        </Button>
      </Card>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {groups.map((group) => (
        <GroupCard key={group.id} group={group} />
      ))}
    </div>
  );
}
