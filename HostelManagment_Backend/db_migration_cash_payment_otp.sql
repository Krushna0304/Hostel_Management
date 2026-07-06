-- One-time migration for the normalized cash-payment OTP design.
-- Hibernate (ddl-auto=update) will CREATE cash_payment_allow and payment_otps,
-- but it will NOT drop the now-removed columns on cash_payment_otps. The old
-- `used` column is NOT NULL, so new OTP inserts would fail until it is dropped.

-- 1. Slim down cash_payment_otps to (otp_id, otp_hash, expiry_time, owner_phone, created_at).
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS used;
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS agreement_id;
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS schedule_id;
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS charge_id;
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS settlement_id;
ALTER TABLE cash_payment_otps DROP COLUMN IF EXISTS electricity_bill_id;

-- 2. New tables are created automatically by Hibernate. For reference, their shape:
--    cash_payment_allow(cp_id uuid pk, owner_id uuid, method_name varchar,
--                       is_allowed boolean, created_at timestamptz,
--                       unique(owner_id, method_name))
--    payment_otps(id uuid pk, otp_id uuid, cp_id uuid, request_id varchar,
--                 used boolean, created_at timestamptz)

-- 3. (Optional) Seed config rows for existing owners so the Settings screen is
--    pre-populated. The app also creates these lazily on first access, so this is
--    optional. Disabled by default for the three configurable methods.
-- INSERT INTO cash_payment_allow (cp_id, owner_id, method_name, is_allowed, created_at)
-- SELECT gen_random_uuid(), u.user_id, m.method_name, m.is_allowed, now()
-- FROM users u
-- JOIN roles r ON r.role_id = u.role_id AND r.name = 'OWNER'
-- CROSS JOIN (VALUES
--     ('ELECTRICITY_BILL', false),
--     ('INSTALLMENT',      false),
--     ('OTHER_CHARGE',     false),
--     ('AGREEMENT',        true),
--     ('SETTLEMENT',       true)
-- ) AS m(method_name, is_allowed)
-- ON CONFLICT (owner_id, method_name) DO NOTHING;

-- 4. (Optional) Drop any stale OTP rows from the old design.
-- DELETE FROM cash_payment_otps;
