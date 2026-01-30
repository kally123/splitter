import { apiClient } from "./client";
import type {
  Group,
  GroupWithMembers,
  CreateGroupRequest,
  UpdateGroupRequest,
  GroupMember,
  GroupInvitation,
} from "@/types/group";

export const groupsApi = {
  getMyGroups: async (): Promise<Group[]> => {
    const response = await apiClient.get<Group[]>("/api/v1/groups");
    return response.data;
  },

  getById: async (id: string): Promise<GroupWithMembers> => {
    const response = await apiClient.get<GroupWithMembers>(`/api/v1/groups/${id}`);
    return response.data;
  },

  create: async (data: CreateGroupRequest): Promise<Group> => {
    const response = await apiClient.post<Group>("/api/v1/groups", data);
    return response.data;
  },

  update: async (id: string, data: UpdateGroupRequest): Promise<Group> => {
    const response = await apiClient.put<Group>(`/api/v1/groups/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/groups/${id}`);
  },

  getMembers: async (groupId: string): Promise<GroupMember[]> => {
    const response = await apiClient.get<GroupMember[]>(
      `/api/v1/groups/${groupId}/members`
    );
    return response.data;
  },

  addMember: async (groupId: string, userId: string): Promise<GroupMember> => {
    const response = await apiClient.post<GroupMember>(
      `/api/v1/groups/${groupId}/members`,
      { userId }
    );
    return response.data;
  },

  removeMember: async (groupId: string, userId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/groups/${groupId}/members/${userId}`);
  },

  updateMemberRole: async (
    groupId: string,
    userId: string,
    role: string
  ): Promise<GroupMember> => {
    const response = await apiClient.patch<GroupMember>(
      `/api/v1/groups/${groupId}/members/${userId}`,
      { role }
    );
    return response.data;
  },

  // Invitations
  createInvitation: async (groupId: string, email: string): Promise<GroupInvitation> => {
    const response = await apiClient.post<GroupInvitation>("/api/v1/invitations", {
      groupId,
      email,
    });
    return response.data;
  },

  acceptInvitation: async (token: string): Promise<GroupMember> => {
    const response = await apiClient.post<GroupMember>(
      `/api/v1/invitations/${token}/accept`
    );
    return response.data;
  },

  declineInvitation: async (token: string): Promise<void> => {
    await apiClient.post(`/api/v1/invitations/${token}/decline`);
  },

  getMyInvitations: async (): Promise<GroupInvitation[]> => {
    const response = await apiClient.get<GroupInvitation[]>("/api/v1/invitations/pending");
    return response.data;
  },
};
