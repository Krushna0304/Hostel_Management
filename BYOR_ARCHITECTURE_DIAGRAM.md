# Bring Your Own Razorpay - Architecture Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         HOSTEL MANAGEMENT SYSTEM                         │
│                    "Bring Your Own Razorpay" Architecture                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND LAYER                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │
│  │  Owner Payment   │  │  MCP Monitoring  │  │  Tenant Payment  │     │
│  │  Settings Page   │  │    Dashboard     │  │   Checkout Page  │     │
│  │                  │  │                  │  │                  │     │
│  │  - Test Creds    │  │  - View All      │  │  - Pay Now       │     │
│  │  - Activate      │  │  - Override      │  │  - Razorpay UI   │     │
│  │  - Deactivate    │  │  - Statistics    │  │  - Verify        │     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘     │
│           │                     │                      │                │
└───────────┼─────────────────────┼──────────────────────┼────────────────┘
            │                     │                      │
            │ HTTPS               │ HTTPS                │ HTTPS
            ▼                     ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           CONTROLLER LAYER                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │
│  │OwnerRazorpay     │  │McpRazorpay       │  │Payment           │     │
│  │Controller        │  │Controller        │  │Controller        │     │
│  │                  │  │                  │  │                  │     │
│  │@PreAuthorize     │  │@PreAuthorize     │  │@PreAuthorize     │     │
│  │("OWNER")         │  │("MCP")           │  │("TENANT")        │     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘     │
│           │                     │                      │                │
└───────────┼─────────────────────┼──────────────────────┼────────────────┘
            │                     │                      │
            ▼                     ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            SERVICE LAYER                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │
│  │OwnerRazorpay     │  │McpRazorpay       │  │Payment           │     │
│  │Service           │  │Service           │  │Service           │     │
│  │                  │  │                  │  │                  │     │
│  │- testConnection  │  │- getAllConfigs   │  │- createOrder     │     │
│  │- saveActivate    │  │- mcpOverride     │  │- verifyPayment   │     │
│  │- getDecrypted    │  │- forceReverify   │  │- refund          │     │
│  └──────────────────┘  └──────────────────┘  └─────────┬────────┘     │
│           │                     │                       │               │
│           │                     │                       │               │
│           ▼                     ▼                       ▼               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │
│  │Encryption        │  │                  │  │OwnerRazorpay     │     │
│  │Service           │  │  (Statistics)    │  │Gateway           │     │
│  │                  │  │                  │  │                  │     │
│  │- encrypt()       │  │                  │  │- createOrder()   │     │
│  │- decrypt()       │  │                  │  │- verifyPayment() │     │
│  │AES-256-GCM       │  │                  │  │- refund()        │     │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘     │
│           │                                            │                │
└───────────┼────────────────────────────────────────────┼────────────────┘
            │                                            │
            ▼                                            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PERSISTENCE LAYER                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────┐      │
