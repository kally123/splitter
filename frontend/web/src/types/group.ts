export type GroupType = 
  | "HOME"
  | "TRIP"
  | "COUPLE"
  | "FRIENDS"
  | "FAMILY"
  | "WORK"
  | "OTHER";

export type MemberRole = "OWNER" | "ADMIN" | "MEMBER";

export interface Group {
  id: string;
  name: string;
  description?: string;
  groupType: GroupType;
  coverImageUrl?: string;
  simplifyDebts: boolean;
  defaultCurrency: string;
  createdBy: string;
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GroupMember {
  id: string;
  groupId: string;
  userId: string;
  displayName: string;
  email: string;
  avatarUrl?: string;
  role: MemberRole;
  joinedAt: string;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  groupType: GroupType;
  defaultCurrency?: string;
  simplifyDebts?: boolean;
}

export interface UpdateGroupRequest {
  name?: string;
  description?: string;
  groupType?: GroupType;
  defaultCurrency?: string;
  simplifyDebts?: boolean;
}

export interface GroupWithMembers extends Group {
  members: GroupMember[];
}

export interface GroupInvitation {
  id: string;
  groupId: string;
  groupName: string;
  invitedByName: string;
  email: string;
  token: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "EXPIRED" | "CANCELLED";
  expiresAt: string;
  createdAt: string;
}
