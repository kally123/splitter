-- V2__add_multi_currency_support.sql
-- Add multi-currency support to expenses

-- Add columns for multi-currency support
ALTER TABLE expenses 
ADD COLUMN IF NOT EXISTS original_amount DECIMAL(15, 4),
ADD COLUMN IF NOT EXISTS original_currency VARCHAR(3),
ADD COLUMN IF NOT EXISTS converted_amount DECIMAL(15, 4),
ADD COLUMN IF NOT EXISTS group_currency VARCHAR(3) DEFAULT 'USD',
ADD COLUMN IF NOT EXISTS exchange_rate DECIMAL(19, 6),
ADD COLUMN IF NOT EXISTS exchange_rate_date DATE;

-- Update existing records to use current amount as original
UPDATE expenses SET 
    original_amount = amount,
    original_currency = currency,
    converted_amount = amount,
    group_currency = currency,
    exchange_rate = 1.0
WHERE original_amount IS NULL;

-- Add index for currency queries
CREATE INDEX IF NOT EXISTS idx_expenses_currency ON expenses(currency);
CREATE INDEX IF NOT EXISTS idx_expenses_original_currency ON expenses(original_currency);
