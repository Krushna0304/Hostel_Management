@echo off
echo ============================================================================
echo Testing Overdue Functionality
echo ============================================================================

echo.
echo Step 1: Starting Backend (if not already running)...
echo Please make sure your backend is running on http://localhost:8080

echo.
echo Step 2: Testing API endpoint...
curl -X POST http://localhost:8080/api/test/trigger-overdue-job -H "Content-Type: application/json"

echo.
echo Step 3: Testing Collection Summary API...
echo Please login first and get your auth token, then run:
echo curl -X GET http://localhost:8080/api/owner/reports/collection-summary -H "Authorization: Bearer YOUR_TOKEN"

echo.
echo Step 4: Check database directly...
echo Run the SQL queries in complete_overdue_test.sql

echo.
echo Step 5: Test Frontend...
echo 1. Start frontend: npm run dev (in HostelManagement_Frontend folder)
echo 2. Login as owner
echo 3. Go to Collections Dashboard
echo 4. Look for red "overdue" badges

pause