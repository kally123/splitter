"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Plus, X } from "lucide-react";
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
import { createGroupSchema, type CreateGroupFormData } from "@/lib/utils/validators";
import { groupsApi } from "@/lib/api";
import type { GroupType } from "@/lib/types/group";

const groupTypes: { value: GroupType; label: string }[] = [
  { value: "HOME", label: "Home" },
  { value: "TRIP", label: "Trip" },
  { value: "COUPLE", label: "Couple" },
  { value: "OTHER", label: "Other" },
];

const currencies = [
  { value: "USD", label: "USD - US Dollar" },
  { value: "EUR", label: "EUR - Euro" },
  { value: "GBP", label: "GBP - British Pound" },
  { value: "INR", label: "INR - Indian Rupee" },
  { value: "JPY", label: "JPY - Japanese Yen" },
  { value: "CAD", label: "CAD - Canadian Dollar" },
  { value: "AUD", label: "AUD - Australian Dollar" },
];

export function CreateGroupForm() {
  const router = useRouter();
  const { toast } = useToast();
  const [isLoading, setIsLoading] = useState(false);
  const [memberEmails, setMemberEmails] = useState<string[]>([]);
  const [emailInput, setEmailInput] = useState("");

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateGroupFormData>({
    resolver: zodResolver(createGroupSchema),
    defaultValues: {
      name: "",
      description: "",
      type: "OTHER",
      currency: "USD",
    },
  });

  const addMemberEmail = () => {
    const email = emailInput.trim().toLowerCase();
    if (email && !memberEmails.includes(email) && email.includes("@")) {
      setMemberEmails([...memberEmails, email]);
      setEmailInput("");
    }
  };

  const removeMemberEmail = (email: string) => {
    setMemberEmails(memberEmails.filter((e) => e !== email));
  };

  const onSubmit = async (data: CreateGroupFormData) => {
    try {
      setIsLoading(true);
      const group = await groupsApi.create({
        ...data,
        memberEmails,
      });

      toast({
        title: "Group created!",
        description: `${group.name} has been created successfully.`,
      });

      router.push(`/groups/${group.id}`);
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Failed to create group",
        description: error.response?.data?.message || "Something went wrong",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-2xl">
      <CardHeader>
        <CardTitle>Create a new group</CardTitle>
        <CardDescription>
          Set up a group to start splitting expenses with friends, family, or roommates.
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-6">
          <div className="space-y-2">
            <Label htmlFor="name">Group Name</Label>
            <Input
              id="name"
              placeholder="Enter group name"
              disabled={isLoading}
              {...register("name")}
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description (optional)</Label>
            <Input
              id="description"
              placeholder="What is this group for?"
              disabled={isLoading}
              {...register("description")}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="type">Group Type</Label>
              <Select
                defaultValue="OTHER"
                onValueChange={(value) => setValue("type", value as GroupType)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  {groupTypes.map((type) => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="currency">Currency</Label>
              <Select
                defaultValue="USD"
                onValueChange={(value) => setValue("currency", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select currency" />
                </SelectTrigger>
                <SelectContent>
                  {currencies.map((currency) => (
                    <SelectItem key={currency.value} value={currency.value}>
                      {currency.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label>Invite Members (optional)</Label>
            <div className="flex gap-2">
              <Input
                placeholder="Enter email address"
                value={emailInput}
                onChange={(e) => setEmailInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addMemberEmail();
                  }
                }}
                disabled={isLoading}
              />
              <Button type="button" onClick={addMemberEmail} disabled={isLoading}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            {memberEmails.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {memberEmails.map((email) => (
                  <div
                    key={email}
                    className="flex items-center gap-1 rounded-full bg-secondary px-3 py-1 text-sm"
                  >
                    <span>{email}</span>
                    <button
                      type="button"
                      onClick={() => removeMemberEmail(email)}
                      className="text-muted-foreground hover:text-foreground"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
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
            {isLoading ? "Creating..." : "Create Group"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
}
