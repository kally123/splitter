export interface Balance {
  userId: string;
  displayName: string;
  avatarUrl?: string;
  amount: number;
  currency: string;
}

export interface GroupBalance {
  groupId: string;
  groupName: string;
  balances: Balance[];
  totalOwed: number;
  totalOwing: number;
}

export interface UserBalance {
  userId: string;
  displayName: string;
  avatarUrl?: string;
  netBalance: number;
  currency: string;
  groupBalances: {
    groupId: string;
    groupName: string;
    amount: number;
  }[];
}

export interface SimplifiedDebt {
  fromUserId: string;
  fromUserName: string;
  toUserId: string;
  toUserName: string;
  amount: number;
  currency: string;
}

export interface BalanceSummary {
  youOwe: number;
  youAreOwed: number;
  netBalance: number;
  currency: string;
  debts: SimplifiedDebt[];
}
