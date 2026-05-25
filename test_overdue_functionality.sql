-- ============================================================================
-- Test Script: Overdue Functionality Testing
-- Purpose: Manually test overdue payment functionality
-- ============================================================================

-- Step 1: Check current payment schedules
SELECT 
    prs.schedule_id,
    prs.due_date,
    prs.payment_status,
    prs.amount,
    prs.paid_amount,
    prs.late_fee_applied,
    u.name as tenant_name,
    h.name as hostel_name,
    r.room_number
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
LEFT JOIN room_allotments ra ON tpp.room_allotment_id = ra.allotment_id
LEFT JOIN rooms r ON ra.room_id = r.room_id
LEFT JOIN hostels h ON r.hostel_id = h.hostel_id
WHERE prs.payment_status IN ('SCHEDULED', 'OVERDUE', 'PARTIALLY_PAID')
ORDER BY prs.due_date;

-- Step 2: Create test overdue scenario
-- Update a SCHEDULED payment to have a past due date
UPDATE payment_request_schedules 
SET due_date = CURRENT_DATE - INTERVAL '5 days'
WHERE payment_status = 'SCHEDULED' 
AND schedule_id = (
    SELECT schedule_id 
    FROM payment_request_schedules 
    WHERE payment_status = 'SCHEDULED' 
    LIMIT 1
);

-- Step 3: Verify the update
SELECT 
    schedule_id,
    due_date,
    payment_status,
    amount,
    late_fee_applied,
    CURRENT_DATE as today,
    (CURRENT_DATE - due_date) as days_overdue
FROM payment_request_schedules 
WHERE due_date < CURRENT_DATE 
AND payment_status = 'SCHEDULED';

-- Step 4: Manually trigger overdue logic (simulate the job)
-- This will mark overdue payments and apply late fees
UPDATE payment_request_schedules 
SET 
    payment_status = 'OVERDUE',
    late_fee_applied = CASE 
        WHEN (CURRENT_DATE - due_date) > 3 THEN 100 -- Example: ₹100 late fee after 3 days
        ELSE 0 
    END
WHERE due_date < CURRENT_DATE 
AND payment_status = 'SCHEDULED';

-- Step 5: Verify overdue payments are marked correctly
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

-- Step 6: Check collection dashboard data
-- This query simulates what the CollectionDashboard shows
SELECT 
    u.name as tenant_name,
    h.name as hostel_name,
    r.room_number,
    COUNT(CASE WHEN prs.payment_status = 'OVERDUE' THEN 1 END) as overdue_installments,
    SUM(CASE WHEN prs.payment_status = 'OVERDUE' THEN (prs.amount - prs.paid_amount + prs.late_fee_applied) ELSE 0 END) as total_overdue_amount
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.plan_id = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
LEFT JOIN room_allotments ra ON tpp.room_allotment_id = ra.allotment_id
LEFT JOIN rooms r ON ra.room_id = r.room_id
LEFT JOIN hostels h ON r.hostel_id = h.hostel_id
GROUP BY u.user_id, u.name, h.name, r.room_number
HAVING COUNT(CASE WHEN prs.payment_status = 'OVERDUE' THEN 1 END) > 0
ORDER BY total_overdue_amount DESC;

-- Step 7: Reset test data (run this to clean up after testing)
-- UPDATE payment_request_schedules 
-- SET 
--     payment_status = 'SCHEDULED',
--     late_fee_applied = 0,
--     due_date = CURRENT_DATE + INTERVAL '30 days'
-- WHERE payment_status = 'OVERDUE' 
-- AND late_fee_applied > 0;