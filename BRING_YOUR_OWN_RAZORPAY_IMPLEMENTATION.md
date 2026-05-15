# Bring Your Own Razorpay - Implementation Guide

## Overview

This document describes the complete implementation of the "Bring Your Own Razorpay" (BYOR) model in the Hostel Management System. This feature allows each hostel owner to connect their own Razorpay account, ensuring payments go directly to their account while maintaining platform-level control through the MCP (Master Control Panel).

## Architecture

### Core Principles

1. **Multi-Tenant Payment Processing**: Each owner has their own Razorpay credentials
2. **Secure Credential Storage**: All secrets are encrypted using AES-256-GCM
3. **Role-Based Access Control**: Strict separation between Owner, MCP, and other roles
4. **Dynamic Gateway Selection**: Payment gateway is selected at runtime based on the owner
5. **MCP Override Capability**: Platform can disable payments per owner when needed

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Payment Flow                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Tenant → PaymentController → PaymentService                │
│                                    ↓                         │
│                          Identify Owner (from Agreement)     │
│                                    ↓                         │
│                          OwnerRazorpayService                │
│                                    ↓                         │
│                          Get Encrypted Credentials           │
│                                    ↓                         │
│                          EncryptionService (Decrypt)         │
│                                    ↓                         │
│                          OwnerRazorpayGateway (Dynamic)      │
│                                    ↓                         │
│                          Razorpay API (Owner's Account)      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

### Table: `owner_razorpay_configs`

```sql
CREATE TABLE owner_razorpay_configs (
    config_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL UNIQUE,
    razorpay_key_id VARCHAR(100) NOT NULL,
    razorpay_key_secret_encrypted VARCHAR(500) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_VERIFIED',
    last_verified_at TIMESTAMP,
    verification_error VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    mcp_override_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    mcp_override_reason VARCHAR(500),
    mcp_override_by UUID,
    mcp_override_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Payment Enablement Logic

Payments are enabled for an owner ONLY when ALL conditions are met:

```java
isPaymentsEnabled = is_active 
                    AND NOT mcp_override_disabled 
                    AND verification_status = 'VERIFIED'
```

## Backend Implementation

### 1. Models

#### OwnerRazorpayConfig.java
- Stores encrypted Razorpay credentials per owner
- Tracks verification status and MCP overrides
- Never exposes raw secrets

### 2. Services

#### OwnerRazorpayService.java
**Responsibilities:**
- Manage owner Razorpay configurations
- Test and verify credentials
- Activate/deactivate payments
- Provide decrypted credentials for payment processing (internal only)

**Key Methods:**
- `getOwnerConfig(UUID ownerId)` - Get configuration (masked secrets)
- `testConnection(UUID ownerId, RazorpayConfigRequest request)` - Verify credentials
- `saveAndActivate(UUID ownerId, RazorpayConfigRequest request)` - Save and enable
- `getDecryptedCredentials(UUID ownerId)` - Internal use only
- `isPaymentsEnabled(UUID ownerId)` - Check if payments are enabled

#### McpRazorpayService.java
**Responsibilities:**
- Monitor all owner configurations
- Enable/disable payments per owner
- Force re-verification
- Provide statistics

**Key Methods:**
- `getAllConfigurations()` - List all owner configs
- `mcpOverride(UUID ownerId, McpOverrideRequest request)` - Enable/disable payments
- `forceReVerification(UUID ownerId)` - Force owner to re-verify
- `getStatistics()` - Dashboard statistics

#### EncryptionService.java
**Responsibilities:**
- Encrypt/decrypt Razorpay secrets using AES-256-GCM
- Uses key from `application.yml`: `razorpay.encryption.key`

**Security:**
- Generate key: `openssl rand -base64 32`
- Store in environment variable or secure vault
- Never commit encryption key to version control

#### PaymentService.java (Updated)
**Changes:**
- Now identifies owner from Agreement or PaymentSchedule
- Dynamically creates `OwnerRazorpayGateway` with owner's credentials
- Falls back to default gateway if owner has no configuration
- Validates payment enablement before processing

**Key Methods:**
- `getPaymentGatewayForOwner(UUID ownerId)` - Get owner-specific gateway
- `createAgreementPaymentOrder()` - Updated to use owner credentials
- `createInstallmentPaymentOrder()` - Updated to use owner credentials
- `verifyAgreementPayment()` - Updated to use owner credentials
- `verifyInstallmentPayment()` - Updated to use owner credentials

#### OwnerRazorpayGateway.java
**Responsibilities:**
- Dynamic payment gateway created per transaction
- Uses owner-specific Razorpay credentials
- Implements `PaymentGateway` interface

**Lifecycle:**
- Created on-demand for each payment
- Initialized with owner's decrypted credentials
- Disposed after transaction

### 3. Controllers

#### OwnerRazorpayController.java
**Route:** `/owner/payment-settings`
**Access:** `@PreAuthorize("hasRole('OWNER')")`

**Endpoints:**
- `GET /` - Get current configuration
- `POST /test-connection` - Test Razorpay credentials
- `POST /save-and-activate` - Save and activate payments
- `POST /deactivate` - Deactivate payments
- `GET /status` - Check if payments are enabled

#### McpRazorpayController.java
**Route:** `/mcp/payment-monitoring`
**Access:** `@PreAuthorize("hasRole('MCP') or hasRole('ADMIN')")`

**Endpoints:**
- `GET /configurations` - List all owner configurations
- `GET /configurations/{ownerId}` - Get specific owner config
- `POST /configurations/{ownerId}/override` - Enable/disable payments
- `POST /configurations/{ownerId}/force-reverify` - Force re-verification
- `GET /statistics` - Get dashboard statistics

### 4. DTOs

#### RazorpayConfigRequest.java
```java
{
  "keyId": "rzp_live_xxxxx",
  "keySecret": "secret_xxxxx"
}
```

#### RazorpayConfigResponse.java
```java
{
  "configId": "uuid",
  "ownerId": "uuid",
  "razorpayKeyId": "rzp_live_xxxxx",
  "maskedKeyId": "rzp_live...xxxx",
  "verificationStatus": "VERIFIED",
  "lastVerifiedAt": "2026-04-27T10:00:00",
  "isActive": true,
  "mcpOverrideDisabled": false,
  "paymentsEnabled": true
}
```

#### McpOverrideRequest.java
```java
{
  "disabled": true,
  "reason": "Suspicious activity detected"
}
```

## Security Considerations

### 1. Credential Storage
- ✅ Secrets encrypted with AES-256-GCM
- ✅ Encryption key stored in environment variable
- ✅ Never exposed in API responses
- ✅ Key ID partially masked in responses

### 2. Access Control
- ✅ Only OWNER can enter/modify credentials
- ✅ MCP can monitor and control but cannot view secrets
- ✅ Other roles have no access
- ✅ Role-based authorization enforced at controller level

### 3. Payment Verification
- ✅ Credentials verified via Razorpay API before activation
- ✅ Test API call made during verification
- ✅ Failed verifications prevent activation
- ✅ MCP can force re-verification

### 4. Audit Trail
- ✅ All MCP actions logged with user ID and timestamp
- ✅ Verification attempts tracked
- ✅ Payment enablement changes recorded

## Configuration

### application.yml

```yaml
razorpay:
  # Default credentials (fallback)
  key:
    id: ${RAZORPAY_KEY_ID:rzp_test_xxxxx}
    secret: ${RAZORPAY_KEY_SECRET:secret_xxxxx}
  webhook:
    secret: ${RAZORPAY_WEBHOOK_SECRET:webhook_secret}
  
  # Encryption key for owner credentials (REQUIRED)
  encryption:
    key: ${RAZORPAY_ENCRYPTION_KEY:}  # Generate: openssl rand -base64 32
```

### Environment Variables

```bash
# Default Razorpay (fallback)
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=secret_xxxxx
RAZORPAY_WEBHOOK_SECRET=webhook_secret

# Encryption key (CRITICAL - Keep secure!)
RAZORPAY_ENCRYPTION_KEY=<base64-encoded-32-byte-key>
```

## Frontend Integration (To Be Implemented)

### Owner Payment Settings Page

**Route:** `/owner/payment-settings`

**UI Components:**
1. **Connection Status Card**
   - Show current status (Not Connected, Verified, Active, Disabled)
   - Display last verification time
   - Show error messages if verification failed

2. **Credentials Form**
   - Input: Razorpay Key ID
   - Input: Razorpay Key Secret (password field)
   - Button: Test Connection
   - Button: Save & Activate Payments

3. **Help Section**
   - Guide: "Get API keys from Razorpay Dashboard → Settings → API Keys"
   - Link to Razorpay documentation

4. **Actions**
   - Deactivate Payments button
   - Re-verify button

**API Calls:**
```javascript
// Get current configuration
GET /owner/payment-settings

// Test connection
POST /owner/payment-settings/test-connection
Body: { keyId, keySecret }

// Save and activate
POST /owner/payment-settings/save-and-activate
Body: { keyId, keySecret }

// Deactivate
POST /owner/payment-settings/deactivate

// Check status
GET /owner/payment-settings/status
```

### MCP Monitoring Dashboard

**Route:** `/mcp/payment-monitoring`

**UI Components:**
1. **Statistics Cards**
   - Total Owners
   - Verified Owners
   - Active Owners
   - MCP Disabled Owners

2. **Owner List Table**
   - Columns: Owner Name, Email, Key ID (masked), Status, Last Verified, Actions
   - Filters: Status, Verification Status
   - Search: By owner name/email

3. **Actions per Owner**
   - Enable/Disable Payments (with reason)
   - Force Re-verification
   - View Details

**API Calls:**
```javascript
// Get all configurations
GET /mcp/payment-monitoring/configurations

// Get specific owner
GET /mcp/payment-monitoring/configurations/{ownerId}

// Override (enable/disable)
POST /mcp/payment-monitoring/configurations/{ownerId}/override
Body: { disabled: true, reason: "..." }

// Force re-verification
POST /mcp/payment-monitoring/configurations/{ownerId}/force-reverify

// Get statistics
GET /mcp/payment-monitoring/statistics
```

## Payment Flow

### Agreement Payment Flow

1. **Tenant clicks "Pay Now" on agreement**
2. **Frontend calls:** `POST /api/payments/create-order`
   ```json
   {
     "agreementId": "abc123",
     "amount": 8500,
     "currency": "INR"
   }
   ```
3. **Backend:**
   - Fetches agreement by ID
   - Identifies owner from `agreement.ownerId`
   - Checks if owner has payments enabled
   - Gets owner's decrypted Razorpay credentials
   - Creates `OwnerRazorpayGateway` with owner's credentials
   - Creates Razorpay order using owner's account
4. **Frontend:**
   - Receives order ID and key ID
   - Opens Razorpay checkout with owner's key
5. **After payment:**
   - Frontend calls: `POST /api/payments/verify-payment`
   ```json
   {
     "orderId": "order_xxx",
     "paymentId": "pay_xxx",
     "signature": "abc123...",
     "agreementId": "abc123"
   }
   ```
6. **Backend:**
   - Fetches agreement to identify owner
   - Gets owner's credentials
   - Verifies signature using owner's secret
   - Returns verification result

### Installment Payment Flow

Same as agreement flow, but:
- Uses `scheduleId` instead of `agreementId`
- Identifies owner via: Schedule → TenantPaymentPlan → Agreement → Owner
- Frontend must pass `scheduleId` in verification request

## Error Handling

### Payment Creation Errors

```java
// Owner not found
throw new NotFoundException("Agreement not found: " + agreementId);

// Owner has no Razorpay config
// Falls back to default gateway (logged as warning)

// Payments not enabled
throw new IllegalStateException(
  "Payments are currently unavailable. Please contact the hostel owner."
);

// Razorpay initialization failed
throw new RuntimeException("Failed to initialize payment gateway: " + e.getMessage());
```

### Verification Errors

```java
// Invalid credentials
verificationStatus = FAILED
verificationError = "Authentication failed"

// Network error
verificationStatus = FAILED
verificationError = "Connection timeout"

// MCP disabled
throw new IllegalStateException("Payments disabled by platform administrator");
```

## Testing

### Manual Testing Steps

#### 1. Owner Setup
```bash
# 1. Login as Owner
# 2. Navigate to /owner/payment-settings
# 3. Enter Razorpay credentials
# 4. Click "Test Connection"
# 5. Verify success message
# 6. Click "Save & Activate"
# 7. Verify payments are enabled
```

#### 2. Payment Processing
```bash
# 1. Login as Tenant
# 2. Navigate to agreement
# 3. Click "Pay Now"
# 4. Complete Razorpay checkout
# 5. Verify payment success
# 6. Check owner's Razorpay dashboard for payment
```

#### 3. MCP Control
```bash
# 1. Login as MCP
# 2. Navigate to /mcp/payment-monitoring
# 3. Find owner in list
# 4. Click "Disable Payments"
# 5. Enter reason
# 6. Verify owner cannot process payments
# 7. Re-enable payments
```

### API Testing with cURL

```bash
# Test connection
curl -X POST http://localhost:8080/owner/payment-settings/test-connection \
  -H "Authorization: Bearer <owner-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "rzp_test_xxxxx",
    "keySecret": "secret_xxxxx"
  }'