│  │              PostgreSQL Database                              │      │
│  │                                                               │      │
│  │  ┌─────────────────────────────────────────────────────┐    │      │
│  │  │  owner_razorpay_configs                             │    │      │
│  │  │  ─────────────────────────────────────────────────  │    │      │
│  │  │  - config_id (UUID, PK)                             │    │      │
│  │  │  - owner_id (UUID, FK → users)                      │    │      │
│  │  │  - razorpay_key_id (VARCHAR)                        │    │      │
│  │  │  - razorpay_key_secret_encrypted (VARCHAR) 🔒       │    │      │
│  │  │  - verification_status (ENUM)                       │    │      │
│  │  │  - is_active (BOOLEAN)                              │    │      │
│  │  │  - mcp_override_disabled (BOOLEAN)                  │    │      │
│  │  │  - mcp_override_reason (VARCHAR)                    │    │      │
│  │  │  - mcp_override_by (UUID, FK → users)               │    │      │
│  │  │  - created_at, updated_at (TIMESTAMP)               │    │      │
│  │  └─────────────────────────────────────────────────────┘    │      │
│  │                                                               │      │
│  │  ┌─────────────────────────────────────────────────────┐    │      │
│  │  │  agreements (MongoDB)                               │    │      │
│  │  │  ─────────────────────────────────────────────────  │    │      │
│  │  │  - id (String)                                      │    │      │
│  │  │  - ownerId (UUID) ← Used to identify owner          │    │      │
│  │  │  - userId (UUID)                                    │    │      │
│  │  │  - roomId (UUID)                                    │    │      │
│  │  │  - rent, deposit, etc.                              │    │      │
│  │  └─────────────────────────────────────────────────────┘    │      │
│  └──────────────────────────────────────────────────────────────┘      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL SERVICES                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────┐      │
│  │                    Razorpay API                               │      │
│  │                                                               │      │
│  │  Owner 1's Account: rzp_live_xxxxx1                          │      │
│  │  Owner 2's Account: rzp_live_xxxxx2                          │      │
│  │  Owner 3's Account: rzp_live_xxxxx3                          │      │
│  │  ...                                                          │      │
│  │                                                               │      │
│  │  Each owner receives payments directly to their account      │      │
│  └──────────────────────────────────────────────────────────────┘      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Payment Flow Sequence

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PAYMENT CREATION FLOW                                 │
└─────────────────────────────────────────────────────────────────────────┘

