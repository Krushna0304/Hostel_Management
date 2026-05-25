-- ============================================================================
-- HOSTEL MANAGEMENT SYSTEM — COMPLETE DB CLEANUP SCRIPT (PostgreSQL + MongoDB)
-- ============================================================================
-- PURPOSE : Wipe all transactional / operational data and reset sequences
--           while PRESERVING master / lookup tables.
--
--  PRESERVED (master data):
--    PostgreSQL:  roles, enhanced_charges, payment_types
--    MongoDB:     room_agreement_plans (collection)
--
--  CLEANED (transactional data):
--    PostgreSQL:  25 tables  (see list below)
--    MongoDB:     agreements (collection) — see Section C at bottom
--
-- USAGE   : Run as a superuser or the DB owner.
--           Safe to run repeatedly (idempotent).
-- UPDATED : 2026-05-24
-- ============================================================================


-- ============================================================================
-- SECTION A — STANDALONE SCRIPT (run in psql / DBeaver / pgAdmin)
-- ============================================================================

BEGIN;

-- Disable triggers temporarily to avoid FK constraint issues during truncation
SET session_replication_role = replica;

-- ----------------------------------------------------------------
-- 1. Electricity billing data  (leaf tables first)
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS electricity_payments          RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS electricity_bills             RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS electricity_accounts          RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 2. Settlement data
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS settlement_requests           RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 3. Payment & billing data
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS cash_payment_otps             RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS other_charge_installments     RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS other_charges                 RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS installment_invoices          RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS monthly_billing_records       RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS payment_request_schedules     RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS tenant_payment_plans          RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS transactions                  RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 4. Tenancy & accommodation data
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS room_allotments               RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 5. Auth tokens
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS password_reset_tokens         RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 6. Hostel operational data (reminders, SMS, subscriptions, razorpay)
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS announcements                 RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS reminder_logs                 RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS sms_templates                 RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS owner_subscriptions           RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS owner_razorpay_configs        RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 7. Property structure data  (rooms → floors → hostels)
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS rooms                         RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS floors                        RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS hostels                       RESTART IDENTITY CASCADE;

-- ----------------------------------------------------------------
-- 8. User contact details & users
--    (roles are preserved — users FK into roles)
-- ----------------------------------------------------------------
TRUNCATE TABLE IF EXISTS user_contact_details          RESTART IDENTITY CASCADE;
TRUNCATE TABLE IF EXISTS users                         RESTART IDENTITY CASCADE;

-- Re-enable triggers
SET session_replication_role = DEFAULT;

COMMIT;

-- ----------------------------------------------------------------
-- Verification: show row counts for all cleaned tables
-- ----------------------------------------------------------------
DO $$
DECLARE
    v_tables TEXT[] := ARRAY[
        'electricity_payments', 'electricity_bills', 'electricity_accounts',
        'settlement_requests',
        'cash_payment_otps', 'other_charge_installments', 'other_charges',
        'installment_invoices', 'monthly_billing_records',
        'payment_request_schedules', 'tenant_payment_plans', 'transactions',
        'room_allotments',
        'password_reset_tokens',
        'announcements', 'reminder_logs', 'sms_templates',
        'owner_subscriptions', 'owner_razorpay_configs',
        'rooms', 'floors', 'hostels',
        'user_contact_details', 'users'
    ];
    v_tbl  TEXT;
    v_cnt  BIGINT;
BEGIN
    RAISE NOTICE '========================================================';
    RAISE NOTICE ' CLEANED TABLES — Row counts (should all be 0)';
    RAISE NOTICE '========================================================';
    FOREACH v_tbl IN ARRAY v_tables LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = v_tbl AND table_schema = 'public') THEN
            EXECUTE format('SELECT COUNT(*) FROM %I', v_tbl) INTO v_cnt;
            RAISE NOTICE '  %-35s → % rows', v_tbl, v_cnt;
        ELSE
            RAISE NOTICE '  %-35s → TABLE NOT FOUND (skipped)', v_tbl;
        END IF;
    END LOOP;
