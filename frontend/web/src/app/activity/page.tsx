"use client";

import { ProtectedRoute } from "@/components/auth";
import { DashboardLayout } from "@/components/layout";
import { Card, CardContent, CardHeader, CardTitle, Button } from "@/components/ui";
import { Spinner } from "@/components/ui/spinner";
import { useNotifications } from "@/lib/hooks";
import { formatRelativeTime } from "@/lib/utils/formatters";
import { Bell, Check } from "lucide-react";

export default function ActivityPage() {
  const { notifications, isLoading, markAsRead, markAllAsRead } = useNotifications();

  return (
    <ProtectedRoute>
      <DashboardLayout>
        <div className="space-y-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight">Activity</h1>
              <p className="text-muted-foreground">
                View your recent activity and notifications.
              </p>
            </div>
            {notifications.length > 0 && (
              <Button variant="outline" onClick={() => markAllAsRead()}>
                <Check className="mr-2 h-4 w-4" />
                Mark all as read
              </Button>
            )}
          </div>

          {isLoading ? (
            <div className="flex h-48 items-center justify-center">
              <Spinner size="lg" />
            </div>
          ) : notifications.length === 0 ? (
            <Card className="flex h-48 flex-col items-center justify-center gap-4 text-center">
              <Bell className="h-12 w-12 text-muted-foreground" />
              <div>
                <h3 className="font-semibold">No activity yet</h3>
                <p className="text-sm text-muted-foreground">
                  Your activity and notifications will appear here
                </p>
              </div>
            </Card>
          ) : (
            <Card>
              <CardContent className="p-0">
                <div className="divide-y">
                  {notifications.map((notification) => (
                    <div
                      key={notification.id}
                      className="flex items-start gap-4 p-4 hover:bg-muted/50 cursor-pointer"
                      onClick={() => !notification.read && markAsRead(notification.id)}
                    >
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                        <Bell className="h-5 w-5 text-primary" />
                      </div>
                      <div className="flex-1">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-medium">{notification.title}</p>
                            <p className="text-sm text-muted-foreground">
                              {notification.message}
                            </p>
                          </div>
                          {!notification.read && (
                            <div className="h-2 w-2 rounded-full bg-primary" />
                          )}
                        </div>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {formatRelativeTime(new Date(notification.createdAt))}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </DashboardLayout>
    </ProtectedRoute>
  );
}
