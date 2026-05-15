-- Make plan_id nullable in transactions table to support other-charge payments
-- that are not associated with a TenantPaymentPlan
ALTER TABLE transactions ALTER COLUMN plan_id DROP NOT NULL;