END $$;

-- ----------------------------------------------------------------
-- Masters that are KEPT (confirm row counts)
-- ----------------------------------------------------------------
DO $$
DECLARE
    v_masters TEXT[] := ARRAY['roles', 'enhanced_charges', 'payment_types'];
    v_tbl    TEXT;
    v_cnt    BIGINT;
BEGIN
    RAISE NOTICE '========================================================';
    RAISE NOTICE ' PRESERVED MASTER TABLES (PostgreSQL)';
    RAISE NOTICE '========================================================';
    FOREACH v_tbl IN ARRAY v_masters LOOP
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = v_tbl AND table_schema = 'public') THEN
            EXECUTE format('SELECT COUNT(*) FROM %I', v_tbl) INTO v_cnt;
            RAISE NOTICE '  ✅ KEPT — %-30s → % rows', v_tbl, v_cnt;
        ELSE
            RAISE NOTICE '  ⚠️  %-30s → TABLE NOT FOUND', v_tbl;
        END IF;
    END LOOP;
    RAISE NOTICE '';
    RAISE NOTICE ' PRESERVED MASTER COLLECTIONS (MongoDB — not managed by this script):';
    RAISE NOTICE '  ✅ KEPT — room_agreement_plans (MongoDB collection)';
END $$;


-- ============================================================================
-- SECTION B — STORED PROCEDURE  (call anytime: CALL clean_hostel_db();)
-- ============================================================================

CREATE OR REPLACE PROCEDURE clean_hostel_db(
    p_dry_run BOOLEAN DEFAULT FALSE   -- TRUE → only prints what would be deleted
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_tables TEXT[] := ARRAY[
        -- Order: most-dependent (leaves) → least-dependent (roots)
        -- Electricity module
        'electricity_payments',
        'electricity_bills',
        'electricity_accounts',
        -- Settlement module
        'settlement_requests',
        -- Payments & billing
        'cash_payment_otps',
        'other_charge_installments',
        'other_charges',
        'installment_invoices',
        'monthly_billing_records',
        'payment_request_schedules',
        'tenant_payment_plans',
        'transactions',
        -- Tenancy
        'room_allotments',
        -- Auth tokens
        'password_reset_tokens',
        -- Operational
        'announcements',
        'reminder_logs',
        'sms_templates',
        'owner_subscriptions',
        'owner_razorpay_configs',
        -- Property structure
        'rooms',
        'floors',
        'hostels',
        -- Users
        'user_contact_details',
        'users'
    ];
    v_tbl    TEXT;
    v_cnt    BIGINT;
    v_total  BIGINT := 0;
BEGIN
    RAISE NOTICE '========================================================';
    RAISE NOTICE ' Hostel DB Cleanup — dry_run = %', p_dry_run;
    RAISE NOTICE '========================================================';

    IF NOT p_dry_run THEN
        -- Bypass FK triggers for the session
        SET LOCAL session_replication_role = replica;
    END IF;

    FOREACH v_tbl IN ARRAY v_tables LOOP
        -- Check if table exists first (graceful handling)
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_name = v_tbl AND table_schema = 'public'
        ) THEN
            EXECUTE format('SELECT COUNT(*) FROM %I', v_tbl) INTO v_cnt;
            v_total := v_total + v_cnt;

            IF p_dry_run THEN
                RAISE NOTICE '[DRY-RUN] Would delete % rows from "%"', v_cnt, v_tbl;
            ELSE
                EXECUTE format('TRUNCATE TABLE %I RESTART IDENTITY CASCADE', v_tbl);
                RAISE NOTICE '[DONE] Truncated "%" (had % rows)', v_tbl, v_cnt;
            END IF;
        ELSE
            RAISE NOTICE '[SKIP] Table "%" does not exist', v_tbl;
        END IF;
    END LOOP;

    IF NOT p_dry_run THEN
        SET LOCAL session_replication_role = DEFAULT;
    END IF;

    RAISE NOTICE '--------------------------------------------------------';
    IF p_dry_run THEN
        RAISE NOTICE 'DRY-RUN COMPLETE — % total rows would be deleted', v_total;
    ELSE
        RAISE NOTICE 'CLEANUP COMPLETE — % total rows deleted', v_total;
    END IF;
    RAISE NOTICE '';
    RAISE NOTICE 'PostgreSQL master tables preserved:';
    RAISE NOTICE '  • roles';
    RAISE NOTICE '  • enhanced_charges';
    RAISE NOTICE '  • payment_types';
    RAISE NOTICE '';
    RAISE NOTICE '⚠  MongoDB collections NOT handled by this procedure:';
    RAISE NOTICE '  • agreements       → run: db.agreements.deleteMany({})';
    RAISE NOTICE '  • room_agreement_plans → KEEP (master data)';
    RAISE NOTICE '========================================================';
