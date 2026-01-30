-- Payment Service Schema
-- V1: Initial schema

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    settlement_id UUID,
    group_id UUID,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(20) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_fee DECIMAL(19, 4),
    idempotency_key VARCHAR(255) UNIQUE,
    metadata JSONB,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PROCESSING', 'REQUIRES_ACTION', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_payment_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'VENMO', 'MANUAL'))
);

-- Payment methods table
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_customer_id VARCHAR(255),
    provider_payment_method_id VARCHAR(255),
    type VARCHAR(50),
    last_four VARCHAR(4),
    brand VARCHAR(50),
    exp_month INTEGER,
    exp_year INTEGER,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT chk_pm_provider CHECK (provider IN ('STRIPE', 'PAYPAL', 'VENMO', 'MANUAL'))
);

-- Webhooks table for idempotent processing
CREATE TABLE IF NOT EXISTS webhooks (
    id UUID PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    process_error TEXT,
    received_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for payments
CREATE INDEX idx_payments_from_user ON payments(from_user_id);
CREATE INDEX idx_payments_to_user ON payments(to_user_id);
CREATE INDEX idx_payments_settlement ON payments(settlement_id);
CREATE INDEX idx_payments_group ON payments(group_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider_tx ON payments(provider_transaction_id);
CREATE INDEX idx_payments_created ON payments(created_at DESC);

-- Indexes for payment methods
CREATE INDEX idx_pm_user ON payment_methods(user_id);
CREATE INDEX idx_pm_user_active ON payment_methods(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_pm_provider_id ON payment_methods(provider_payment_method_id);

-- Indexes for webhooks
CREATE INDEX idx_webhooks_event ON webhooks(event_id);
CREATE INDEX idx_webhooks_unprocessed ON webhooks(received_at) WHERE processed = FALSE;

-- Comments
COMMENT ON TABLE payments IS 'Payment transactions between users';
COMMENT ON TABLE payment_methods IS 'Stored payment methods for users';
COMMENT ON TABLE webhooks IS 'Received payment provider webhooks for idempotent processing';
