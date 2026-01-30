"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Plus, Settings, Users, ArrowLeft } from "lucide-react";
import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { Button, Card, CardContent, CardHeader, CardTitle, Badge, Tabs, TabsList, TabsTrigger, TabsContent, Avatar, AvatarFallback, AvatarImage } from "@/components/ui";
import { Spinner } from "@/components/ui/spinner";
import { ExpenseList } from "@/components/expenses";
import { BalanceSummary, SettleUpModal } from "@/components/balances";
import { useGroup, useBalances } from "@/lib/hooks";
import type { Balance } from "@/lib/types/balance";

export default function GroupDetailPage() {
  const params = useParams();
  const groupId = params.id as string;
  const { group, isLoading } = useGroup(groupId);
  const { balances } = useBalances(groupId);
  const [settleModalOpen, setSettleModalOpen] = useState(false);
  const [selectedBalance, setSelectedBalance] = useState<Balance | null>(null);

  const handleSettleUp = (balance: Balance) => {
    setSelectedBalance(balance);
    setSettleModalOpen(true);
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

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

  if (!group) {
    return (
      <ProtectedRoute>
        <DashboardLayout>
          <div className="flex h-[50vh] flex-col items-center justify-center gap-4">
            <p className="text-muted-foreground">Group not found</p>
            <Button asChild>
              <Link href="/groups">Back to Groups</Link>
            </Button>
          </div>
        </DashboardLayout>
      </ProtectedRoute>
    );
  }

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          {/* Header */}
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="icon" asChild>
                <Link href="/groups">
                  <ArrowLeft className="h-4 w-4" />
                </Link>
              </Button>
              <div className="flex-1">
                <div className="flex items-center gap-3">
                  <h1 className="text-3xl font-bold tracking-tight">{group.name}</h1>
                  <Badge variant="outline">{group.type}</Badge>
                </div>
                {group.description && (
                  <p className="text-muted-foreground">{group.description}</p>
                )}
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="icon" asChild>
                  <Link href={`/groups/${groupId}/settings`}>
                    <Settings className="h-4 w-4" />
                  </Link>
                </Button>
                <Button asChild>
                  <Link href={`/expenses/new?groupId=${groupId}`}>
                    <Plus className="mr-2 h-4 w-4" />
                    Add Expense
                  </Link>
                </Button>
              </div>
            </div>

            {/* Members */}
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="flex items-center gap-2 text-sm font-medium">
                  <Users className="h-4 w-4" />
                  Members ({group.memberCount})
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-3">
                  {group.members?.map((member) => (
                    <div key={member.userId} className="flex items-center gap-2">
                      <Avatar className="h-8 w-8">
                        <AvatarImage src={member.avatarUrl} />
                        <AvatarFallback className="text-xs">
                          {getInitials(member.displayName)}
                        </AvatarFallback>
                      </Avatar>
                      <div className="text-sm">
                        <p className="font-medium">{member.displayName}</p>
                        <p className="text-xs text-muted-foreground">{member.role}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Tabs */}
          <Tabs defaultValue="expenses">
            <TabsList>
              <TabsTrigger value="expenses">Expenses</TabsTrigger>
              <TabsTrigger value="balances">Balances</TabsTrigger>
            </TabsList>
            <TabsContent value="expenses" className="mt-4">
              <ExpenseList groupId={groupId} />
            </TabsContent>
            <TabsContent value="balances" className="mt-4">
              <BalanceSummary
                balances={balances || []}
                currency={group.currency}
                onSettleUp={handleSettleUp}
              />
            </TabsContent>
          </Tabs>
        </div>

        <SettleUpModal
          open={settleModalOpen}
          onOpenChange={setSettleModalOpen}
          balance={selectedBalance}
          groupId={groupId}
        />
      </DashboardLayout>
    </ProtectedRoute>
  );
}
