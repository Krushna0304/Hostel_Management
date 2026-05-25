-- Database migration for Agreement Settlement Module
-- Run this script to add the required tables for Settlement functionality

-- Create settlement_requests table
CREATE TABLE IF NOT EXISTS settlement_requests (
    settlement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE SET NULL,
    
    -- Settlement Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_OWNER_REVIEW' 
        CHECK (status IN ('PENDING_OWNER_REVIEW', 'CALCULATION_IN_PROGRESS', 'PENDING_TENANT_PAYMENT', 
                         'PENDING_OWNER_PAYMENT', 'PAYMENT_IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    
    -- Financial Details
    security_deposit DECIMAL(10,2),
    outstanding_rent DECIMAL(10,2) DEFAULT 0 CHECK (outstanding_rent >= 0),
    outstanding_charges DECIMAL(10,2) DEFAULT 0 CHECK (outstanding_charges >= 0),
    damage_charges DECIMAL(10,2) DEFAULT 0 CHECK (damage_charges >= 0),
    cleaning_charges DECIMAL(10,2) DEFAULT 0 CHECK (cleaning_charges >= 0),
    other_deductions DECIMAL(10,2) DEFAULT 0 CHECK (other_deductions >= 0),
    total_deductions DECIMAL(10,2) DEFAULT 0 CHECK (total_deductions >= 0),
    final_settlement_amount DECIMAL(10,2),
    
    -- Settlement Type
    settlement_type VARCHAR(20) CHECK (settlement_type IN ('TENANT_PAYABLE', 'OWNER_PAYABLE')),
    
    -- Notes and Comments
    tenant_notes VARCHAR(500),
    owner_notes VARCHAR(500),
    damage_description VARCHAR(1000),
    
    -- Payment Reference
    payment_reference VARCHAR(255),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT unique_active_settlement_per_agreement 
        UNIQUE (agreement_id) 
        DEFERRABLE INITIALLY DEFERRED
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_settlement_owner_status ON settlement_requests(owner_id, status);
CREATE INDEX IF NOT EXISTS idx_settlement_tenant_status ON settlement_requests(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_settlement_agreement ON settlement_requests(agreement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_created_at ON settlement_requests(created_at DESC);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_settlement_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER settlement_updated_at_trigger
    BEFORE UPDATE ON settlement_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_settlement_updated_at();

-- Add settlement-related comments for documentation
COMMENT ON TABLE settlement_requests IS 'Stores agreement settlement requests and their processing status';
COMMENT ON COLUMN settlement_requests.settlement_type IS 'Indicates who needs to pay: TENANT_PAYABLE (tenant owes money) or OWNER_PAYABLE (owner refunds money)';
COMMENT ON COLUMN settlement_requests.final_settlement_amount IS 'Absolute amount to be paid, regardless of direction';
COMMENT ON COLUMN settlement_requests.status IS 'Current status of the settlement process';

-- Insert sample data for testing (optional - remove in production)
-- This is commented out by default
/*
INSERT INTO settlement_requests (
    agreement_id, tenant_id, owner_id, security_deposit, 
    tenant_notes, status
) VALUES (
    'sample-agreement-id', 
    (SELECT user_id FROM users WHERE role = 'TENANT' LIMIT 1),
    (SELECT user_id FROM users WHERE role = 'OWNER' LIMIT 1),
    5000.00,
    'Requesting settlement due to job relocation',
    'PENDING_OWNER_REVIEW'
);
*/