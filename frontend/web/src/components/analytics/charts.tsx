"use client";

import { useMemo } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn, formatCurrency } from "@/lib/utils";

// Color palette for charts
const COLORS = [
  "hsl(var(--chart-1))",
  "hsl(var(--chart-2))",
  "hsl(var(--chart-3))",
  "hsl(var(--chart-4))",
  "hsl(var(--chart-5))",
];

interface SpendingChartProps {
  data: Array<{
    date: string;
    amount: number;
    count?: number;
  }>;
  currency?: string;
  title?: string;
  description?: string;
  type?: "area" | "line" | "bar";
  className?: string;
}

export function SpendingChart({
  data,
  currency = "USD",
  title = "Spending Trend",
  description,
  type = "area",
  className,
}: SpendingChartProps) {
  const ChartComponent = type === "bar" ? BarChart : type === "line" ? LineChart : AreaChart;
  
  return (
    <Card className={cn("w-full", className)}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <ChartComponent data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
            <XAxis
              dataKey="date"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => formatCurrency(value, currency, true)}
            />
            <Tooltip
              content={({ active, payload, label }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="bg-background border rounded-lg shadow-lg p-3">
                      <p className="text-sm font-medium">{label}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatCurrency(payload[0].value as number, currency)}
                      </p>
                    </div>
                  );
                }
                return null;
              }}
            />
            {type === "bar" ? (
              <Bar dataKey="amount" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
            ) : type === "line" ? (
              <Line
                type="monotone"
                dataKey="amount"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                dot={false}
              />
            ) : (
              <Area
                type="monotone"
                dataKey="amount"
                stroke="hsl(var(--primary))"
                fill="hsl(var(--primary))"
                fillOpacity={0.2}
              />
            )}
          </ChartComponent>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

interface CategoryPieChartProps {
  data: Array<{
    category: string;
    amount: number;
    percentage: number;
  }>;
  currency?: string;
  title?: string;
  className?: string;
}

export function CategoryPieChart({
  data,
  currency = "USD",
  title = "Spending by Category",
  className,
}: CategoryPieChartProps) {
  return (
    <Card className={cn("w-full", className)}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              fill="#8884d8"
              paddingAngle={2}
              dataKey="amount"
              nameKey="category"
              label={({ category, percentage }) => `${category} (${percentage.toFixed(1)}%)`}
              labelLine={false}
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const data = payload[0].payload;
                  return (
                    <div className="bg-background border rounded-lg shadow-lg p-3">
                      <p className="text-sm font-medium">{data.category}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatCurrency(data.amount, currency)} ({data.percentage.toFixed(1)}%)
                      </p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

interface GroupComparisonChartProps {
  data: Array<{
    groupName: string;
    amount: number;
    count: number;
  }>;
  currency?: string;
  title?: string;
  className?: string;
}

export function GroupComparisonChart({
  data,
  currency = "USD",
  title = "Spending by Group",
  className,
}: GroupComparisonChartProps) {
  return (
    <Card className={cn("w-full", className)}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={data} layout="vertical" margin={{ top: 10, right: 30, left: 100, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" horizontal={false} />
            <XAxis
              type="number"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => formatCurrency(value, currency, true)}
            />
            <YAxis
              type="category"
              dataKey="groupName"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              width={90}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const data = payload[0].payload;
                  return (
                    <div className="bg-background border rounded-lg shadow-lg p-3">
                      <p className="text-sm font-medium">{data.groupName}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatCurrency(data.amount, currency)}
                      </p>
                      <p className="text-xs text-muted-foreground">{data.count} expenses</p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Bar dataKey="amount" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]}>
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

interface MemberBalanceChartProps {
  data: Array<{
    userName: string;
    balance: number;
  }>;
  currency?: string;
  title?: string;
  className?: string;
}

export function MemberBalanceChart({
  data,
  currency = "USD",
  title = "Member Balances",
  className,
}: MemberBalanceChartProps) {
  // Sort by balance and split into positive/negative
  const sortedData = useMemo(() => 
    [...data].sort((a, b) => b.balance - a.balance),
    [data]
  );

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={Math.max(200, data.length * 40)}>
          <BarChart data={sortedData} layout="vertical" margin={{ top: 10, right: 30, left: 100, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" horizontal={false} />
            <XAxis
              type="number"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => formatCurrency(value, currency, true)}
            />
            <YAxis
              type="category"
              dataKey="userName"
              tick={{ fontSize: 12 }}
              tickLine={false}
              axisLine={false}
              width={90}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const data = payload[0].payload;
                  return (
                    <div className="bg-background border rounded-lg shadow-lg p-3">
                      <p className="text-sm font-medium">{data.userName}</p>
                      <p className={cn(
                        "text-sm",
                        data.balance >= 0 ? "text-green-600" : "text-red-600"
                      )}>
                        {data.balance >= 0 ? "Gets back " : "Owes "}
                        {formatCurrency(Math.abs(data.balance), currency)}
                      </p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Bar dataKey="balance" radius={[0, 4, 4, 0]}>
              {sortedData.map((entry) => (
                <Cell
                  key={entry.userName}
                  fill={entry.balance >= 0 ? "hsl(var(--chart-2))" : "hsl(var(--destructive))"}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
