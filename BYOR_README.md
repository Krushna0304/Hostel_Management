# Bring Your Own Razorpay (BYOR) 🚀

## Overview

The "Bring Your Own Razorpay" feature allows each hostel owner to connect their own Razorpay account to the platform. This ensures that payments go directly to the owner's Razorpay account while maintaining platform-level control and security.

## 🎯 Key Benefits

### For Owners
- ✅ Payments go directly to their Razorpay account
- ✅ Full control over their payment credentials
- ✅ Can activate/deactivate payments anytime
- ✅ No revenue sharing with platform
- ✅ Access to their own Razorpay dashboard

### For Platform (MCP)
- ✅ Monitor all owner payment configurations
- ✅ Enable/disable payments per owner
- ✅ Force re-verification when needed
- ✅ Audit trail for compliance
- ✅ No liability for payment processing

### For Tenants
- ✅ Seamless payment experience
- ✅ Secure payment processing
- ✅ Multiple payment methods (via Razorpay)
- ✅ Instant payment confirmation

## 🏗️ Architecture

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

## 🔐 Security

### Encryption
- **Algorithm:** AES-256-GCM (Galois/Counter Mode)
- **Key Storage:** Environment variable or secure vault
- **Secret Storage:** Encrypted in database
- **API Exposure:** Secrets never exposed in responses

### Access Control
- **Owner:** Can enter, test, and activate credentials
- **MCP:** Can monitor and control but cannot view secrets
- **Others:** No access to payment settings

### Verification
- Credentials tested via Razorpay API before activation
- Failed verifications prevent payment processing
- MCP can force re-verification

## 📁 Project Structure

```
HostelManagment_Backend/
├── src/main/java/com/krunity/HostelManagment/
│   ├── model/
│   │   └── OwnerRazorpayConfig.java          # Credential storage model
│   ├── repository/
│   │   └── OwnerRazorpayConfigRepository.java # Database access
│   ├── service/
│   │   ├── EncryptionService.java             # AES-256-GCM encryption
│   │   ├── OwnerRazorpayService.java          # Owner credential management
│   │   ├── McpRazorpayService.java            # MCP monitoring
│   │   ├── OwnerRazorpayGateway.java          # Dynamic payment gateway
│   │   └── PaymentService.java                # Updated payment logic
│   ├── controller/
│   │   ├── OwnerRazorpayController.java       # Owner endpoints
│   │   └── McpRazorpayController.java         # MCP endpoints
│   └── dto/
│       ├── RazorpayConfigRequest.java         # Credential input
│       ├── RazorpayConfigResponse.java        # Configuration output
│       └── McpOverrideRequest.java            # MCP override input
└── src/main/resources/
    └── application.yml                         # Configuration

Documentation/
├── BRING_YOUR_OWN_RAZORPAY_IMPLEMENTATION.md  # Complete guide
├── BYOR_IMPLEMENTATION_CHECKLIST.md           # Task checklist
├── BYOR_QUICK_START.md                        # Quick setup guide
├── BYOR_SUMMARY.md                            # Implementation summary
├── BYOR_README.md                             # This file
└── database_migration_owner_razorpay.sql      # Database schema
```

## 🚀 Quick Start

### 1. Generate Encryption Key

```bash
openssl rand -base64 32
```

### 2. Configure Application

Set environment variable:

```bash
export RAZORPAY_ENCRYPTION_KEY="<your-generated-key>"
```

Or update `application.yml`:

```yaml
payment:
  razorpay:
    encryption:
      key: ${RAZORPAY_ENCRYPTION_KEY:<your-key>}
```

### 3. Run Database Migration

```bash
psql -U postgres -d hostel_management -f database_migration_owner_razorpay.sql
```

### 4. Start Application

```bash
cd HostelManagment_Backend
./mvnw spring-boot:run
```

### 5. Test API

```bash
# Test connection (as Owner)
curl -X POST http://localhost:8080/owner/payment-settings/test-connection \
  -H "Authorization: Bearer <owner-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "keyId": "rzp_test_xxxxx",
    "keySecret": "your_secret"
  }'
```

## 📚 Documentation

### For Developers
- **[Complete Implementation Guide](./BRING_YOUR_OWN_RAZORPAY_IMPLEMENTATION.md)** - Technical details, architecture, API docs
- **[Quick Start Guide](./BYOR_QUICK_START.md)** - Setup and testing in 5 minutes
- **[Implementation Checklist](./BYOR_IMPLEMENTATION_CHECKLIST.md)** - Task tracking

### For Database Admins
- **[Database Migration Script](./database_migration_owner_razorpay.sql)** - Schema and indexes

