-- Database migration for Other Charges functionality
-- Run this script to add the required tables for Other Charges feature

-- Create other_charges table
CREATE TABLE IF NOT EXISTS other_charges (
    charge_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    charge_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    category VARCHAR(50) NOT NULL CHECK (category IN ('RENT', 'RECURRING_CHARGE', 'ONE_TIME_CHARGE', 'REFUNDABLE_DEPOSIT', 'DEDUCTION_CHARGE', 'OTHER_CHARGE_TENANT', 'OTHER_CHARGE_ROOM')),
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PARTIALLY_PAID', 'COMPLETED', 'OVERDUE', 'CANCELLED')),
    
    -- Relationships
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    tenant_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    hostel_id UUID NOT NULL REFERENCES hostels(hostel_id) ON DELETE CASCADE,
    
    -- Payment details
    due_date TIMESTAMP,
    paid_date TIMESTAMP,
    paid_amount DECIMAL(10,2) DEFAULT 0 CHECK (paid_amount >= 0),
    
    -- Installment details
    installment_enabled BOOLEAN DEFAULT FALSE,
    installment_count INTEGER CHECK (installment_count IS NULL OR installment_count BETWEEN 2 AND 12),
    installment_amount DECIMAL(10,2) CHECK (installment_amount IS NULL OR installment_amount > 0),
    
    -- Status
    active BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create other_charge_installments table
CREATE TABLE IF NOT EXISTS other_charge_installments (
    installment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    charge_id UUID NOT NULL REFERENCES other_charges(charge_id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    installment_number INTEGER NOT NULL CHECK (installment_number > 0),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    due_date TIMESTAMP NOT NULL,
    
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PARTIALLY_PAID', 'COMPLETED', 'OVERDUE', 'CANCELLED')),
    paid_date TIMESTAMP,
    paid_amount DECIMAL(10,2) DEFAULT 0 CHECK (paid_amount >= 0),
    
    transaction_id VARCHAR(255),
    payment_method VARCHAR(50),
    notes VARCHAR(500),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE(charge_id, installment_number),
    UNIQUE(charge_id, tenant_id, installment_number)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_other_charges_owner_id ON other_charges(owner_id);
CREATE INDEX IF NOT EXISTS idx_other_charges_tenant_id ON other_charges(tenant_id);
CREATE INDEX IF NOT EXISTS idx_other_charges_room_id ON other_charges(room_id);
CREATE INDEX IF NOT EXISTS idx_other_charges_hostel_id ON other_charges(hostel_id);
CREATE INDEX IF NOT EXISTS idx_other_charges_category ON other_charges(category);
CREATE INDEX IF NOT EXISTS idx_other_charges_payment_status ON other_charges(payment_status);
CREATE INDEX IF NOT EXISTS idx_other_charges_due_date ON other_charges(due_date);
CREATE INDEX IF NOT EXISTS idx_other_charges_active ON other_charges(active);
CREATE INDEX IF NOT EXISTS idx_other_charges_created_at ON other_charges(created_at);

CREATE INDEX IF NOT EXISTS idx_other_charge_installments_charge_id ON other_charge_installments(charge_id);
CREATE INDEX IF NOT EXISTS idx_other_charge_installments_tenant_id ON other_charge_installments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_other_charge_installments_payment_status ON other_charge_installments(payment_status);
CREATE INDEX IF NOT EXISTS idx_other_charge_installments_due_date ON other_charge_installments(due_date);

-- Add constraints to ensure data integrity
ALTER TABLE other_charges ADD CONSTRAINT check_tenant_or_room_required 
    CHECK (
        (category = 'OTHER_CHARGE_TENANT' AND tenant_id IS NOT NULL AND room_id IS NULL) OR
        (category = 'OTHER_CHARGE_ROOM' AND room_id IS NOT NULL AND tenant_id IS NULL) OR
        (category NOT IN ('OTHER_CHARGE_TENANT', 'OTHER_CHARGE_ROOM'))
    );

ALTER TABLE other_charges ADD CONSTRAINT check_installment_consistency 
    CHECK (
        (installment_enabled = FALSE AND installment_count IS NULL AND installment_amount IS NULL) OR
        (installment_enabled = TRUE AND installment_count IS NOT NULL AND installment_amount IS NOT NULL)
    );

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_other_charges_updated_at 
    BEFORE UPDATE ON other_charges 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_other_charge_installments_updated_at 
    BEFORE UPDATE ON other_charge_installments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data (optional - remove in production)
-- This is just for testing purposes
/*
INSERT INTO other_charges (
    charge_name, description, amount, category, owner_id, tenant_id, hostel_id, due_date
) VALUES (
    'Electricity Bill - January', 
    'Monthly electricity charges for January 2024', 
    1500.00, 
    'OTHER_CHARGE_TENANT', 
    (SELECT user_id FROM users WHERE role = 'OWNER' LIMIT 1),
    (SELECT user_id FROM users WHERE role = 'TENANT' LIMIT 1),
    (SELECT hostel_id FROM hostels LIMIT 1),
    CURRENT_TIMESTAMP + INTERVAL '7 days'
);
*/

-- Verify tables were created successfully
SELECT 
    table_name, 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('other_charges', 'other_charge_installments')
ORDER BY table_name, ordinal_position;

COMMENT ON TABLE other_charges IS 'Stores additional charges that owners can apply to tenants or rooms';
COMMENT ON TABLE other_charge_installments IS 'Stores installment details for other charges that support installment payments';

COMMENT ON COLUMN other_charges.category IS 'Type of charge: OTHER_CHARGE_TENANT for tenant-specific, OTHER_CHARGE_ROOM for room-based charges';
COMMENT ON COLUMN other_charges.tenant_id IS 'For tenant-specific charges, references the specific tenant';
COMMENT ON COLUMN other_charges.room_id IS 'For room-based charges, references the room (amount split among current tenants)';
COMMENT ON COLUMN other_charges.installment_enabled IS 'Whether this charge can be paid in installments';
COMMENT ON COLUMN other_charge_installments.tenant_id IS 'The specific tenant responsible for this installment (even for room-based charges)';