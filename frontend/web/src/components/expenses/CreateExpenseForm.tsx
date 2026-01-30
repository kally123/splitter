"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { CalendarIcon } from "lucide-react";
import {
  Button,
  Input,
  Label,
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui";
import { useToast } from "@/components/ui/use-toast";
import { createExpenseSchema, type CreateExpenseFormData } from "@/lib/utils/validators";
import { expensesApi } from "@/lib/api";
import type { ExpenseCategory, SplitType } from "@/lib/types/expense";
import type { Group } from "@/lib/types/group";

interface CreateExpenseFormProps {
  groups: Group[];
  defaultGroupId?: string;
}

const categories: { value: ExpenseCategory; label: string }[] = [
  { value: "FOOD", label: "🍔 Food & Drinks" },
  { value: "TRANSPORT", label: "🚗 Transport" },
  { value: "ACCOMMODATION", label: "🏠 Accommodation" },
  { value: "ENTERTAINMENT", label: "🎬 Entertainment" },
  { value: "SHOPPING", label: "🛍️ Shopping" },
  { value: "UTILITIES", label: "💡 Utilities" },
  { value: "HEALTHCARE", label: "🏥 Healthcare" },
  { value: "OTHER", label: "📝 Other" },
];

const splitTypes: { value: SplitType; label: string; description: string }[] = [
  { value: "EQUAL", label: "Equal", description: "Split evenly among all members" },
  { value: "EXACT", label: "Exact amounts", description: "Specify exact amounts for each person" },
  { value: "PERCENTAGE", label: "Percentages", description: "Split by percentage" },
  { value: "SHARES", label: "Shares", description: "Split by number of shares" },
];

export function CreateExpenseForm({ groups, defaultGroupId }: CreateExpenseFormProps) {
  const router = useRouter();
  const { toast } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(
    groups.find((g) => g.id === defaultGroupId) || null
  );

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateExpenseFormData>({
    resolver: zodResolver(createExpenseSchema),
    defaultValues: {
      groupId: defaultGroupId || "",
      description: "",
      amount: 0,
      category: "OTHER",
      splitType: "EQUAL",
      date: new Date().toISOString().split("T")[0],
    },
  });

  const onSubmit = async (data: CreateExpenseFormData) => {
    try {
      setIsLoading(true);
      
      const expense = await expensesApi.create({
        groupId: data.groupId,
        description: data.description,
        amount: data.amount,
        currency: selectedGroup?.currency || "USD",
        category: data.category,
        splitType: data.splitType,
        date: data.date,
        splits: [], // For EQUAL split, backend calculates
      });

      toast({
        title: "Expense added!",
        description: `${data.description} has been added successfully.`,
      });

      router.push(`/groups/${data.groupId}`);
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Failed to add expense",
        description: error.response?.data?.message || "Something went wrong",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-2xl">
      <CardHeader>
        <CardTitle>Add an expense</CardTitle>
        <CardDescription>
          Record an expense and split it with your group members.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="groupId">Group</Label>
            <Select
              defaultValue={defaultGroupId}
              onValueChange={(value) => {
                setValue("groupId", value);
                setSelectedGroup(groups.find((g) => g.id === value) || null);
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a group" />
              </SelectTrigger>
              <SelectContent>
                {groups.map((group) => (
                  <SelectItem key={group.id} value={group.id}>
                    {group.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.groupId && (
              <p className="text-sm text-destructive">{errors.groupId.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input
              id="description"
              placeholder="What was this expense for?"
              disabled={isLoading}
              {...register("description")}
            />
            {errors.description && (
              <p className="text-sm text-destructive">{errors.description.message}</p>
            )}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="amount">Amount</Label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-muted-foreground">
                  {selectedGroup?.currency || "USD"}
                </span>
                <Input
                  id="amount"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  className="pl-14"
                  disabled={isLoading}
                  {...register("amount", { valueAsNumber: true })}
                />
              </div>
              {errors.amount && (
                <p className="text-sm text-destructive">{errors.amount.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="date">Date</Label>
              <Input
                id="date"
                type="date"
                disabled={isLoading}
                {...register("date")}
              />
              {errors.date && (
                <p className="text-sm text-destructive">{errors.date.message}</p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="category">Category</Label>
              <Select
                defaultValue="OTHER"
                onValueChange={(value) => setValue("category", value as ExpenseCategory)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((category) => (
                    <SelectItem key={category.value} value={category.value}>
                      {category.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="splitType">Split Type</Label>
              <Select
                defaultValue="EQUAL"
                onValueChange={(value) => setValue("splitType", value as SplitType)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="How to split?" />
                </SelectTrigger>
                <SelectContent>
                  {splitTypes.map((type) => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
        <CardFooter className="flex justify-between">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.back()}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Adding..." : "Add Expense"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
