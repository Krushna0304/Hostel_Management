# Agreement Settlement Module Documentation

## Overview

The Agreement Settlement Module provides a comprehensive system for handling the termination and financial settlement of hostel agreements between owners and tenants. It automates the calculation of outstanding amounts, manages additional charges, and facilitates the final payment process.

## Features

### 1. Settlement Request Initiation
- **Tenant-initiated**: Tenants can request settlement for their active agreements
- **Validation**: System validates agreement status and prevents duplicate requests
- **Notes**: Tenants can add notes explaining their settlement request

### 2. Automated Financial Calculation
- **Outstanding Rent**: Automatically calculates overdue rent installments
- **Outstanding Charges**: Includes pending other charges (maintenance, utilities, etc.)
- **Security Deposit**: Factors in the refundable security deposit
- **Real-time Updates**: Calculations update as owner adds additional charges

### 3. Owner Review and Approval
- **Manual Charges**: Owners can add damage charges, cleaning fees, and other deductions
- **Detailed Breakdown**: Complete financial summary with itemized charges
- **Approval/Rejection**: Owners can approve or reject settlement requests with notes

### 4. Payment Processing
- **Bidirectional Payments**: Handles both tenant payments and owner refunds
- **Payment Tracking**: Records payment references and completion timestamps
- **Status Updates**: Real-time status updates throughout the process

### 5. Comprehensive Audit Trail
- **Status Tracking**: Complete history of settlement progress
- **Notifications**: Email notifications for all parties at each stage
- **Documentation**: Detailed records of all charges, payments, and notes

## Functional Flow

### Step 1: Settlement Request Initiation
```
Tenant Dashboard → Request Settlement → Add Notes → Submit Request
```
- Tenant selects active agreement
- System validates eligibility
- Request created with PENDING_OWNER_REVIEW status

### Step 2: Owner Review
```
Owner Dashboard → Review Request → Calculate Charges → Approve/Reject
```
- Owner sees pending settlement requests
- System displays automated calculations
- Owner adds manual charges (damage, cleaning, etc.)
- Final settlement amount determined

### Step 3: Payment Processing
```
Settlement Approved → Payment Required → Payment Completion → Agreement Closure
```

**Case A: Tenant Payable (Outstanding > Security Deposit)**
- Tenant receives payment request
- Tenant makes payment and confirms
- Settlement marked as completed

**Case B: Owner Payable (Security Deposit > Outstanding)**
- Owner processes refund to tenant
- Owner confirms payment completion
- Settlement marked as completed

### Step 4: Agreement Closure
- Agreement status updated to SETTLED
- No further transactions allowed
- Settlement summary archived

## API Endpoints

### Settlement Management
```
POST   /api/settlements/request           # Initiate settlement
GET    /api/settlements/{id}/calculate    # Get settlement calculation
POST   /api/settlements/{id}/approve      # Approve/reject settlement
POST   /api/settlements/{id}/complete     # Mark settlement complete
GET    /api/settlements/owner             # Get owner settlements
GET    /api/settlements/tenant            # Get tenant settlements
```

### Supporting Endpoints
```
GET    /tenant/agreements                 # Get tenant agreements for settlement
```

## Database Schema

### settlement_requests Table
```sql
- settlement_id (UUID, Primary Key)
- agreement_id (String, Foreign Key to MongoDB)
- tenant_id (UUID, Foreign Key to users)
- owner_id (UUID, Foreign Key to users)
- room_id (UUID, Foreign Key to rooms)
- status (Enum: PENDING_OWNER_REVIEW, PENDING_TENANT_PAYMENT, etc.)
- security_deposit (Decimal)
- outstanding_rent (Decimal)
- outstanding_charges (Decimal)
- damage_charges (Decimal)
- cleaning_charges (Decimal)
- other_deductions (Decimal)
- total_deductions (Decimal)
- final_settlement_amount (Decimal)
- settlement_type (String: TENANT_PAYABLE/OWNER_PAYABLE)
- tenant_notes (Text)
- owner_notes (Text)
- damage_description (Text)
- payment_reference (String)
- created_at (Timestamp)
- updated_at (Timestamp)
- settled_at (Timestamp)
```

## Settlement Status Flow

```
PENDING_OWNER_REVIEW
    ↓ (Owner approves)
PENDING_TENANT_PAYMENT (if tenant owes money)
    ↓ (Tenant pays)
COMPLETED

OR

PENDING_OWNER_REVIEW
    ↓ (Owner approves)
PENDING_OWNER_PAYMENT (if owner owes refund)
    ↓ (Owner pays)
COMPLETED

OR

PENDING_OWNER_REVIEW
    ↓ (Owner rejects)
REJECTED
```

## Frontend Components

