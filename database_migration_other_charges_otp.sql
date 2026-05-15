-- Migration to add chargeId support to cash_payment_otps table for Other Charges OTP functionality
-- This allows OTP verification for Other Charges payments in addition to installment payments

-- Add chargeId column to cash_payment_otps table
ALTER TABLE cash_payment_otps 
ADD COLUMN charge_id VARCHAR(255);

-- Add index for better query performance on chargeId
CREATE INDEX idx_cash_payment_otps_charge_id ON cash_payment_otps(charge_id);

-- Add index for combined queries (chargeId + used + expiry_time)
CREATE INDEX idx_cash_payment_otps_charge_used_expiry ON cash_payment_otps(charge_id, used, expiry_time);

-- Update table comment to reflect new functionality
COMMENT ON TABLE cash_payment_otps IS 'Stores OTP records for cash payment verification. Supports both installment payments (schedule_id) and other charges (charge_id)';

-- Add column comments for clarity
COMMENT ON COLUMN cash_payment_otps.schedule_id IS 'Payment schedule ID for installment payments';
COMMENT ON COLUMN cash_payment_otps.charge_id IS 'Other charge ID for other charge payments';
COMMENT ON COLUMN cash_payment_otps.agreement_id IS 'Agreement ID for legacy agreement-based payments';