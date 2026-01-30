-- Performance Optimization Indexes for Splitter
-- Run after V1 migrations on each service database

-- =============================================
-- Expense Service Optimizations
-- =============================================
\c splitter_expenses;

-- Composite index for group expense queries (most common query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expenses_group_created 
ON expenses(group_id, created_at DESC);

-- Index for user's expenses (payer)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expenses_payer_created 
ON expenses(paid_by, created_at DESC);

-- Index for expense shares by user
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expense_shares_user 
ON expense_shares(user_id);

-- Composite index for efficient expense share lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expense_shares_user_expense 
ON expense_shares(user_id, expense_id);

-- Index for category filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expenses_category 
ON expenses(category);

-- Index for date range queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expenses_date 
ON expenses(expense_date DESC);

-- Partial index for active (non-deleted) expenses
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_expenses_active 
ON expenses(group_id, created_at DESC) 
WHERE deleted_at IS NULL;

-- =============================================
-- Balance Service Optimizations
-- =============================================
\c splitter_balances;

-- Materialized view for user balances (refreshed on expense events)
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_user_balances AS
SELECT 
    es.user_id,
    e.group_id,
    SUM(CASE 
        WHEN e.paid_by = es.user_id THEN e.amount - es.share_amount 
        ELSE -es.share_amount 
    END) as net_balance,
    COUNT(DISTINCT e.id) as expense_count,
    MAX(e.created_at) as last_expense_at
FROM expense_shares es
JOIN expenses e ON es.expense_id = e.id
WHERE e.deleted_at IS NULL
GROUP BY es.user_id, e.group_id;

-- Unique index for concurrent refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_user_balances_pk 
ON mv_user_balances(user_id, group_id);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_user_balances()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_user_balances;
END;
$$ LANGUAGE plpgsql;

-- Pairwise balance view for simplified debt calculation
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_pairwise_balances AS
SELECT 
    es.user_id as debtor_id,
    e.paid_by as creditor_id,
    e.group_id,
    SUM(es.share_amount) as amount_owed
FROM expense_shares es
JOIN expenses e ON es.expense_id = e.id
WHERE e.deleted_at IS NULL
  AND es.user_id != e.paid_by
GROUP BY es.user_id, e.paid_by, e.group_id
HAVING SUM(es.share_amount) > 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_pairwise_balances_pk 
ON mv_pairwise_balances(debtor_id, creditor_id, group_id);

-- =============================================
-- Group Service Optimizations
-- =============================================
\c splitter_groups;

-- Index for user's group memberships
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_members_user 
ON group_members(user_id);

-- Composite index for membership checks
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_members_user_group 
ON group_members(user_id, group_id);

-- Index for group member listing
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_group_members_group 
ON group_members(group_id);

-- =============================================
-- Settlement Service Optimizations
-- =============================================
\c splitter_settlements;

-- Index for user's settlements
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_settlements_from_user 
ON settlements(from_user_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_settlements_to_user 
ON settlements(to_user_id, created_at DESC);

-- Index for group settlements
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_settlements_group 
ON settlements(group_id, created_at DESC);

-- =============================================
-- User Service Optimizations
-- =============================================
\c splitter_users;

-- Index for friend lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_friendships_user_status 
ON friendships(user_id, status);

-- Index for email search (case-insensitive)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_lower 
ON users(LOWER(email));

-- Index for user search by name
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_display_name_trgm 
ON users USING gin(display_name gin_trgm_ops);

-- =============================================
-- Analytics Optimizations
-- =============================================
\c splitter_expenses;

-- Aggregation view for spending analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_spending_by_category AS
SELECT 
    es.user_id,
    e.category,
    DATE_TRUNC('month', e.expense_date) as month,
    SUM(es.share_amount) as total_spent,
    COUNT(*) as expense_count
FROM expense_shares es
JOIN expenses e ON es.expense_id = e.id
WHERE e.deleted_at IS NULL
GROUP BY es.user_id, e.category, DATE_TRUNC('month', e.expense_date);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_spending_by_category_pk 
ON mv_spending_by_category(user_id, category, month);

-- Daily spending trend
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_daily_spending AS
SELECT 
    es.user_id,
    e.group_id,
    DATE(e.expense_date) as date,
    SUM(es.share_amount) as total_spent,
    COUNT(*) as expense_count
FROM expense_shares es
JOIN expenses e ON es.expense_id = e.id
WHERE e.deleted_at IS NULL
  AND e.expense_date >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY es.user_id, e.group_id, DATE(e.expense_date);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_daily_spending_pk 
ON mv_daily_spending(user_id, group_id, date);

-- =============================================
-- Query Optimization Helpers
-- =============================================

-- Function to get user's total balance efficiently
CREATE OR REPLACE FUNCTION get_user_total_balance(p_user_id UUID)
RETURNS TABLE(total_owed NUMERIC, total_owed_to_you NUMERIC) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN net_balance < 0 THEN ABS(net_balance) ELSE 0 END), 0) as total_owed,
        COALESCE(SUM(CASE WHEN net_balance > 0 THEN net_balance ELSE 0 END), 0) as total_owed_to_you
    FROM mv_user_balances
    WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to get simplified debts for a group
