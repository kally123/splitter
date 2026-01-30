"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { groupsApi } from "@/lib/api";
import { useToast } from "@/components/ui/use-toast";
import type { CreateGroupRequest, UpdateGroupRequest } from "@/lib/types/group";

export function useGroups() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["groups"],
    queryFn: () => groupsApi.getAll(),
  });

  return {
    groups: data,
    isLoading,
    error,
    refetch,
  };
}

export function useGroup(groupId: string) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["groups", groupId],
    queryFn: () => groupsApi.getById(groupId),
    enabled: !!groupId,
  });

  return {
    group: data,
    isLoading,
    error,
    refetch,
  };
}

export function useCreateGroup() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (data: CreateGroupRequest) => groupsApi.create(data),
    onSuccess: (group) => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      toast({
        title: "Group created!",
        description: `${group.name} has been created successfully.`,
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to create group",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    createGroup: mutation.mutate,
    isCreating: mutation.isPending,
    error: mutation.error,
  };
}

export function useUpdateGroup(groupId: string) {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (data: UpdateGroupRequest) => groupsApi.update(groupId, data),
    onSuccess: (group) => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      queryClient.invalidateQueries({ queryKey: ["groups", groupId] });
      toast({
        title: "Group updated!",
        description: `${group.name} has been updated.`,
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to update group",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    updateGroup: mutation.mutate,
    isUpdating: mutation.isPending,
    error: mutation.error,
  };
}

export function useDeleteGroup() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: (groupId: string) => groupsApi.delete(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
      toast({
        title: "Group deleted",
        description: "The group has been deleted.",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to delete group",
        description: error.response?.data?.message || "Something went wrong",
      });
    },
  });

  return {
    deleteGroup: mutation.mutate,
    isDeleting: mutation.isPending,
    error: mutation.error,
  };
}
