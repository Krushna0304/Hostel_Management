# Simple API test script
# Run this after logging in to get auth token

Write-Host "Testing Overdue Functionality via API"
Write-Host "======================================"

# Test if backend is running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"owner@example.com","password":"password123"}' -ErrorAction Stop
    Write-Host "✓ Backend is running"
    Write-Host "Response: $($response.StatusCode)"
} catch {
    Write-Host "✗ Backend not accessible: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Login to get auth token"
Write-Host "2. Use token to call: GET /api/owner/reports/collection-summary"
Write-Host "3. Look for overdue amounts in response"