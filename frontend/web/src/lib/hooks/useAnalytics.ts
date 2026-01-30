import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

interface DateRange {
  startDate?: string;
  endDate?: string;
}

// Types
interface CategoryBreakdown {
  category: string;
  amount: number;
  count: number;
  percentage: number;
}

interface GroupBreakdown {
  groupId: string;
  groupName: string;
  amount: number;
  count: number;
  percentage: number;
}

interface DailySpending {
  date: string;
  amount: number;
  count: number;
}

interface MonthlySpending {
  year: number;
  month: number;
  amount: number;
  count: number;
}

interface SpendingSummary {
  userId: string;
  startDate: string;
  endDate: string;
  currency: string;
  totalSpent: number;
  totalOwed: number;
  totalOwedToYou: number;
  netBalance: number;
  expenseCount: number;
  groupCount: number;
  settledCount: number;
  averageExpense: number;
  largestExpense: number;
  byCategory: CategoryBreakdown[];
  byGroup: GroupBreakdown[];
  dailyTrend: DailySpending[];
  monthlyTrend: MonthlySpending[];
}

interface TrendPoint {
  date: string;
  amount: number;
  count: number;
  cumulativeAmount: number;
}

interface Forecast {
  nextPeriodEstimate: number;
  nextMonthEstimate: number;
  confidence: number;
}

interface TrendAnalysis {
  startDate: string;
  endDate: string;
  period: string;
  spending: TrendPoint[];
  averageSpending: number;
  spendingGrowthRate: number;
  trend: "increasing" | "decreasing" | "stable";
  forecast: Forecast;
}

interface CategoryDetail {
  category: string;
  categoryIcon: string;
  amount: number;
  count: number;
  percentage: number;
  averageAmount: number;
  trend: number;
}

interface CategoryAnalysis {
  currency: string;
  totalSpent: number;
  categories: CategoryDetail[];
}

interface MemberContribution {
  userId: string;
  userName: string;
  paidAmount: number;
  shareAmount: number;
  balance: number;
  expenseCount: number;
  contributionPercentage: number;
}

interface GroupAnalytics {
  groupId: string;
  groupName: string;
  startDate: string;
  endDate: string;
  currency: string;
  totalExpenses: number;
  expenseCount: number;
  memberCount: number;
  averageExpensePerMember: number;
  memberContributions: MemberContribution[];
  byCategory: CategoryBreakdown[];
}

// Query keys
export const analyticsKeys = {
  all: ["analytics"] as const,
  spending: (params?: DateRange) => [...analyticsKeys.all, "spending", params] as const,
  trend: (period: string, params?: DateRange) => [...analyticsKeys.all, "trend", period, params] as const,
  categories: (params?: DateRange) => [...analyticsKeys.all, "categories", params] as const,
  group: (groupId: string, params?: DateRange) => [...analyticsKeys.all, "group", groupId, params] as const,
};

// Get spending summary
export function useSpendingSummary(params?: DateRange & { currency?: string }) {
  return useQuery({
    queryKey: analyticsKeys.spending(params),
    queryFn: async () => {
      const searchParams = new URLSearchParams();
      if (params?.startDate) searchParams.set("startDate", params.startDate);
      if (params?.endDate) searchParams.set("endDate", params.endDate);
      if (params?.currency) searchParams.set("currency", params.currency);
      
      const response = await api.get<SpendingSummary>(
        `/analytics/spending?${searchParams.toString()}`
      );
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Get spending trend
export function useSpendingTrend(
  period: "daily" | "weekly" | "monthly" = "daily",
  params?: DateRange
) {
  return useQuery({
    queryKey: analyticsKeys.trend(period, params),
    queryFn: async () => {
      const searchParams = new URLSearchParams();
      searchParams.set("period", period);
      if (params?.startDate) searchParams.set("startDate", params.startDate);
      if (params?.endDate) searchParams.set("endDate", params.endDate);
      
      const response = await api.get<TrendAnalysis>(
        `/analytics/spending/trend?${searchParams.toString()}`
      );
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
  });
}

// Get category analysis
export function useCategoryAnalysis(params?: DateRange) {
  return useQuery({
    queryKey: analyticsKeys.categories(params),
    queryFn: async () => {
      const searchParams = new URLSearchParams();
      if (params?.startDate) searchParams.set("startDate", params.startDate);
      if (params?.endDate) searchParams.set("endDate", params.endDate);
      
      const response = await api.get<CategoryAnalysis>(
        `/analytics/categories?${searchParams.toString()}`
      );
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
  });
}

// Get group analytics
export function useGroupAnalytics(groupId: string, params?: DateRange) {
  return useQuery({
    queryKey: analyticsKeys.group(groupId, params),
    queryFn: async () => {
      const searchParams = new URLSearchParams();
      if (params?.startDate) searchParams.set("startDate", params.startDate);
      if (params?.endDate) searchParams.set("endDate", params.endDate);
      
      const response = await api.get<GroupAnalytics>(
        `/analytics/groups/${groupId}?${searchParams.toString()}`
      );
      return response.data;
    },
    enabled: !!groupId,
    staleTime: 5 * 60 * 1000,
  });
}

// Helper to format trend data for charts
export function formatTrendData(trend: TrendAnalysis) {
  return trend.spending.map((point) => ({
    date: new Date(point.date).toLocaleDateString("en-US", { 
      month: "short", 
      day: "numeric" 
    }),
    amount: point.amount,
    count: point.count,
    cumulative: point.cumulativeAmount,
  }));
}

// Helper to format category data for pie chart
export function formatCategoryData(analysis: CategoryAnalysis) {
  return analysis.categories.map((cat) => ({
    category: cat.category,
    icon: cat.categoryIcon,
    amount: cat.amount,
    percentage: cat.percentage,
    count: cat.count,
  }));
}
