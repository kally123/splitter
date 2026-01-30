-- V3__add_recurring_expenses.sql
-- Recurring expenses table

CREATE TABLE IF NOT EXISTS recurring_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    created_by UUID NOT NULL,
    description VARCHAR(200) NOT NULL,
    amount DECIMAL(15, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    category VARCHAR(30),
    split_type VARCHAR(20) NOT NULL DEFAULT 'EQUAL',
    splits JSONB,
    
    -- Recurrence settings
    frequency VARCHAR(20) NOT NULL,  -- DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
    interval_value INTEGER NOT NULL DEFAULT 1,
    day_of_week INTEGER,  -- 1-7 for weekly (Monday = 1)
    day_of_month INTEGER, -- 1-31 for monthly
    month_of_year INTEGER, -- 1-12 for yearly
    
    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE,
    next_occurrence DATE NOT NULL,
    last_generated DATE,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    is_deleted BOOLEAN DEFAULT false,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Track which expenses were generated from recurring templates
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS recurring_expense_id UUID REFERENCES recurring_expenses(id);

-- Indexes for recurring expenses
CREATE INDEX IF NOT EXISTS idx_recurring_next_occurrence 
    ON recurring_expenses(next_occurrence) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_recurring_group 
    ON recurring_expenses(group_id);
CREATE INDEX IF NOT EXISTS idx_recurring_created_by 
    ON recurring_expenses(created_by);
CREATE INDEX IF NOT EXISTS idx_recurring_active 
    ON recurring_expenses(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_expenses_recurring 
    ON expenses(recurring_expense_id) WHERE recurring_expense_id IS NOT NULL;
