# Bring Your Own Razorpay - Quick Reference Card

## 🚀 Quick Setup (5 Minutes)

### 1. Generate Encryption Key
```bash
openssl rand -base64 32
```

### 2. Set Environment Variable
```bash
export RAZORPAY_ENCRYPTION_KEY="<your-generated-key>"
```

### 3. Run Database Migration
```bash
psql -U postgres -d hostel_management -f database_migration_owner_razorpay.sql
```

### 4. Start Application
```bash
# Backend
cd HostelManagment_Backend
./mvnw spring-boot:run

# Frontend
cd HostelManagement_Frontend
npm install
npm run dev
```

---

## 📍 Routes

### Owner
- **Payment Settings:** `/owner/payment-settings`

### MCP
- **Payment Monitoring:** `/mcp/payment-monitoring`

---

## 🔌 API Endpoints

### Owner Endpoints
```
GET    /owner/payment-settings
POST   /owner/payment-settings/test-connection
POST   /owner/payment-settings/save-and-activate
POST   /owner/payment-settings/deactivate
GET    /owner/payment-settings/status
```

### MCP Endpoints
```
GET    /mcp/payment-monitoring/configurations
GET    /mcp/payment-monitoring/configurations/{ownerId}
POST   /mcp/payment-monitoring/configurations/{ownerId}/override
POST   /mcp/payment-monitoring/configurations/{ownerId}/force-reverify
GET    /mcp/payment-monitoring/statistics
```

---

## 💾 Database

### Table
```sql
owner_razorpay_configs
├── config_id (UUID, PK)
├── owner_id (UUID, FK → users)
├── razorpay_key_id (VARCHAR)
├── razorpay_key_secret_encrypted (VARCHAR) 🔒
├── verification_status (ENUM)
├── is_active (BOOLEAN)
├── mcp_override_disabled (BOOLEAN)
└── timestamps
```

### Payment Enabled Logic
```sql
is_active = TRUE
AND mcp_override_disabled = FALSE
AND verification_status = 'VERIFIED'
```

---

## 🎨 Status Badges

| Status | Color | Meaning |
|--------|-------|---------|
| ✓ Active | Green | Payments enabled |
| ⚠ Disabled by Platform | Red | MCP disabled |
| ⏸ Inactive | Yellow | Verified but not active |
| ✗ Verification Failed | Red | Invalid credentials |
| ○ Not Connected | Gray | No configuration |

---

## 🔐 Security

### Encryption
- **Algorithm:** AES-256-GCM
- **Key Storage:** Environment variable
- **Secret Storage:** Encrypted in database
- **API Exposure:** Never exposed

### Access Control
- **Owner:** Full control of their credentials
- **MCP:** Monitor and control, cannot view secrets
- **Others:** No access

---

## 🧪 Quick Test

### Test Owner Flow
```bash
# 1. Test connection
curl -X POST http://localhost:8080/owner/payment-settings/test-connection \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"keyId": "rzp_test_xxxxx", "keySecret": "secret"}'

# 2. Save and activate
curl -X POST http://localhost:8080/owner/payment-settings/save-and-activate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"keyId": "rzp_test_xxxxx", "keySecret": "secret"}'
```

### Test MCP Flow
```bash
# 1. Get statistics
curl -X GET http://localhost:8080/mcp/payment-monitoring/statistics \
  -H "Authorization: Bearer <token>"

# 2. Disable owner
curl -X POST http://localhost:8080/mcp/payment-monitoring/configurations/{ownerId}/override \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"disabled": true, "reason": "Test"}'
```

---

## 📊 Files Created

### Backend (11 files)
```
✅ OwnerRazorpayConfig.java
✅ OwnerRazorpayConfigRepository.java
✅ EncryptionService.java
✅ OwnerRazorpayService.java
✅ McpRazorpayService.java
✅ OwnerRazorpayGateway.java
✅ OwnerRazorpayController.java
✅ McpRazorpayController.java
✅ RazorpayConfigRequest.java
✅ RazorpayConfigResponse.java
✅ McpOverrideRequest.java
```

### Frontend (4 files)
```
✅ paymentSettingsService.js
✅ mcpPaymentService.js
✅ PaymentSettings.jsx
✅ PaymentMonitoring.jsx
```

### Documentation (9 files)
```
✅ BRING_YOUR_OWN_RAZORPAY_IMPLEMENTATION.md
✅ BYOR_ARCHITECTURE_DIAGRAM.md
✅ BYOR_FRONTEND_IMPLEMENTATION.md
✅ BYOR_QUICK_START.md
✅ BYOR_IMPLEMENTATION_CHECKLIST.md
✅ BYOR_SUMMARY.md
✅ BYOR_README.md
✅ BYOR_COMPLETE_IMPLEMENTATION_SUMMARY.md
✅ BYOR_QUICK_REFERENCE.md
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Encryption key not configured | Set `RAZORPAY_ENCRYPTION_KEY` |
| Authentication failed | Verify Razorpay credentials |
| Payments unavailable | Check owner config & MCP status |
| Verification failed | Ensure correct credentials |

---

## 📖 Documentation

| Document | Purpose |
|----------|---------|
| `BYOR_README.md` | Feature overview |
| `BYOR_QUICK_START.md` | 5-minute setup |
| `BRING_YOUR_OWN_RAZORPAY_IMPLEMENTATION.md` | Complete technical guide |
| `BYOR_FRONTEND_IMPLEMENTATION.md` | Frontend guide |
| `BYOR_COMPLETE_IMPLEMENTATION_SUMMARY.md` | Full summary |
| `BYOR_QUICK_REFERENCE.md` | This card |

---

## ✅ Deployment Checklist

- [ ] Generate encryption key
- [ ] Set environment variable
- [ ] Run database migration
- [ ] Deploy backend
- [ ] Deploy frontend
- [ ] Test owner flow
- [ ] Test MCP flow
- [ ] Test payment flow
- [ ] Monitor logs

---

## 📞 Support

1. Check documentation
2. Review logs
3. Test API endpoints
4. Check database state
5. Contact development team

---

**Version:** 1.0.0
**Status:** ✅ Production Ready
**Last Updated:** April 27, 2026
