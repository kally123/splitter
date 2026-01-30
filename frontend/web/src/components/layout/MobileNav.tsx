"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { X } from "lucide-react";
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

export function MobileNav() {
  const pathname = usePathname();
  const { isMobileNavOpen, closeMobileNav } = useUIStore();

  if (!isMobileNavOpen) return null;

  return (
    <>
      {/* Overlay */}
      <div
        className="fixed inset-0 z-40 bg-black/50 md:hidden"
        onClick={closeMobileNav}
      />

      {/* Drawer */}
      <div className="fixed inset-y-0 left-0 z-50 w-72 bg-background shadow-lg md:hidden">
        <div className="flex h-16 items-center justify-between border-b px-4">
          <Link href="/dashboard" className="flex items-center gap-2" onClick={closeMobileNav}>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold">
              S
            </div>
            <span className="font-bold">Splitter</span>
          </Link>
          <Button variant="ghost" size="icon" onClick={closeMobileNav}>
            <X className="h-5 w-5" />
          </Button>
        </div>

        <div className="flex flex-col gap-2 p-4">
          <Button asChild className="mb-4 gap-2" onClick={closeMobileNav}>
            <Link href="/expenses/new">
              <Plus className="h-4 w-4" />
              <span>Add Expense</span>
            </Link>
          </Button>

          <nav className="flex flex-col gap-1">
            {navigation.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  onClick={closeMobileNav}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  )}
                >
                  <item.icon className="h-5 w-5" />
                  <span>{item.name}</span>
                </Link>
              );
            })}
          </nav>
        </div>
      </div>
    </>
  );
}