### For Project Managers
- **[Implementation Summary](./BYOR_SUMMARY.md)** - Status and next steps

## 🔌 API Endpoints

### Owner Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/owner/payment-settings` | Get current configuration |
| POST | `/owner/payment-settings/test-connection` | Test Razorpay credentials |
| POST | `/owner/payment-settings/save-and-activate` | Save and activate payments |
| POST | `/owner/payment-settings/deactivate` | Deactivate payments |
| GET | `/owner/payment-settings/status` | Check if payments enabled |

### MCP Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/mcp/payment-monitoring/configurations` | List all owner configs |
| GET | `/mcp/payment-monitoring/configurations/{ownerId}` | Get specific owner config |
| POST | `/mcp/payment-monitoring/configurations/{ownerId}/override` | Enable/disable payments |
| POST | `/mcp/payment-monitoring/configurations/{ownerId}/force-reverify` | Force re-verification |
| GET | `/mcp/payment-monitoring/statistics` | Get dashboard statistics |

## 🧪 Testing

### Manual Testing

```bash
# 1. Test connection
curl -X POST http://localhost:8080/owner/payment-settings/test-connection \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"keyId": "rzp_test_xxx", "keySecret": "secret"}'

# 2. Save and activate
curl -X POST http://localhost:8080/owner/payment-settings/save-and-activate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"keyId": "rzp_test_xxx", "keySecret": "secret"}'

# 3. Create payment order
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"agreementId": "abc123", "amount": 8500, "currency": "INR"}'
```

### Database Queries

```sql
-- Check owner payment status
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

## 🐛 Troubleshooting

### Issue: "Encryption key not configured"
**Solution:** Set `RAZORPAY_ENCRYPTION_KEY` environment variable

### Issue: "Authentication failed" during test
**Solution:** Verify Razorpay credentials are correct and in test mode

### Issue: "Payments are currently unavailable"
**Solution:** 
- Check if owner has configured Razorpay
- Verify payments are active
- Check if MCP has disabled payments

### Issue: "Payment verification failed"
**Solution:** Ensure correct owner credentials are used and scheduleId is passed

## 📊 Monitoring

### Logs to Watch

```
✅ OwnerRazorpayGateway initialized for owner: {ownerId}
✅ Razorpay credentials verified for owner: {ownerId}
✅ Razorpay payments activated for owner: {ownerId}
❌ Razorpay verification failed for owner {ownerId}: {error}
⚠️  Owner {ownerId} has no Razorpay configuration, using default gateway
```

### Metrics to Track

- Number of owners with configured Razorpay
- Payment success rate per owner
- Failed verification attempts
- MCP override actions
- Payment processing time

## 🎯 Current Status

| Component | Status | Progress |
|-----------|--------|----------|
| Backend | ✅ Complete | 100% |
| Database | ✅ Complete | 100% |
| Documentation | ✅ Complete | 100% |
| Frontend (Owner) | ⏳ Pending | 0% |
| Frontend (MCP) | ⏳ Pending | 0% |
| Testing | ⏳ Pending | 0% |
| Deployment | ⏳ Pending | 0% |

## 🔮 Future Enhancements

1. **Multi-Gateway Support**
   - Support for Stripe, PayPal, etc.
   - Owner can choose preferred gateway

2. **Automated Re-verification**
   - Periodic credential validation
   - Alert owners of expiring credentials

3. **Payment Analytics**
   - Revenue reports per owner
   - Success rate tracking
   - Failed transaction analysis

4. **Webhook Routing**
   - Route webhooks to correct owner
   - Verify webhook signatures per owner

5. **Backup Payment Method**
   - Secondary gateway configuration
   - Automatic failover

## 🤝 Contributing

### Adding New Payment Gateway

1. Implement `PaymentGateway` interface
2. Create gateway-specific service
3. Update `PaymentService` to support new gateway
4. Add configuration in `application.yml`

### Adding New Features

1. Follow existing architecture patterns
2. Maintain security standards
3. Update documentation
4. Add tests

## 📞 Support

For issues or questions:
1. Check troubleshooting section
2. Review application logs
3. Check database state
4. Refer to complete implementation guide

## 📄 License

This feature is part of the Hostel Management System.

---

**Version:** 1.0.0  
**Last Updated:** April 27, 2026  
**Status:** Backend Complete, Frontend Pending  
**Maintainer:** Development Team

## 🎉 Acknowledgments

This implementation follows industry best practices for:
- Multi-tenant SaaS architecture
- Secure credential management
- Payment gateway integration
- Role-based access control

Built with ❤️ for hostel owners and platform administrators.
