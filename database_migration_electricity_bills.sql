-- Migration script for Electricity Bill Management System
-- This creates tables for electricity accounts, bills, and payments

-- Create electricity_accounts table
CREATE TABLE electricity_accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL,
    account_number VARCHAR(100) NOT NULL UNIQUE,
    owner_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_electricity_accounts_room FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    CONSTRAINT fk_electricity_accounts_owner FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

-- Create electricity_bills table
CREATE TABLE electricity_bills (
    bill_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    room_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    tenant_id UUID,
    bill_month INTEGER NOT NULL CHECK (bill_month >= 1 AND bill_month <= 12),
    bill_year INTEGER NOT NULL CHECK (bill_year >= 2020),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    remaining_amount DECIMAL(10,2) NOT NULL CHECK (remaining_amount >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE')),
    due_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_electricity_bills_account FOREIGN KEY (account_id) REFERENCES electricity_accounts(account_id),
    CONSTRAINT fk_electricity_bills_room FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    CONSTRAINT fk_electricity_bills_owner FOREIGN KEY (owner_id) REFERENCES users(user_id),
    CONSTRAINT fk_electricity_bills_tenant FOREIGN KEY (tenant_id) REFERENCES users(user_id),
    CONSTRAINT uk_electricity_bills_account_period UNIQUE (account_id, bill_month, bill_year)
);

-- Create electricity_payments table
CREATE TABLE electricity_payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_mode VARCHAR(20) NOT NULL CHECK (payment_mode IN ('ONLINE', 'CASH')),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    payment_reference VARCHAR(255),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_electricity_payments_bill FOREIGN KEY (bill_id) REFERENCES electricity_bills(bill_id),
    CONSTRAINT fk_electricity_payments_tenant FOREIGN KEY (tenant_id) REFERENCES users(user_id)
);

-- Create indexes for better performance
CREATE INDEX idx_electricity_accounts_owner ON electricity_accounts(owner_id);
CREATE INDEX idx_electricity_accounts_room ON electricity_accounts(room_id);
CREATE INDEX idx_electricity_accounts_number ON electricity_accounts(account_number);

CREATE INDEX idx_electricity_bills_owner ON electricity_bills(owner_id);
CREATE INDEX idx_electricity_bills_tenant ON electricity_bills(tenant_id);
CREATE INDEX idx_electricity_bills_account ON electricity_bills(account_id);
CREATE INDEX idx_electricity_bills_period ON electricity_bills(bill_year, bill_month);
CREATE INDEX idx_electricity_bills_status ON electricity_bills(status);

CREATE INDEX idx_electricity_payments_bill ON electricity_payments(bill_id);
CREATE INDEX idx_electricity_payments_tenant ON electricity_payments(tenant_id);
CREATE INDEX idx_electricity_payments_created ON electricity_payments(created_at);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_electricity_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_electricity_accounts_updated_at
    BEFORE UPDATE ON electricity_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_electricity_updated_at();

CREATE TRIGGER trigger_electricity_bills_updated_at
    BEFORE UPDATE ON electricity_bills
    FOR EACH ROW
    EXECUTE FUNCTION update_electricity_updated_at();

-- Insert sample data (optional)
-- This can be uncommented to add test data

/*
-- Sample electricity accounts
INSERT INTO electricity_accounts (room_id, account_number, owner_id) 
SELECT r.room_id, 'ELE' || LPAD(ROW_NUMBER() OVER (ORDER BY r.room_id)::text, 6, '0'), h.owner_id
FROM rooms r 
JOIN hostels h ON r.hostel_id = h.hostel_id 
LIMIT 10;

-- Sample electricity bills
INSERT INTO electricity_bills (account_id, room_id, owner_id, tenant_id, bill_month, bill_year, total_amount, remaining_amount)
SELECT 
    ea.account_id,
    ea.room_id,
    ea.owner_id,
    a.user_id as tenant_id,
    EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER,
    EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER,
    (RANDOM() * 2000 + 500)::DECIMAL(10,2),
    (RANDOM() * 2000 + 500)::DECIMAL(10,2)
FROM electricity_accounts ea
JOIN agreements a ON ea.room_id = a.room_id AND a.status = 'ACTIVE'
LIMIT 5;
*/

-- Verify the migration
SELECT 'Electricity accounts table created' as status, COUNT(*) as count FROM electricity_accounts
UNION ALL
SELECT 'Electricity bills table created' as status, COUNT(*) as count FROM electricity_bills
UNION ALL
SELECT 'Electricity payments table created' as status, COUNT(*) as count FROM electricity_payments;