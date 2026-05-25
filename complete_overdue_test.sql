-- ============================================================================
-- Complete Overdue Functionality Test
-- Run these queries step by step in PostgreSQL
-- ============================================================================

-- Step 1: Check current payment schedules
SELECT 
    prs.schedule_id,
    prs.due_date,
    prs.payment_status,
    prs.amount,
    prs.paid_amount,
    prs.late_fee_applied,
    u.name as tenant_name
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID')
ORDER BY prs.due_date;

-- Step 2: Create overdue scenario (set due date to past)
UPDATE payment_request_schedules 
SET due_date = CURRENT_DATE - INTERVAL '5 days'
WHERE payment_status = 'SCHEDULED' 
AND schedule_id = (
    SELECT schedule_id 
    FROM payment_request_schedules 
    WHERE payment_status = 'SCHEDULED' 
    LIMIT 1
);

-- Step 3: Mark as overdue and apply late fee (simulate the job)
UPDATE payment_request_schedules 
SET 
    payment_status = 'OVERDUE',
    late_fee_applied = CASE 
        WHEN (CURRENT_DATE - due_date) > 3 THEN 100 -- ₹100 late fee after 3 days
        ELSE 0 
    END
WHERE due_date < CURRENT_DATE 
AND payment_status = 'SCHEDULED';

-- Step 4: Verify overdue payments
SELECT 
    prs.schedule_id,
    prs.due_date,
    prs.payment_status,
    prs.amount,
    prs.paid_amount,
    prs.late_fee_applied,
    u.name as tenant_name,
    (CURRENT_DATE - prs.due_date) as days_overdue
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.payment_status = 'OVERDUE'
ORDER BY prs.due_date;

-- Step 5: Check collection summary (what dashboard shows)
SELECT 
    'Total Overdue' as metric,
    COUNT(*) as count,
    SUM(prs.amount - prs.paid_amount + prs.late_fee_applied) as total_amount
FROM payment_request_schedules prs
WHERE prs.payment_status = 'OVERDUE'
UNION ALL
SELECT 
    'Total Pending' as metric,
    COUNT(*) as count,
    SUM(prs.amount - prs.paid_amount + prs.late_fee_applied) as total_amount
FROM payment_request_schedules prs
WHERE prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID');