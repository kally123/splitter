-- Receipt Service Schema
-- V1: Initial schema

-- Receipts table
CREATE TABLE IF NOT EXISTS receipts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    expense_id UUID,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    raw_ocr_text TEXT,
    parsed_data JSONB,
    error_message TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_status CHECK (status IN ('UPLOADED', 'PROCESSING', 'PARSED', 'FAILED'))
);

-- Indexes for common queries
CREATE INDEX idx_receipts_user_id ON receipts(user_id);
CREATE INDEX idx_receipts_expense_id ON receipts(expense_id);
CREATE INDEX idx_receipts_status ON receipts(status);
CREATE INDEX idx_receipts_uploaded_at ON receipts(uploaded_at DESC);

-- Index for finding pending receipts to process
CREATE INDEX idx_receipts_pending ON receipts(uploaded_at) WHERE status = 'UPLOADED';

-- Comments
COMMENT ON TABLE receipts IS 'Stores receipt images and OCR processing results';
COMMENT ON COLUMN receipts.storage_path IS 'Path to the file in S3 or local storage';
COMMENT ON COLUMN receipts.raw_ocr_text IS 'Raw text extracted from OCR';
COMMENT ON COLUMN receipts.parsed_data IS 'Structured JSON data parsed from OCR text';
