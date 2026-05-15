-- Quick Setup Script for Other Charges Feature
-- Execute this script to set up the Other Charges functionality

-- Step 1: Create the tables
\i database_migration_other_charges.sql

-- Step 2: Verify tables were created
SELECT 
    table_name, 
    column_count
FROM (
    SELECT 
        table_name,
        COUNT(*) as column_count
    FROM information_schema.columns 
    WHERE table_name IN ('other_charges', 'other_charge_installments')
    GROUP BY table_name
) t
ORDER BY table_name;

-- Step 3: Check if indexes were created
SELECT 
    indexname,
    tablename
FROM pg_indexes 
WHERE tablename IN ('other_charges', 'other_charge_installments')
ORDER BY tablename, indexname;

-- Step 4: Insert sample data for testing (optional)
-- Uncomment the following lines if you want sample data

/*
-- Sample other charge for testing
INSERT INTO other_charges (
    charge_name, 
    description, 
    amount, 
    category, 
    owner_id, 
    tenant_id, 
    hostel_id, 
    due_date,
    installment_enabled,
    installment_count,
    installment_amount
) VALUES (
    'Electricity Bill - January 2024',
    'Monthly electricity charges for January',
    1500.00,
    'OTHER_CHARGE_TENANT',
    (SELECT user_id FROM users WHERE role = 'OWNER' LIMIT 1),
    (SELECT user_id FROM users WHERE role = 'TENANT' LIMIT 1),
    (SELECT hostel_id FROM hostels LIMIT 1),
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    true,
    3,
    500.00
);

-- Sample room-based charge
INSERT INTO other_charges (
    charge_name, 
    description, 
    amount, 
    category, 
    owner_id, 
    room_id, 
    hostel_id, 
    due_date
) VALUES (
    'Room Maintenance Fee',
    'Quarterly maintenance fee for room facilities',
    3000.00,
    'OTHER_CHARGE_ROOM',
    (SELECT user_id FROM users WHERE role = 'OWNER' LIMIT 1),
    (SELECT room_id FROM rooms LIMIT 1),
    (SELECT hostel_id FROM hostels LIMIT 1),
    CURRENT_TIMESTAMP + INTERVAL '14 days'
);
*/

-- Step 5: Verify the setup
SELECT 
    'Setup Complete!' as status,
    COUNT(*) as other_charges_count
FROM other_charges;

COMMENT ON SCRIPT IS 'Other Charges feature setup completed successfully. You can now use the Other Charges functionality in your hostel management system.';