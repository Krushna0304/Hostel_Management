-- Make agreement_id nullable in cash_payment_otps table
-- This allows the table to support both agreement payments and installment payments

ALTER TABLE cash_payment_otps 
ALTER COLUMN agreement_id DROP NOT NULL;

-- Add a comment to explain the change
COMMENT ON COLUMN cash_payment_otps.agreement_id IS 'Agreement ID for agreement activation payments. NULL for installment payments.';
COMMENT ON COLUMN cash_payment_otps.schedule_id IS 'Schedule ID for installment payments. NULL for agreement activation payments.';