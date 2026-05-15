# Bring Your Own Razorpay - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

This guide will help you set up and test the "Bring Your Own Razorpay" feature locally.

## Prerequisites

- Java 17+
- PostgreSQL database
- Maven
- Razorpay test account (get from https://razorpay.com)

## Step 1: Database Setup

Run the migration script:

```bash
psql -U postgres -d hostel_management -f database_migration_owner_razorpay.sql
```

Verify table creation:

```sql
\d owner_razorpay_configs
```

## Step 2: Generate Encryption Key

Generate a secure encryption key:

```bash
openssl rand -base64 32
```

Copy the output (e.g., `K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=`)

## Step 3: Configure Application

Update `application.yml`:

```yaml
razorpay:
  # Default credentials (fallback)
  key:
    id: ${RAZORPAY_KEY_ID:rzp_test_xxxxx}
    secret: ${RAZORPAY_KEY_SECRET:secret_xxxxx}
  webhook:
    secret: ${RAZORPAY_WEBHOOK_SECRET:webhook_secret}
  
  # Encryption key (REQUIRED)
  encryption:
    key: ${RAZORPAY_ENCRYPTION_KEY:K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=}
```

Or set environment variable:

```bash
export RAZORPAY_ENCRYPTION_KEY="K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="
```

## Step 4: Start Application

```bash
cd HostelManagment_Backend
./mvnw spring-boot:run
```

## Step 5: Test API Endpoints

### 5.1 Get Razorpay Test Credentials

1. Go to https://dashboard.razorpay.com/
2. Sign up or log in
3. Navigate to Settings → API Keys
4. Copy your Test Key ID and Key Secret

### 5.2 Test Connection (as Owner)

```bash
curl -X POST http://localhost:8080/owner/payment-settings/test-connection \
  -H "Authorization: Bearer <owner-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "rzp_test_xxxxx",
    "keySecret": "your_secret_here"
  }'
```

Expected response:

```json
{
  "configId": "uuid",
  "ownerId": "uuid",
  "razorpayKeyId": "rzp_test_xxxxx",
  "maskedKeyId": "rzp_test...xxxx",
  "verificationStatus": "VERIFIED",
  "lastVerifiedAt": "2026-04-27T10:00:00",
  "isActive": false,
  "mcpOverrideDisabled": false,
  "paymentsEnabled": false
}
```

### 5.3 Save and Activate (as Owner)

```bash
curl -X POST http://localhost:8080/owner/payment-settings/save-and-activate \
  -H "Authorization: Bearer <owner-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "rzp_test_xxxxx",
    "keySecret": "your_secret_here"
  }'
```

Expected response:

```json
{
  "configId": "uuid",
  "ownerId": "uuid",
  "razorpayKeyId": "rzp_test_xxxxx",
  "maskedKeyId": "rzp_test...xxxx",
  "verificationStatus": "VERIFIED",
  "lastVerifiedAt": "2026-04-27T10:00:00",
  "isActive": true,
  "mcpOverrideDisabled": false,
  "paymentsEnabled": true
}
```

### 5.4 Create Payment Order (as Tenant)

```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Authorization: Bearer <tenant-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "agreementId": "your-agreement-id",
    "amount": 8500,
    "currency": "INR"
  }'
```

Expected response:

```json
{
  "orderId": "order_xxxxx",
  "providerKey": "rzp_test_xxxxx",
  "amount": 850000,
  "currency": "INR",
  "receiptId": "AGR-your-agreement-id",
  "provider": "razorpay"
}
```

### 5.5 Check MCP Statistics (as MCP)

```bash
curl -X GET http://localhost:8080/mcp/payment-monitoring/statistics \
  -H "Authorization: Bearer <mcp-jwt-token>"
```

Expected response:

```json
{
  "totalOwners": 5,
  "verifiedOwners": 3,
  "activeOwners": 2,
  "mcpDisabledOwners": 0
}
```

## Step 6: Test Payment Flow

### 6.1 Create Test Agreement

First, create a test agreement with an owner:

```bash
# Create agreement via your existing API
# Make sure it has an ownerId assigned
```

### 6.2 Configure Owner's Razorpay

```bash
# Use the test connection and save-and-activate endpoints above
```

### 6.3 Create Payment Order

```bash
# Use the create-order endpoint above
# Note the orderId and providerKey
```

### 6.4 Complete Payment (Frontend)

In your frontend, use the Razorpay checkout:

```javascript
const options = {
  key: response.providerKey, // Owner's Razorpay key
  amount: response.amount,
  currency: response.currency,
  order_id: response.orderId,
  handler: function(response) {
    // Verify payment
    verifyPayment({
      orderId: response.razorpay_order_id,
      paymentId: response.razorpay_payment_id,
      signature: response.razorpay_signature,
      agreementId: agreementId
    });
  }
};

const rzp = new Razorpay(options);
rzp.open();
```

### 6.5 Verify Payment

```bash
curl -X POST http://localhost:8080/api/payments/verify-payment \
  -H "Authorization: Bearer <tenant-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order_xxxxx",
    "paymentId": "pay_xxxxx",
    "signature": "signature_xxxxx",
    "agreementId": "your-agreement-id"
  }'
```

Expected response:

```json
{
  "verified": true,
  "paymentId": "pay_xxxxx",
  "orderId": "order_xxxxx",
  "message": "Payment verified successfully"
}
```

## Step 7: Test MCP Controls

### 7.1 Disable Owner's Payments

```bash
curl -X POST http://localhost:8080/mcp/payment-monitoring/configurations/{ownerId}/override \
  -H "Authorization: Bearer <mcp-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "disabled": true,
    "reason": "Testing MCP override"
  }'
```

### 7.2 Try Creating Payment Order (Should Fail)

```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Authorization: Bearer <tenant-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "agreementId": "your-agreement-id",
    "amount": 8500,
    "currency": "INR"
  }'
```

Expected error:

```json
{
  "error": "Payments are currently unavailable. Please contact the hostel owner to enable payment processing."
}
```

### 7.3 Re-enable Payments

```bash
curl -X POST http://localhost:8080/mcp/payment-monitoring/configurations/{ownerId}/override \
  -H "Authorization: Bearer <mcp-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "disabled": false,
    "reason": "Test completed"
  }'
```

## Troubleshooting

### Issue: "Encryption key not configured"

**Solution:** Set the `RAZORPAY_ENCRYPTION_KEY` environment variable or update `application.yml`

### Issue: "Authentication failed" during test connection

**Solution:** 
- Verify Razorpay credentials are correct
- Ensure you're using test mode credentials (rzp_test_...)
- Check if Razorpay API is accessible

### Issue: "Agreement not found"

**Solution:** 
- Ensure the agreement exists in MongoDB
- Verify the agreementId is correct
- Check if the agreement has an ownerId assigned

### Issue: "Payments are currently unavailable"

**Solution:**
- Check if owner has configured Razorpay
- Verify owner's payments are active
- Check if MCP has disabled payments
- Verify verification status is VERIFIED

### Issue: "Payment verification failed"

**Solution:**
- Ensure you're using the correct owner's credentials
- Verify the signature is correct
- Check if the orderId and paymentId match
- Ensure scheduleId is passed for installment payments

## Database Queries for Debugging

### Check owner configuration

```sql
SELECT 
    u.email,
    orc.razorpay_key_id,
    orc.verification_status,
    orc.is_active,
    orc.mcp_override_disabled,
    orc.last_verified_at
FROM users u
LEFT JOIN owner_razorpay_configs orc ON u.user_id = orc.owner_id
WHERE u.role = 'OWNER';
```

### Check payment enablement

```sql
SELECT 
    u.email,
    CASE 
        WHEN orc.is_active AND NOT orc.mcp_override_disabled 
             AND orc.verification_status = 'VERIFIED' 
        THEN 'ENABLED' 
        ELSE 'DISABLED' 
    END as payment_status,
    orc.verification_error,
    orc.mcp_override_reason
FROM users u
LEFT JOIN owner_razorpay_configs orc ON u.user_id = orc.owner_id
WHERE u.user_id = '<owner-uuid>';
```

### View MCP actions

```sql
SELECT 
    u.email as owner_email,
    mcp.email as mcp_email,
    orc.mcp_override_disabled,
    orc.mcp_override_reason,
    orc.mcp_override_at
FROM owner_razorpay_configs orc
JOIN users u ON orc.owner_id = u.user_id
LEFT JOIN users mcp ON orc.mcp_override_by = mcp.user_id
WHERE orc.mcp_override_by IS NOT NULL
ORDER BY orc.mcp_override_at DESC;
```

## Logs to Monitor

Watch application logs for these messages:

```
✅ OwnerRazorpayGateway initialized for owner: {ownerId}
✅ Razorpay credentials verified for owner: {ownerId}
✅ Razorpay payments activated for owner: {ownerId}
❌ Razorpay verification failed for owner {ownerId}: {error}
⚠️  Owner {ownerId} has no Razorpay configuration, using default gateway
```

## Next Steps

1. ✅ Backend is ready
2. ⏳ Implement frontend UI for owner payment settings
3. ⏳ Implement frontend UI for MCP monitoring dashboard
4. ⏳ Update payment flow in frontend to handle errors
5. ⏳ Add comprehensive testing
6. ⏳ Deploy to staging environment

## Resources

- [Razorpay API Documentation](https://razorpay.com/docs/api/)
- [Razorpay Test Cards](https://razorpay.com/docs/payments/payments/test-card-details/)
- [Complete Implementation Guide](./BRING_YOUR_OWN_RAZORPAY_IMPLEMENTATION.md)
- [Implementation Checklist](./BYOR_IMPLEMENTATION_CHECKLIST.md)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review application logs
3. Check database state
4. Refer to the complete implementation guide

---

**Happy Testing! 🚀**