# Create payment order
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Authorization: Bearer <tenant-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "agreementId": "abc123",
    "amount": 8500,
    "currency": "INR"
  }'

# MCP override
curl -X POST http://localhost:8080/mcp/payment-monitoring/configurations/{ownerId}/override \
  -H "Authorization: Bearer <mcp-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "disabled": true,
    "reason": "Suspicious activity"
  }'
```

## Migration Guide

### Database Migration

```bash
# Run the migration script
psql -U postgres -d hostel_management -f database_migration_owner_razorpay.sql
```

### Existing Owners

For existing owners who were using the default Razorpay:

1. **Option 1: Continue with default**
   - No action needed
   - System falls back to default gateway
   - Payments continue to work

2. **Option 2: Migrate to own Razorpay**
   - Owner logs in
   - Navigates to payment settings
   - Enters their Razorpay credentials
   - Tests and activates
   - Future payments use their account

## Monitoring & Maintenance

### Health Checks

```sql
-- Check payment enablement status
SELECT 
    u.email,
    orc.razorpay_key_id,
    orc.verification_status,
    orc.is_active,
    orc.mcp_override_disabled,
    CASE 
        WHEN orc.is_active AND NOT orc.mcp_override_disabled 
             AND orc.verification_status = 'VERIFIED' 
        THEN 'ENABLED' 
        ELSE 'DISABLED' 
    END as payment_status