### Core Components
- **SettlementRequestModal**: Tenant settlement initiation
- **SettlementCalculationModal**: Owner review and approval
- **SettlementStatusBadge**: Status display component
- **SettlementSummary**: Reusable settlement display

### Pages
- **Owner/Settlements**: Settlement management dashboard
- **Tenant/Settlements**: Settlement request and tracking
- **Integration**: Settlement buttons in tenant dashboard

## Business Rules

### Settlement Eligibility
- Only ACTIVE agreements can request settlement
- One active settlement request per agreement
- Tenant must be the agreement holder

### Financial Calculations
- Security deposit is always refundable base amount
- Outstanding amounts are automatically calculated
- Manual charges require owner approval
- Final amount determines payment direction

### Payment Rules
- Tenant payments required when total dues > security deposit
- Owner refunds required when security deposit > total dues
- Payment references required for completion
- All payments must be confirmed by payer

## Security Considerations

### Authorization
- Tenants can only request settlement for their own agreements
- Owners can only process settlements for their properties
- Settlement calculations verified server-side

### Data Validation
- All financial amounts validated for non-negative values
- Settlement status transitions enforced
- Duplicate settlement prevention

### Audit Trail
- All actions logged with timestamps
- Payment references recorded
- Status change history maintained

## Integration Points

### Existing Systems
- **Agreement Management**: Links to active agreements
- **Payment System**: Integrates with existing payment processing
- **Notification System**: Email notifications for all events
- **User Management**: Role-based access control

### External Dependencies
- **Email Service**: Settlement notifications
- **Payment Gateway**: Optional integration for automated payments
- **SMS Service**: Optional SMS notifications

## Usage Examples

### Tenant Requesting Settlement
```javascript
// Tenant initiates settlement
const settlement = await settlementService.initiateSettlement({
  agreementId: "agreement-123",
  tenantNotes: "Moving out due to job relocation"
});
```

### Owner Processing Settlement
```javascript
// Owner approves with additional charges
const approval = await settlementService.approveSettlement(settlementId, {
  damageCharges: 1000,
  cleaningCharges: 500,
  ownerNotes: "Minor wall damage, standard cleaning",
  approved: true
});
```

### Completing Settlement
```javascript
// Mark settlement as completed
const completion = await settlementService.completeSettlement(
  settlementId, 
  "TXN123456789"
);
```

## Error Handling

### Common Errors
- **ConflictException**: Duplicate settlement requests
- **NotFoundException**: Agreement or settlement not found
- **ValidationException**: Invalid financial amounts
- **AuthorizationException**: Insufficient permissions

### Error Responses
```json
{
  "error": "SETTLEMENT_ALREADY_EXISTS",
  "message": "Settlement request already exists for this agreement",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Testing

### Unit Tests
- Settlement calculation logic
- Status transition validation
- Authorization checks
- Financial amount validation

### Integration Tests
- End-to-end settlement flow
- Payment processing integration
- Notification delivery
- Database consistency

### Manual Testing Scenarios
1. **Happy Path**: Complete settlement with tenant payment
2. **Refund Scenario**: Complete settlement with owner refund
3. **Rejection Flow**: Owner rejects settlement request
4. **Edge Cases**: Zero amounts, maximum values, concurrent requests

## Deployment Notes

### Database Migration
```sql
-- Run the settlement migration script
\i database_migration_settlement.sql
```

### Configuration
- Email notifications can be enabled/disabled via application properties
- Payment gateway integration is optional
- SMS notifications require SMS service configuration

### Monitoring
- Monitor settlement completion rates
- Track average settlement processing time
- Alert on failed payment confirmations
- Monitor for stuck settlements

## Future Enhancements

### Planned Features
- **Automated Payments**: Direct integration with payment gateways
- **Dispute Resolution**: Formal dispute process for rejected settlements
- **Bulk Processing**: Handle multiple settlements simultaneously
- **Advanced Reporting**: Settlement analytics and reporting
- **Mobile App**: Native mobile app support

### Potential Improvements
- **AI-powered Damage Assessment**: Automated damage charge calculation
- **Integration with Property Management**: Link to external property systems
- **Multi-currency Support**: Handle different currencies
- **Installment Settlements**: Allow settlement amount to be paid in installments

## Support and Maintenance

### Common Issues
- **Calculation Discrepancies**: Verify outstanding amount calculations
- **Status Stuck**: Check for failed notification deliveries
- **Payment Confirmation**: Ensure payment references are properly recorded

### Maintenance Tasks
- **Regular Cleanup**: Archive completed settlements older than 1 year
- **Performance Monitoring**: Monitor settlement calculation performance
- **Data Integrity**: Periodic validation of settlement amounts

### Contact Information
For technical support or questions about the Settlement Module, contact the development team or refer to the main application documentation.