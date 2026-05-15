-- ============================================
-- Payment Reminder System - Database Migration
-- ============================================

-- 1. Create owner_subscriptions table
CREATE TABLE IF NOT EXISTS owner_subscriptions (
    subscription_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    tier VARCHAR(50) NOT NULL CHECK (tier IN ('FREE', 'BETA', 'PRO', 'ENTERPRISE')),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sms_reminders_enabled BOOLEAN NOT NULL DEFAULT false,
    email_reminders_enabled BOOLEAN NOT NULL DEFAULT false,
    custom_templates_enabled BOOLEAN NOT NULL DEFAULT false,
    max_hostels INTEGER,
    max_tenants INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 2. Create sms_templates table
CREATE TABLE IF NOT EXISTS sms_templates (
    template_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    reminder_type VARCHAR(50) NOT NULL CHECK (reminder_type IN ('BEFORE_DUE_DATE', 'ON_DUE_DATE', 'AFTER_DUE_DATE')),
    template_content VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Create reminder_logs table
CREATE TABLE IF NOT EXISTS reminder_logs (
    log_id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    reminder_type VARCHAR(50) NOT NULL CHECK (reminder_type IN ('BEFORE_DUE_DATE', 'ON_DUE_DATE', 'AFTER_DUE_DATE')),
    message_sent VARCHAR(500) NOT NULL,
    sent_via VARCHAR(20) NOT NULL CHECK (sent_via IN ('SMS', 'EMAIL', 'BOTH')),
    success BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES payment_request_schedules(schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 4. Update tenant_payment_plans table
ALTER TABLE tenant_payment_plans 
ADD COLUMN IF NOT EXISTS room_allotment_id UUID;

ALTER TABLE tenant_payment_plans
ADD CONSTRAINT fk_tenant_payment_plan_allotment 
FOREIGN KEY (room_allotment_id) REFERENCES room_allotments(allotment_id) ON DELETE SET NULL;

-- 5. Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_owner_subscription_owner 
ON owner_subscriptions(owner_id, is_active);

CREATE INDEX IF NOT EXISTS idx_sms_template_owner_type 
ON sms_templates(owner_id, reminder_type, is_active);

CREATE INDEX IF NOT EXISTS idx_reminder_log_schedule 
ON reminder_logs(schedule_id, reminder_type);

CREATE INDEX IF NOT EXISTS idx_reminder_log_sent_at 
ON reminder_logs(sent_at);

CREATE INDEX IF NOT EXISTS idx_payment_schedule_due_date 
ON payment_request_schedules(due_date, payment_status);

-- 6. Insert default FREE subscriptions for existing owners
-- (Run this only if you want to create subscriptions for existing owners)
INSERT INTO owner_subscriptions (
    subscription_id,
    owner_id,
    tier,
    start_date,
    is_active,
    sms_reminders_enabled,
    email_reminders_enabled,
    custom_templates_enabled,
    max_hostels,
    max_tenants,
    created_at
)
SELECT 
    gen_random_uuid(),
    u.user_id,
    'FREE',
    CURRENT_TIMESTAMP,
    true,
    false,
    true,
    false,
    1,
    10,
    CURRENT_TIMESTAMP
FROM users u
INNER JOIN roles r ON u.role_id = r.role_id
WHERE r.role_name = 'OWNER'
AND NOT EXISTS (
    SELECT 1 FROM owner_subscriptions os 
    WHERE os.owner_id = u.user_id AND os.is_active = true
);

-- 7. Update existing tenant_payment_plans with room_allotment_id
-- (This links payment plans to room allotments for reminder context)
UPDATE tenant_payment_plans tpp
SET room_allotment_id = ra.allotment_id
FROM room_allotments ra
WHERE ra.tenant_id = tpp.tenant_id
AND ra.room_allotment_status = 'CONFIRMED'
AND tpp.room_allotment_id IS NULL;

-- ============================================
-- Verification Queries
-- ============================================

-- Check owner subscriptions
SELECT 
    u.display_name,
    os.tier,
    os.sms_reminders_enabled,
    os.email_reminders_enabled,
    os.custom_templates_enabled,
    os.is_active
FROM owner_subscriptions os
JOIN users u ON os.owner_id = u.user_id
WHERE os.is_active = true;

-- Check SMS templates
SELECT 
    u.display_name,
    st.reminder_type,
    LEFT(st.template_content, 50) as template_preview,
    st.is_active
FROM sms_templates st
JOIN users u ON st.owner_id = u.user_id
WHERE st.is_active = true;

-- Check reminder logs (last 10)
SELECT 
    u.display_name as tenant_name,
    rl.reminder_type,
    rl.sent_via,
    rl.success,
    rl.sent_at
FROM reminder_logs rl
JOIN users u ON rl.tenant_id = u.user_id
ORDER BY rl.sent_at DESC
LIMIT 10;

-- Check payment schedules due in next 7 days
SELECT 
    u.display_name as tenant_name,
    prs.due_date,
    prs.amount,
    prs.payment_status,
    prs.late_fee_applied
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.tenant_payment_plan = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
AND prs.payment_status IN ('SCHEDULED', 'PARTIALLY_PAID', 'OVERDUE')
ORDER BY prs.due_date;

-- ============================================
-- Rollback Script (if needed)
-- ============================================

-- Uncomment to rollback changes
/*
DROP TABLE IF EXISTS reminder_logs CASCADE;
DROP TABLE IF EXISTS sms_templates CASCADE;
DROP TABLE IF EXISTS owner_subscriptions CASCADE;
ALTER TABLE tenant_payment_plans DROP COLUMN IF EXISTS room_allotment_id;
*/