FROM users u
LEFT JOIN owner_razorpay_configs orc ON u.user_id = orc.owner_id
WHERE u.role = 'OWNER';
```

### Logs to Monitor

```
✅ OwnerRazorpayGateway initialized for owner: {ownerId}
✅ Razorpay credentials verified for owner: {ownerId}
✅ Razorpay payments activated for owner: {ownerId}
❌ Razorpay verification failed for owner {ownerId}: {error}
⚠️  Owner {ownerId} has no Razorpay configuration, using default gateway
```

## Future Enhancements

1. **Webhook Support**
   - Route webhooks to correct owner based on order metadata
   - Verify webhook signatures with owner-specific secrets

2. **Multi-Gateway Support**
   - Allow owners to choose between Razorpay, Stripe, PayPal
   - Abstract gateway selection logic

3. **Payment Analytics**
   - Track payment success rates per owner
   - Monitor failed transactions
   - Generate revenue reports

4. **Automated Re-verification**
   - Periodic credential validation
   - Alert owners of expiring credentials
   - Auto-disable on repeated failures

5. **Backup Payment Method**
   - Allow owners to configure secondary payment gateway
   - Automatic failover on primary failure

## Support & Troubleshooting

### Common Issues

**Issue:** "Payments are currently unavailable"
- **Cause:** Owner hasn't configured Razorpay or payments are disabled
- **Solution:** Owner should visit payment settings and configure credentials

**Issue:** "Failed to initialize payment gateway"
- **Cause:** Invalid credentials or Razorpay API error
- **Solution:** Owner should re-verify credentials

**Issue:** "Payment verification failed"
- **Cause:** Signature mismatch or wrong credentials used
- **Solution:** Ensure frontend passes correct scheduleId/agreementId

**Issue:** "MCP disabled payments"
- **Cause:** Platform administrator disabled payments for this owner
- **Solution:** Contact platform support

## Conclusion

The "Bring Your Own Razorpay" implementation provides:
- ✅ Multi-tenant payment processing
- ✅ Secure credential management
- ✅ Role-based access control
- ✅ Platform-level oversight
- ✅ Owner autonomy
- ✅ Scalable architecture

This implementation ensures payments go directly to each owner's Razorpay account while maintaining platform control and security.
