-- One-time migration for the "split electricity bill across tenants" change.
-- Run this once against the database. Hibernate's ddl-auto=update will NOT
-- relax existing NOT NULL constraints or drop columns, so these are manual.

-- 1. A bill is now shared across multiple tenants; it no longer has a single tenant.
ALTER TABLE electricity_bills DROP COLUMN IF EXISTS tenant_id;

-- 2. A payment row is now a per-tenant SHARE that starts PENDING. payment_mode and
--    payment-reference columns stay empty until the tenant actually pays the share.
ALTER TABLE electricity_payments ALTER COLUMN payment_mode DROP NOT NULL;
ALTER TABLE electricity_payments ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- 3. (Optional) Old electricity_payments rows were COMPLETED transactions under the
--    previous model, not shares. If this is a dev/test DB, clear the old electricity
--    data so the new split logic starts clean:
-- DELETE FROM electricity_payments;
-- DELETE FROM electricity_bills;
