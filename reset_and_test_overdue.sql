-- ============================================================================
-- Reset and Test Overdue Functionality Again
-- Run these queries step by step in PostgreSQL
-- ============================================================================

-- Step 1: Check current status of all payment schedules
SELECT 
    prs.schedule_id,
    prs.due_date,
    prs.payment_status,
    prs.amount,
    prs.paid_amount,
    prs.late_fee_applied,
    u.name as tenant_name,
    prs.installment_number
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID')
ORDER BY prs.due_date;

-- Step 2: Reset any existing overdue payments back to SCHEDULED (cleanup from previous tests)
UPDATE payment_request_schedules 
SET 
    payment_status = 'SCHEDULED',
    late_fee_applied = 0,
    due_date = CURRENT_DATE + INTERVAL '30 days'
WHERE payment_status = 'OVERDUE';

-- Step 3: Create a fresh overdue scenario - pick a SCHEDULED payment and make it overdue
UPDATE payment_request_schedules 
SET due_date = CURRENT_DATE - INTERVAL '7 days'  -- 7 days overdue
WHERE payment_status = 'SCHEDULED' 
AND schedule_id = (
    SELECT schedule_id 
    FROM payment_request_schedules 
    WHERE payment_status = 'SCHEDULED' 
    ORDER BY due_date ASC
    LIMIT 1
);

-- Step 4: Simulate the PaymentOverdueJob - mark overdue and apply late fee
UPDATE payment_request_schedules 
SET 
    payment_status = 'OVERDUE',
    late_fee_applied = CASE 
        WHEN (CURRENT_DATE - due_date) > 3 THEN 150  -- ₹150 late fee after 3 days
        ELSE 50  -- ₹50 base late fee
    END
WHERE due_date < CURRENT_DATE 
AND payment_status = 'SCHEDULED';

-- Step 5: Verify the overdue payment was created
SELECT 
    prs.schedule_id,
    prs.due_date,
    prs.payment_status,
    prs.amount,
    prs.paid_amount,
    prs.late_fee_applied,
    u.name as tenant_name,
    prs.installment_number,
    (CURRENT_DATE - prs.due_date) as days_overdue,
    (prs.amount - prs.paid_amount + prs.late_fee_applied) as total_due
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.payment_status = 'OVERDUE'
ORDER BY prs.due_date;

-- Step 6: Get collection summary (what the dashboard API returns)
SELECT 
    'Total Collected' as metric,
    COALESCE(SUM(prs.paid_amount), 0) as amount
FROM payment_request_schedules prs
WHERE prs.payment_status = 'COMPLETED'
UNION ALL
SELECT 
    'Total Pending' as metric,
    COALESCE(SUM(prs.amount - prs.paid_amount + prs.late_fee_applied), 0) as amount
FROM payment_request_schedules prs
WHERE prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID')
UNION ALL
SELECT 
    'Total Overdue' as metric,
    COALESCE(SUM(prs.amount - prs.paid_amount + prs.late_fee_applied), 0) as amount
FROM payment_request_schedules prs
WHERE prs.payment_status = 'OVERDUE'
UNION ALL
SELECT 
    'Overdue Tenants Count' as metric,
    COUNT(DISTINCT tpp.tenant_id) as amount
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
WHERE prs.payment_status = 'OVERDUE';

-- Step 7: Get tenant-wise overdue breakdown (what shows in the table)
SELECT 
    u.name as tenant_name,
    u.user_id as tenant_id,
    COUNT(CASE WHEN prs.payment_status = 'OVERDUE' THEN 1 END) as overdue_installments,
    COUNT(CASE WHEN prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID') THEN 1 END) as pending_installments,
    SUM(CASE WHEN prs.payment_status = 'OVERDUE' THEN (prs.amount - prs.paid_amount + prs.late_fee_applied) ELSE 0 END) as total_overdue_amount,
    tpp.installment_amount as monthly_rent
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
GROUP BY u.user_id, u.name, tpp.installment_amount
HAVING COUNT(CASE WHEN prs.payment_status = 'OVERDUE' THEN 1 END) > 0
ORDER BY total_overdue_amount DESC;