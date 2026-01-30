"use client";

import Link from "next/link";
import { Users, ArrowRight } from "lucide-react";
import { Card, CardContent, CardFooter, CardHeader, CardTitle, Badge, Avatar, AvatarFallback, AvatarImage } from "@/components/ui";
import { formatCurrency } from "@/lib/utils/formatters";
import type { Group } from "@/lib/types/group";

interface GroupCardProps {
  group: Group;
  userBalance?: number;
}

export function GroupCard({ group, userBalance = 0 }: GroupCardProps) {
  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const getBalanceColor = (balance: number) => {
    if (balance > 0) return "text-green-600";
    if (balance < 0) return "text-red-600";
    return "text-muted-foreground";
  };

  const getBalanceText = (balance: number) => {
    if (balance > 0) return `You are owed ${formatCurrency(balance, group.currency)}`;
    if (balance < 0) return `You owe ${formatCurrency(Math.abs(balance), group.currency)}`;
    return "Settled up";
  };

  return (
    <Link href={`/groups/${group.id}`}>
      <Card className="transition-all hover:shadow-md hover:border-primary/50">
        <CardHeader className="pb-2">
          <div className="flex items-start justify-between">
            <CardTitle className="text-lg">{group.name}</CardTitle>
            <Badge variant={group.type === "HOME" ? "default" : "secondary"}>
              {group.type}
            </Badge>
          </div>
          {group.description && (
            <p className="text-sm text-muted-foreground line-clamp-2">
              {group.description}
            </p>
          )}
        </CardHeader>
        <CardContent className="pb-2">
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm text-muted-foreground">
              {group.memberCount} members
            </span>
          </div>
          <div className="mt-3 flex -space-x-2">
            {group.members?.slice(0, 5).map((member) => (
              <Avatar key={member.userId} className="h-8 w-8 border-2 border-background">
                <AvatarImage src={member.avatarUrl} />
                <AvatarFallback className="text-xs">
                  {getInitials(member.displayName)}
                </AvatarFallback>
              </Avatar>
            ))}
            {group.memberCount > 5 && (
              <div className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-background bg-muted text-xs font-medium">
                +{group.memberCount - 5}
              </div>
            )}
          </div>
        </CardContent>
        <CardFooter className="flex items-center justify-between border-t pt-4">
          <span className={`text-sm font-medium ${getBalanceColor(userBalance)}`}>
            {getBalanceText(userBalance)}
          </span>
          <ArrowRight className="h-4 w-4 text-muted-foreground" />
        </CardFooter>
      </Card>
    </Link>
  );
}
