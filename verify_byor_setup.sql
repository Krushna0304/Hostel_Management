-- ============================================================================
-- BYOR Setup Verification Script
-- Run this to verify the setup is complete and correct
-- ============================================================================

\echo '========================================='
\echo 'BYOR Setup Verification'
\echo '========================================='
\echo ''

-- 1. Check if table exists
\echo '1. Checking if owner_razorpay_configs table exists...'
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM information_schema.tables 
            WHERE table_name = 'owner_razorpay_configs'
        ) 
        THEN '✅ Table exists'
        ELSE '❌ Table NOT found'
    END as status;

\echo ''

-- 2. Check table structure
\echo '2. Verifying table structure...'
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'owner_razorpay_configs'
ORDER BY ordinal_position;

\echo ''

-- 3. Check indexes
\echo '3. Checking indexes...'
SELECT 
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'owner_razorpay_configs'
ORDER BY indexname;

\echo ''

-- 4. Check constraints
\echo '4. Checking constraints...'
SELECT 
    conname as constraint_name,
    CASE contype
        WHEN 'p' THEN 'PRIMARY KEY'
        WHEN 'u' THEN 'UNIQUE'
        WHEN 'c' THEN 'CHECK'
        WHEN 'f' THEN 'FOREIGN KEY'
    END as constraint_type,
    pg_get_constraintdef(oid) as definition
FROM pg_constraint
WHERE conrelid = 'owner_razorpay_configs'::regclass
ORDER BY contype, conname;

\echo ''

-- 5. Check triggers
\echo '5. Checking triggers...'
SELECT 
    trigger_name,
    event_manipulation,
    action_timing,
    action_statement
FROM information_schema.triggers
WHERE event_object_table = 'owner_razorpay_configs';

\echo ''

-- 6. Check functions
\echo '6. Checking related functions...'
SELECT 
    routine_name,
    routine_type,
    data_type as return_type
FROM information_schema.routines
WHERE routine_name LIKE '%owner_razorpay%'
ORDER BY routine_name;

\echo ''

-- 7. Test data integrity
\echo '7. Testing table accessibility...'
SELECT 
    COUNT(*) as total_configs,
    COUNT(CASE WHEN verification_status = 'VERIFIED' THEN 1 END) as verified_count,
    COUNT(CASE WHEN is_active = true THEN 1 END) as active_count,
    COUNT(CASE WHEN mcp_override_disabled = true THEN 1 END) as mcp_disabled_count
FROM owner_razorpay_configs;

\echo ''

-- 8. Check foreign key relationships
\echo '8. Verifying foreign key relationships...'
SELECT 
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'owner_razorpay_configs';

\echo ''

-- 9. Summary
\echo '========================================='
\echo 'Verification Summary'
\echo '========================================='

SELECT 
    '✅ Setup Complete' as status,
    'All database objects created successfully' as message
WHERE EXISTS (
    SELECT 1 
    FROM information_schema.tables 
    WHERE table_name = 'owner_razorpay_configs'
)
AND EXISTS (
    SELECT 1 
    FROM information_schema.triggers 
    WHERE event_object_table = 'owner_razorpay_configs'
)
AND EXISTS (
    SELECT 1 
    FROM pg_indexes 
    WHERE tablename = 'owner_razorpay_configs'
);

\echo ''
\echo '========================================='
\echo 'Next Steps:'
\echo '1. Start backend: ./mvnw spring-boot:run'
\echo '2. Start frontend: npm run dev'
\echo '3. Test at: /owner/payment-settings'
\echo '========================================='
