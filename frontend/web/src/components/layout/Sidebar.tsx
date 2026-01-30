"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  Receipt,
  Wallet,
  Activity,
  Settings,
  Plus,
} from "lucide-react";
import { cn } from "@/lib/utils/cn";
import { Button } from "@/components/ui";
import { useUIStore } from "@/lib/stores";

const navigation = [
  {
    name: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    name: "Groups",
    href: "/groups",
    icon: Users,
  },
  {
    name: "Expenses",
    href: "/expenses",
    icon: Receipt,
  },
  {
    name: "Balances",
    href: "/balances",
    icon: Wallet,
  },
  {
    name: "Activity",
    href: "/activity",
    icon: Activity,
  },
  {
    name: "Settings",
    href: "/settings",
    icon: Settings,
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isSidebarOpen } = useUIStore();

  return (
    <aside
      className={cn(
        "fixed left-0 top-16 z-30 hidden h-[calc(100vh-4rem)] w-64 border-r bg-background transition-all duration-300 md:block",
        !isSidebarOpen && "md:w-16"
      )}
    >
      <div className="flex h-full flex-col gap-2 p-4">
        <Button
          asChild
          className="mb-4 gap-2"
          size={isSidebarOpen ? "default" : "icon"}
        >
          <Link href="/expenses/new">
            <Plus className="h-4 w-4" />
            {isSidebarOpen && <span>Add Expense</span>}
          </Link>
        </Button>

        <nav className="flex flex-1 flex-col gap-1">
          {navigation.map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.name}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  !isSidebarOpen && "justify-center px-2"
                )}
              >
                <item.icon className="h-5 w-5 shrink-0" />
                {isSidebarOpen && <span>{item.name}</span>}
              </Link>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}
