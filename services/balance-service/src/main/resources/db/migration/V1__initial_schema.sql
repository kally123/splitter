-- V1__initial_schema.sql
-- Balance Service Database Schema

-- Balances table (net balance between two users in a group)
CREATE TABLE IF NOT EXISTS balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_id, from_user_id, to_user_id)
);

-- Balance transactions table (audit trail)
CREATE TABLE IF NOT EXISTS balance_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    transaction_type VARCHAR(20) NOT NULL,
    reference_id UUID,
    description VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_balances_group ON balances(group_id);
CREATE INDEX IF NOT EXISTS idx_balances_from_user ON balances(from_user_id);
CREATE INDEX IF NOT EXISTS idx_balances_to_user ON balances(to_user_id);
CREATE INDEX IF NOT EXISTS idx_balances_non_zero ON balances(group_id) WHERE amount != 0;

CREATE INDEX IF NOT EXISTS idx_transactions_group ON balance_transactions(group_id);
CREATE INDEX IF NOT EXISTS idx_transactions_from_user ON balance_transactions(from_user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_to_user ON balance_transactions(to_user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON balance_transactions(reference_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created ON balance_transactions(created_at);
