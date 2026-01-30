-- =============================================
-- Splitter Database Initialization Script
-- Creates separate schemas for each microservice
-- =============================================

-- Create databases for each service
CREATE DATABASE splitter_users;
CREATE DATABASE splitter_groups;
CREATE DATABASE splitter_expenses;
CREATE DATABASE splitter_balances;
CREATE DATABASE splitter_settlements;
CREATE DATABASE splitter_notifications;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE splitter_users TO splitter;
GRANT ALL PRIVILEGES ON DATABASE splitter_groups TO splitter;
GRANT ALL PRIVILEGES ON DATABASE splitter_expenses TO splitter;
GRANT ALL PRIVILEGES ON DATABASE splitter_balances TO splitter;
GRANT ALL PRIVILEGES ON DATABASE splitter_settlements TO splitter;
GRANT ALL PRIVILEGES ON DATABASE splitter_notifications TO splitter;

-- =============================================
-- User Service Schema
-- =============================================
\c splitter_users;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    default_currency CHAR(3) DEFAULT 'USD',
    locale VARCHAR(10) DEFAULT 'en-US',
    timezone VARCHAR(50) DEFAULT 'UTC',
    email_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'blocked')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_friendships_user ON friendships(user_id);
CREATE INDEX idx_friendships_friend ON friendships(friend_id);
CREATE INDEX idx_friendships_status ON friendships(status);

-- =============================================
-- Group Service Schema
-- =============================================
\c splitter_groups;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    group_type VARCHAR(20) DEFAULT 'other' CHECK (group_type IN ('trip', 'household', 'couple', 'event', 'project', 'other')),
    cover_image_url VARCHAR(500),
    simplify_debts BOOLEAN DEFAULT true,
    default_currency CHAR(3) DEFAULT 'USD',
    created_by UUID NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(20) DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    nickname VARCHAR(50),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_groups_created_by ON groups(created_by);
CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- =============================================
-- Expense Service Schema
-- =============================================
\c splitter_expenses;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7),
    parent_id UUID REFERENCES categories(id),
    sort_order INTEGER DEFAULT 0,
    is_system BOOLEAN DEFAULT false
);

-- Insert default categories
INSERT INTO categories (name, icon, color, is_system, sort_order) VALUES
('General', 'receipt', '#6B7280', true, 0),
('Food & Drink', 'utensils', '#EF4444', true, 1),
('Groceries', 'shopping-cart', '#F97316', true, 2),
('Transportation', 'car', '#EAB308', true, 3),
('Entertainment', 'film', '#22C55E', true, 4),
('Utilities', 'zap', '#14B8A6', true, 5),
('Rent', 'home', '#3B82F6', true, 6),
('Travel', 'plane', '#8B5CF6', true, 7),
('Shopping', 'shopping-bag', '#EC4899', true, 8),
('Healthcare', 'heart-pulse', '#F43F5E', true, 9),
('Education', 'graduation-cap', '#6366F1', true, 10),
('Sports', 'dumbbell', '#10B981', true, 11),
('Gifts', 'gift', '#F59E0B', true, 12),
('Other', 'more-horizontal', '#9CA3AF', true, 99);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) DEFAULT 'USD',
    category_id UUID REFERENCES categories(id),
    paid_by UUID NOT NULL,
    split_type VARCHAR(20) DEFAULT 'EQUAL' CHECK (split_type IN ('EQUAL', 'PERCENTAGE', 'SHARES', 'EXACT')),
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    receipt_url VARCHAR(500),
    notes TEXT,
    is_recurring BOOLEAN DEFAULT false,
    recurring_interval VARCHAR(20) CHECK (recurring_interval IN ('daily', 'weekly', 'monthly', 'yearly')),
    next_occurrence DATE,
    is_deleted BOOLEAN DEFAULT false,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE expense_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    share_amount DECIMAL(15, 2) NOT NULL,
    share_percentage DECIMAL(5, 2),
    share_units INTEGER,
    is_payer BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_expenses_group ON expenses(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by);
CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_expenses_created_by ON expenses(created_by);
CREATE INDEX idx_expense_shares_expense ON expense_shares(expense_id);
CREATE INDEX idx_expense_shares_user ON expense_shares(user_id);

-- =============================================
-- Balance Service Schema
-- =============================================
\c splitter_balances;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    group_id UUID,
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    currency CHAR(3) DEFAULT 'USD',
    last_calculated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(from_user_id, to_user_id, group_id, currency)
);

CREATE TABLE balance_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    total_owed DECIMAL(15, 2) DEFAULT 0,
    total_owing DECIMAL(15, 2) DEFAULT 0,
    net_balance DECIMAL(15, 2) DEFAULT 0,
    currency CHAR(3) DEFAULT 'USD',
    snapshot_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_balances_from ON balances(from_user_id);
CREATE INDEX idx_balances_to ON balances(to_user_id);
CREATE INDEX idx_balances_group ON balances(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_balance_snapshots_user ON balance_snapshots(user_id);

-- =============================================
-- Settlement Service Schema
-- =============================================
\c splitter_settlements;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    group_id UUID,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50) CHECK (payment_method IN ('cash', 'bank_transfer', 'paypal', 'venmo', 'zelle', 'other')),
    external_reference VARCHAR(255),
    notes TEXT,
    settled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_settlements_from ON settlements(from_user_id);
CREATE INDEX idx_settlements_to ON settlements(to_user_id);
CREATE INDEX idx_settlements_group ON settlements(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_settlements_date ON settlements(settled_at);

-- =============================================
-- Notification Service Schema
-- =============================================
\c splitter_notifications;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    data JSONB,
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    email_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT true,
    expense_added BOOLEAN DEFAULT true,
    payment_received BOOLEAN DEFAULT true,
    friend_request BOOLEAN DEFAULT true,
    group_invitation BOOLEAN DEFAULT true,
    weekly_summary BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at);