CREATE OR REPLACE FUNCTION get_simplified_debts(p_group_id UUID)
RETURNS TABLE(
    from_user_id UUID, 
    to_user_id UUID, 
    amount NUMERIC
) AS $$
DECLARE
    v_creditors RECORD;
    v_debtors RECORD;
    v_settle_amount NUMERIC;
BEGIN
    -- Create temp tables for processing
    CREATE TEMP TABLE tmp_net_balances AS
    SELECT user_id, net_balance
    FROM mv_user_balances
    WHERE group_id = p_group_id
      AND net_balance != 0;

    CREATE TEMP TABLE tmp_results (
        from_user_id UUID,
        to_user_id UUID,
        amount NUMERIC
    );

    -- Greedy matching algorithm
    WHILE EXISTS (SELECT 1 FROM tmp_net_balances WHERE net_balance > 0.01) 
      AND EXISTS (SELECT 1 FROM tmp_net_balances WHERE net_balance < -0.01)
    LOOP
        -- Get max creditor
        SELECT user_id, net_balance INTO v_creditors
        FROM tmp_net_balances
        WHERE net_balance > 0
        ORDER BY net_balance DESC
        LIMIT 1;

        -- Get max debtor
        SELECT user_id, ABS(net_balance) as net_balance INTO v_debtors
        FROM tmp_net_balances
        WHERE net_balance < 0
        ORDER BY net_balance ASC
        LIMIT 1;

        -- Calculate settlement amount
        v_settle_amount := LEAST(v_creditors.net_balance, v_debtors.net_balance);

        -- Record the settlement
        INSERT INTO tmp_results VALUES (v_debtors.user_id, v_creditors.user_id, v_settle_amount);

        -- Update balances
        UPDATE tmp_net_balances 
        SET net_balance = net_balance - v_settle_amount 
        WHERE user_id = v_creditors.user_id;

        UPDATE tmp_net_balances 
        SET net_balance = net_balance + v_settle_amount 
        WHERE user_id = v_debtors.user_id;

        -- Remove zero balances
        DELETE FROM tmp_net_balances WHERE ABS(net_balance) < 0.01;
    END LOOP;

    -- Return results
    RETURN QUERY SELECT * FROM tmp_results;

    -- Cleanup
    DROP TABLE tmp_net_balances;
    DROP TABLE tmp_results;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- Index Statistics & Maintenance
-- =============================================

-- Enable pg_trgm extension for fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Analyze tables after index creation
ANALYZE expenses;
ANALYZE expense_shares;
ANALYZE settlements;
ANALYZE groups;
ANALYZE group_members;
ANALYZE users;
ANALYZE friendships;
