# Testing Owner Authentication

## Issue
Getting 403 Forbidden when accessing `/owner/payment-settings/test-connection` even after logging in as an owner.

## Root Cause
The backend needs to be restarted after updating `CustomUserDetails.java` to properly load user roles.

## Solution Steps

### 1. ✅ Backend Compiled Successfully
The code has been recompiled with the updated `CustomUserDetails.java`.

### 2. ✅ Database Has OWNER Users
Verified that users with OWNER role exist:
- Test
- Test099
- Test77
- Test99
- krush

### 3. Restart Backend (REQUIRED)

**You need to restart the backend application:**

```bash
# Stop the current backend (Ctrl+C in the terminal where it's running)

# Then restart:
cd HostelManagment_Backend
./mvnw spring-boot:run
```

### 4. Clear Browser Cache and Re-login

After restarting the backend:

1. **Clear browser cache** or open an incognito window
2. **Logout** from the application
3. **Login again** as one of the OWNER users (e.g., "Test" or "krush")
4. **Navigate** to `/owner/payment-settings`
5. **Try** the test connection feature

### 5. Verify JWT Token Contains Role

After logging in, check the browser's localStorage:

```javascript
// Open browser console (F12) and run:
const token = localStorage.getItem('authToken');
console.log('Token:', token);

// Decode the JWT (you can use jwt.io)
const payload = JSON.parse(atob(token.split('.')[1]));
console.log('Payload:', payload);
// Should contain role information
```

## About Razorpay Credentials

### Test vs Production Credentials

**For Testing (Recommended):**
- Use Razorpay **Test Mode** credentials
- Key ID starts with: `rzp_test_`
- These are safe to use for development
- No real money is processed
- You already have test credentials in `application.yml`:
  - Key ID: `rzp_test_S7mavZx4rKjEXU`
  - Key Secret: `MXh52T3LvNSX3ISMOvfkhSec`

**For Production:**
- Use Razorpay **Live Mode** credentials
- Key ID starts with: `rzp_live_`
- Real money is processed
- Only use when deploying to production

### Do You Need Original Credentials?

**No, you don't need to use original/production credentials for testing!**

You can use:
1. **The test credentials already in application.yml** (recommended for testing)
2. **Your own Razorpay test account credentials** (if you want to test with your account)
3. **Dummy/fake credentials** (will fail verification, but good for UI testing)

### Testing the Feature

**Option 1: Use Existing Test Credentials**
```
Key ID: rzp_test_S7mavZx4rKjEXU
Key Secret: MXh52T3LvNSX3ISMOvfkhSec
```
These should work for testing the connection.

**Option 2: Get Your Own Test Credentials**
1. Go to https://dashboard.razorpay.com/
2. Sign up for a free account
3. Navigate to Settings → API Keys
4. Generate **Test Mode** keys
5. Use those in the payment settings page

**Option 3: Test with Invalid Credentials**
- Enter any dummy values
- Click "Test Connection"
- Should see "Verification Failed" message
- This tests the error handling

## Expected Behavior After Fix

### ✅ Success Flow:
1. Login as OWNER
2. Navigate to `/owner/payment-settings`
3. Page loads without 403 error
4. Enter Razorpay credentials
5. Click "Test Connection"
6. See success or failure message (depending on credentials)

### ❌ If Still Getting 403:
1. Backend not restarted
2. User doesn't have OWNER role
3. JWT token expired (logout and login again)
4. Browser cache issue (clear cache)

## Quick Test Commands

### Test the endpoint with curl (after backend restart):

```bash
# 1. Login and get token
curl -X POST http://localhost:8080/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "Test", "password": "your_password"}'

# Copy the token from response

# 2. Test the endpoint
curl -X GET http://localhost:8080/owner/payment-settings \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Should return 200 OK with configuration data
```

## Summary

1. ✅ Code is fixed and compiled
2. ✅ Database has OWNER users
3. ⏳ **RESTART BACKEND** (most important step!)
4. ⏳ Clear browser cache and re-login
5. ⏳ Test the payment settings page

**You do NOT need production Razorpay credentials for testing. Use test mode credentials!**