Tenant                Frontend              Backend                Database              Razorpay
  │                      │                      │                      │                      │
  │  1. Click "Pay Now"  │                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │                      │                      │                      │
  │                      │  2. POST /api/payments/create-order         │                      │
  │                      │     { agreementId, amount }                 │                      │
  │                      ├─────────────────────>│                      │                      │
  │                      │                      │                      │                      │
  │                      │                      │  3. Get Agreement    │                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │  (MongoDB)           │                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │                      │  { ownerId: xxx }    │                      │
  │                      │                      │                      │                      │
  │                      │                      │  4. Get Owner Config │                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │  (PostgreSQL)        │                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │                      │  { keyId, encrypted }│                      │
  │                      │                      │                      │                      │
  │                      │                      │  5. Decrypt Secret   │                      │
  │                      │                      │  (AES-256-GCM)       │                      │
  │                      │                      │                      │                      │
  │                      │                      │  6. Create Gateway   │                      │
  │                      │                      │  new OwnerRazorpay   │                      │
  │                      │                      │  Gateway(keyId, key) │                      │
  │                      │                      │                      │                      │
  │                      │                      │  7. Create Order     │                      │
  │                      │                      ├──────────────────────────────────────────>│
  │                      │                      │  POST /orders        │                      │
  │                      │                      │  (Owner's Account)   │                      │
  │                      │                      │<──────────────────────────────────────────┤
  │                      │                      │  { orderId, ... }    │                      │
  │                      │                      │                      │                      │
  │                      │  8. Return Order     │                      │                      │
  │                      │<─────────────────────┤                      │                      │
  │                      │  { orderId, keyId }  │                      │                      │
  │                      │                      │                      │                      │
  │  9. Open Razorpay UI │                      │                      │                      │
  │<─────────────────────┤                      │                      │                      │
  │                      │                      │                      │                      │
  │  10. Complete Payment│                      │                      │                      │
  ├──────────────────────────────────────────────────────────────────────────────────────>│
  │  (Razorpay Checkout) │                      │                      │                      │
  │<──────────────────────────────────────────────────────────────────────────────────────┤
  │  { paymentId, signature }                   │                      │                      │
  │                      │                      │                      │                      │
  │  11. Verify Payment  │                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │  12. POST /verify    │                      │                      │
  │                      ├─────────────────────>│                      │                      │
  │                      │                      │  13. Get Owner Config│                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │                      │  14. Verify Signature│                      │
  │                      │                      │  (HMAC-SHA256)       │                      │
  │                      │                      │                      │                      │
  │                      │  15. Return Success  │                      │                      │
  │                      │<─────────────────────┤                      │                      │
  │  16. Show Success    │                      │                      │                      │
  │<─────────────────────┤                      │                      │                      │
  │                      │                      │                      │                      │
```

## Owner Configuration Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  OWNER RAZORPAY SETUP FLOW                               │
└─────────────────────────────────────────────────────────────────────────┘

Owner                 Frontend              Backend                Database              Razorpay
  │                      │                      │                      │                      │
  │  1. Navigate to      │                      │                      │                      │
  │  Payment Settings    │                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │                      │                      │                      │
  │                      │  2. GET /owner/payment-settings             │                      │
  │                      ├─────────────────────>│                      │                      │
  │                      │                      │  3. Get Config       │                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │  4. Return Config    │  (Masked secrets)    │                      │
  │                      │<─────────────────────┤                      │                      │
  │                      │                      │                      │                      │
  │  5. Enter Credentials│                      │                      │                      │
  │  - Key ID            │                      │                      │                      │
  │  - Key Secret        │                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │                      │                      │                      │
  │  6. Click "Test"     │                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │  7. POST /test-connection                   │                      │
  │                      ├─────────────────────>│                      │                      │
  │                      │                      │  8. Encrypt Secret   │                      │
  │                      │                      │  (AES-256-GCM)       │                      │
  │                      │                      │                      │                      │
  │                      │                      │  9. Test API Call    │                      │
  │                      │                      ├──────────────────────────────────────────>│
  │                      │                      │  GET /orders?count=1 │                      │
  │                      │                      │<──────────────────────────────────────────┤
  │                      │                      │  { orders: [...] }   │                      │
  │                      │                      │                      │                      │
  │                      │                      │  10. Save Config     │                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │  status: VERIFIED    │                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │                      │                      │                      │
  │                      │  11. Return Success  │                      │                      │
  │                      │<─────────────────────┤                      │                      │
  │  12. Show Success    │                      │                      │                      │
  │<─────────────────────┤                      │                      │                      │
  │                      │                      │                      │                      │
  │  13. Click "Activate"│                      │                      │                      │
  ├─────────────────────>│                      │                      │                      │
  │                      │  14. POST /save-and-activate                │                      │
  │                      ├─────────────────────>│                      │                      │
  │                      │                      │  15. Update Config   │                      │
  │                      │                      ├─────────────────────>│                      │
  │                      │                      │  is_active: true     │                      │
  │                      │                      │<─────────────────────┤                      │
  │                      │                      │                      │                      │
  │                      │  16. Return Success  │                      │                      │
  │                      │<─────────────────────┤                      │                      │
  │  17. Show "Active"   │                      │                      │                      │
  │<─────────────────────┤                      │                      │                      │
  │                      │                      │                      │                      │
```

## MCP Override Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     MCP OVERRIDE FLOW                                    │
└─────────────────────────────────────────────────────────────────────────┘

MCP                   Frontend              Backend                Database
  │                      │                      │                      │
  │  1. Navigate to      │                      │                      │
  │  Monitoring Dashboard│                      │                      │
  ├─────────────────────>│                      │                      │
  │                      │                      │                      │
  │                      │  2. GET /mcp/payment-monitoring/configurations
  │                      ├─────────────────────>│                      │
  │                      │                      │  3. Get All Configs  │
  │                      │                      ├─────────────────────>│
  │                      │                      │<─────────────────────┤
  │                      │  4. Return List      │                      │
  │                      │<─────────────────────┤                      │
  │                      │                      │                      │
  │  5. View Owner List  │                      │                      │
  │<─────────────────────┤                      │                      │
  │                      │                      │                      │
  │  6. Click "Disable"  │                      │                      │
  │  for Owner X         │                      │                      │
  ├─────────────────────>│                      │                      │
  │                      │                      │                      │
  │  7. Enter Reason     │                      │                      │
  │  "Suspicious activity"│                     │                      │
  ├─────────────────────>│                      │                      │
  │                      │                      │                      │
  │  8. Confirm          │                      │                      │
  ├─────────────────────>│                      │                      │
  │                      │  9. POST /configurations/{ownerId}/override │
  │                      ├─────────────────────>│                      │
  │                      │  { disabled: true,   │                      │
  │                      │    reason: "..." }   │                      │
  │                      │                      │  10. Update Config   │
  │                      │                      ├─────────────────────>│
  │                      │                      │  mcp_override: true  │
  │                      │                      │  mcp_by: MCP_ID      │
  │                      │                      │  mcp_reason: "..."   │
  │                      │                      │<─────────────────────┤
  │                      │                      │                      │
  │                      │  11. Return Success  │                      │
  │                      │<─────────────────────┤                      │
  │  12. Show "Disabled" │                      │                      │
  │<─────────────────────┤                      │                      │
  │                      │                      │                      │
  │                      │                      │                      │
  │  Now when tenant tries to pay:              │                      │
  │                      │                      │                      │
  │                      │  POST /create-order  │                      │
  │                      ├─────────────────────>│                      │
  │                      │                      │  Check if enabled    │
  │                      │                      ├─────────────────────>│
  │                      │                      │  mcp_override: true  │
  │                      │                      │<─────────────────────┤
  │                      │                      │                      │
  │                      │  ERROR: Payments     │                      │
  │                      │  unavailable         │                      │
  │                      │<─────────────────────┤                      │
  │                      │                      │                      │
```

## Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        SECURITY LAYERS                                   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 1: Transport Security                                             │
│  ─────────────────────────────────────────────────────────────────────  │
│  ✅ HTTPS/TLS for all API calls                                         │
│  ✅ Certificate validation                                               │
│  ✅ Secure headers (HSTS, CSP, etc.)                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 2: Authentication & Authorization                                 │
│  ─────────────────────────────────────────────────────────────────────  │
│  ✅ JWT token validation                                                 │
│  ✅ Role-based access control (@PreAuthorize)                            │
│  ✅ Owner can only access their own config                               │
│  ✅ MCP can view all but not secrets                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 3: Data Encryption                                                │
│  ─────────────────────────────────────────────────────────────────────  │
│  ✅ AES-256-GCM for secrets at rest                                      │
│  ✅ Unique IV per encryption                                             │
│  ✅ Authenticated encryption (prevents tampering)                        │
│  ✅ Key stored in environment variable                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 4: API Response Filtering                                         │
│  ─────────────────────────────────────────────────────────────────────  │
│  ✅ Secrets never in API responses                                       │
│  ✅ Key ID masking (rzp_test...xxxx)                                     │
│  ✅ Only status and metadata exposed                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  Layer 5: Audit & Monitoring                                             │
│  ─────────────────────────────────────────────────────────────────────  │
│  ✅ All MCP actions logged                                               │
│  ✅ Verification attempts tracked                                        │
│  ✅ Payment enablement changes recorded                                  │
│  ✅ Timestamps for all operations                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CREDENTIAL LIFECYCLE                                  │
└─────────────────────────────────────────────────────────────────────────┘

1. ENTRY
   Owner enters credentials
   ↓
   Frontend (HTTPS)
   ↓
   Backend receives plaintext
   
2. ENCRYPTION
   EncryptionService.encrypt()
   ↓
   AES-256-GCM
   ↓
   Base64(IV + Ciphertext + Tag)
   
3. STORAGE
   PostgreSQL
   ↓
   razorpay_key_secret_encrypted
   ↓
   Encrypted blob stored
   
4. RETRIEVAL (Internal Only)
   PaymentService needs credentials
   ↓
   OwnerRazorpayService.getDecryptedCredentials()
   ↓
   EncryptionService.decrypt()
   ↓
   Plaintext in memory (temporary)
   
5. USAGE
   Create OwnerRazorpayGateway
   ↓
   Call Razorpay API
   ↓
   Gateway disposed
   ↓
   Credentials cleared from memory
   
6. API RESPONSE (Never Contains Secrets)
   {
     "razorpayKeyId": "rzp_test_xxxxx",
     "maskedKeyId": "rzp_test...xxxx",
     "verificationStatus": "VERIFIED",
     "isActive": true
   }
```

---

**Legend:**
- 🔒 = Encrypted
- ✅ = Security measure
- → = Data flow
- ↓ = Process flow
