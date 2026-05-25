-- Migration script to add settlement_id column to cash_payment_otps table
-- This enables OTP functionality for settlement payments

-- Add settlement_id column to cash_payment_otps table
ALTER TABLE cash_payment_otps 
ADD COLUMN settlement_id VARCHAR(255);

-- Add index for better query performance
CREATE INDEX idx_cash_payment_otps_settlement_id ON cash_payment_otps(settlement_id);

-- Add index for settlement OTP queries
CREATE INDEX idx_cash_payment_otps_settlement_used_expiry ON cash_payment_otps(settlement_id, used, expiry_time);

-- Update any existing data if needed (optional)
-- UPDATE cash_payment_otps SET settlement_id = NULL WHERE settlement_id IS NULL;

-- Verify the changes
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'cash_payment_otps' 
ORDER BY ordinal_position;