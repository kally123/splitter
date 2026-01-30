"use client";

import { cn } from "@/lib/utils/cn";
import { useUIStore } from "@/lib/stores";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";
import { MobileNav } from "./MobileNav";

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isSidebarOpen } = useUIStore();

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <Sidebar />
      <MobileNav />
      <main
        className={cn(
          "min-h-[calc(100vh-4rem)] pt-0 transition-all duration-300",
          isSidebarOpen ? "md:ml-64" : "md:ml-16"
        )}
      >
        <div className="container mx-auto p-4 md:p-6">{children}</div>
      </main>
    </div>
  );
}