END;
$$;


-- ============================================================================
-- SECTION C — MONGODB CLEANUP  (run in mongosh / MongoDB Compass)
-- ============================================================================
--
--  Connect to your MongoDB database and run:
--
--    // DELETE transactional collection
--    db.agreements.deleteMany({});
--
--    // KEEP master collection (do NOT delete)
--    // db.room_agreement_plans  ← DO NOT TOUCH
--
-- ============================================================================


-- ============================================================================
-- HOW TO USE
-- ============================================================================
--
--  ┌─────────────────────────────────────────────────────────────────┐
--  │  STEP 1 — PostgreSQL cleanup                                   │
--  │                                                                 │
--  │  OPTION A — Run this entire file as-is in psql / DBeaver /     │
--  │             pgAdmin.  The standalone script above will execute  │
--  │             immediately and clean the database.                 │
--  │                                                                 │
--  │  OPTION B — After running this file once (to create the         │
--  │             procedure), call it anytime:                        │
--  │                                                                 │
--  │    Dry-run (preview only, no changes):                          │
--  │        CALL clean_hostel_db(TRUE);                              │
--  │                                                                 │
--  │    Full clean:                                                  │
--  │        CALL clean_hostel_db();                                  │
--  │        -- or --                                                 │
--  │        CALL clean_hostel_db(FALSE);                             │
--  │                                                                 │
--  │  STEP 2 — MongoDB cleanup (run in mongosh / Compass)           │
--  │                                                                 │
--  │    db.agreements.deleteMany({});                                │
--  │                                                                 │
--  └─────────────────────────────────────────────────────────────────┘
--
-- ════════════════════════════════════════════════════════════════════
-- COMPLETE TABLE INVENTORY (29 tables/collections)
-- ════════════════════════════════════════════════════════════════════
--
-- PRESERVED MASTER DATA (4):
--   PostgreSQL:
--     • roles                     — User role definitions (OWNER, TENANT, ADMIN)
--     • enhanced_charges          — Plan-level charge templates
--     • payment_types             — Payment type lookup
--   MongoDB:
--     • room_agreement_plans      — Plan configurations (collection)
--
-- CLEANED TRANSACTIONAL DATA (25):
--   PostgreSQL (24):
--     • electricity_payments, electricity_bills, electricity_accounts
--     • settlement_requests
--     • cash_payment_otps, other_charge_installments, other_charges
--     • installment_invoices, monthly_billing_records
--     • payment_request_schedules, tenant_payment_plans, transactions
--     • room_allotments
--     • password_reset_tokens
--     • announcements, reminder_logs, sms_templates
--     • owner_subscriptions, owner_razorpay_configs
--     • rooms, floors, hostels
--     • user_contact_details, users
--   MongoDB (1):
--     • agreements               — Rental agreements (collection)
--
-- ============================================================================
