'use client';

import { Plus, Users, Receipt, CreditCard, FolderOpen, Search, Bell, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils/cn';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
    icon?: React.ReactNode;
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
  variant?: 'default' | 'card' | 'inline';
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  secondaryAction,
  className,
  variant = 'default',
}: EmptyStateProps) {
  if (variant === 'inline') {
    return (
      <div className={cn('text-center py-8', className)}>
        {icon && (
          <div className="flex justify-center mb-3">
            <div className="p-2 rounded-full bg-muted text-muted-foreground">
              {icon}
            </div>
          </div>
        )}
        <p className="text-sm font-medium text-muted-foreground">{title}</p>
        {description && (
          <p className="text-xs text-muted-foreground mt-1">{description}</p>
        )}
        {action && (
          <Button size="sm" variant="outline" onClick={action.onClick} className="mt-3">
            {action.icon}
            {action.label}
          </Button>
        )}
      </div>
    );
  }

  if (variant === 'card') {
    return (
      <Card className={cn('text-center', className)}>
        <CardHeader>
          {icon && (
            <div className="flex justify-center mb-2">
              <div className="p-4 rounded-full bg-muted">
                {icon}
              </div>
            </div>
          )}
          <CardTitle>{title}</CardTitle>
          {description && <CardDescription>{description}</CardDescription>}
        </CardHeader>
        {(action || secondaryAction) && (
          <CardContent className="flex flex-col items-center gap-2">
            {action && (
              <Button onClick={action.onClick}>
                {action.icon || <Plus className="mr-2 h-4 w-4" />}
                {action.label}
              </Button>
            )}
            {secondaryAction && (
              <Button variant="ghost" onClick={secondaryAction.onClick}>
                {secondaryAction.label}
              </Button>
            )}
          </CardContent>
        )}
      </Card>
    );
  }

  return (
    <div className={cn('flex flex-col items-center justify-center py-12', className)}>
      {icon && (
        <div className="p-4 rounded-full bg-muted mb-4">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-medium text-foreground">{title}</h3>
      {description && (
        <p className="text-sm text-muted-foreground mt-1 text-center max-w-sm">
          {description}
        </p>
      )}
      {(action || secondaryAction) && (
        <div className="flex flex-col sm:flex-row items-center gap-2 mt-6">
          {action && (
            <Button onClick={action.onClick}>
              {action.icon || <Plus className="mr-2 h-4 w-4" />}
              {action.label}
            </Button>
          )}
          {secondaryAction && (
            <Button variant="ghost" onClick={secondaryAction.onClick}>
              {secondaryAction.label}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

// Pre-built empty states for common use cases

export function EmptyGroups({ onCreateGroup }: { onCreateGroup: () => void }) {
  return (
    <EmptyState
      icon={<Users className="h-8 w-8 text-muted-foreground" />}
      title="No groups yet"
      description="Create your first group to start tracking expenses with friends, family, or roommates."
      action={{
        label: 'Create Group',
        onClick: onCreateGroup,
        icon: <Plus className="mr-2 h-4 w-4" />,
      }}
    />
  );
}

export function EmptyExpenses({
  onAddExpense,
  groupName,
}: {
  onAddExpense: () => void;
  groupName?: string;
}) {
  return (
    <EmptyState
      icon={<Receipt className="h-8 w-8 text-muted-foreground" />}
      title="No expenses yet"
      description={
        groupName
          ? `Add your first expense to ${groupName} to start tracking.`
          : 'Add your first expense to start tracking shared costs.'
      }
      action={{
        label: 'Add Expense',
        onClick: onAddExpense,
        icon: <Plus className="mr-2 h-4 w-4" />,
      }}
    />
  );
}

export function EmptyBalances() {
  return (
    <EmptyState
      icon={<CreditCard className="h-8 w-8 text-muted-foreground" />}
      title="All settled up!"
      description="You don't have any outstanding balances. Great job keeping things even!"
    />
  );
}

export function EmptySearchResults({
  query,
  onClear,
}: {
  query: string;
  onClear?: () => void;
}) {
  return (
    <EmptyState
      icon={<Search className="h-8 w-8 text-muted-foreground" />}
      title="No results found"
      description={`We couldn't find anything matching "${query}". Try a different search term.`}
      action={
        onClear
          ? {
              label: 'Clear Search',
              onClick: onClear,
            }
          : undefined
      }
    />
  );
}

export function EmptyNotifications() {
  return (
    <EmptyState
      icon={<Bell className="h-8 w-8 text-muted-foreground" />}
      title="No notifications"
      description="You're all caught up! New notifications will appear here."
      variant="inline"
    />
  );
}

export function EmptyActivity() {
  return (
    <EmptyState
      icon={<FolderOpen className="h-8 w-8 text-muted-foreground" />}
      title="No recent activity"
      description="Your recent expenses, settlements, and updates will appear here."
    />
  );
}
