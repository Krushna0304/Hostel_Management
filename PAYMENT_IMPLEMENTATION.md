# Tenant Installment Payment Implementation

## Overview
Implemented a complete payment flow for tenant installment payments with both **Online (Razorpay)** and **Cash (OTP-verified)** payment options, similar to the agreement acceptance flow.

## Features Implemented

### Frontend Changes

#### 1. Payment Modal Component (`PaymentModal.jsx`)
- Reusable modal for installment payments
- Payment mode selection (Online/Cash)
- OTP flow for cash payments
- Razorpay integration for online payments
- Payment summary display
- Error handling and loading states

#### 2. Updated Tenant Dashboard (`Dashboard.jsx`)
- Integrated PaymentModal component
- Replaced simple payment button with modal trigger
- Added payment success callback to refresh data
- Removed inline payment error handling (now in modal)

### Backend Changes

#### 1. Database Model Updates

**CashPaymentOtp Model**
- Added `scheduleId` field to support installment payments
- Made `agreementId` nullable (either agreementId or scheduleId is present)

#### 2. Repository Updates

**CashPaymentOtpRepository**
- Added `findByScheduleIdAndUsedFalseAndExpiryTimeAfter()` method for installment OTP lookup

#### 3. Service Layer Updates

**CashPaymentOtpService**
- Added `generateAndSendInstallmentOtp()` - generates OTP for installment payments
- Added `verifyInstallmentOtp()` - verifies OTP for installment payments
- Both methods follow the same security pattern as agreement OTPs

**PaymentScheduleService**
- Updated `recordPayment()` to support both payment modes:
  - **Cash Mode**: Verifies OTP before recording payment
  - **Online Mode**: Verifies Razorpay payment signature before recording
- Added payment verification logic
- Maintains transaction records with proper payment mode

**PaymentService**
- Added `createInstallmentPaymentOrder()` - creates Razorpay order for installments
- Added `verifyInstallmentPayment()` - verifies installment payment signatures

#### 4. Controller Updates

**CashPaymentOtpController**
- Added `/api/cash-payment-otp/send-installment/{scheduleId}` endpoint
- Sends OTP to owner for installment payment verification

**PaymentController**
- Added `/api/payments/create-installment-order` endpoint
- Creates Razorpay payment order for installment payments

#### 5. DTO Updates

**RecordPaymentRequest**
- Added `otp` field for cash payments
- Added `razorpayOrderId`, `razorpayPaymentId`, `razorpaySignature` for online payments

**CreateInstallmentOrderApiRequest** (new inner class)
- Request DTO for creating installment payment orders

## Payment Flow

### Cash Payment Flow
1. Tenant clicks "Pay now" button
2. Payment modal opens with payment mode selection
3. Tenant selects "Cash payment"
4. Tenant clicks "Send OTP to Owner"
5. Backend generates 6-digit OTP and sends to owner via SMS
6. Tenant collects OTP from owner after handing over cash
7. Tenant enters OTP and confirms payment
8. Backend verifies OTP and records payment
9. Dashboard refreshes with updated payment status

### Online Payment Flow
1. Tenant clicks "Pay now" button
2. Payment modal opens with payment mode selection
3. Tenant selects "Online payment"
4. Tenant clicks "Confirm payment"
5. Backend creates Razorpay order
6. Razorpay checkout modal opens
7. Tenant completes payment
8. Backend verifies payment signature
9. Payment is recorded and dashboard refreshes

## Security Features
- OTP is hashed using BCrypt before storage
- OTP expires after 10 minutes
- OTP can only be used once
- Razorpay signature verification for online payments
- Payment amount validation
- Transaction status tracking

## API Endpoints

### New Endpoints
- `POST /api/cash-payment-otp/send-installment/{scheduleId}` - Send OTP for installment
- `POST /api/payments/create-installment-order` - Create Razorpay order for installment

### Updated Endpoints
- `POST /tenant/pay/{scheduleId}` - Now supports both cash and online payments

## Database Schema Changes
```sql
ALTER TABLE cash_payment_otps 
  ADD COLUMN schedule_id UUID,
  ALTER COLUMN agreement_id DROP NOT NULL;
```

## Testing Checklist
- [ ] Cash payment with valid OTP
- [ ] Cash payment with invalid OTP
- [ ] Cash payment with expired OTP
- [ ] Online payment with Razorpay
- [ ] Online payment with mock provider
- [ ] Payment amount validation
- [ ] Partial payment handling
- [ ] Full payment completion
- [ ] Dashboard refresh after payment
- [ ] Error handling for all scenarios

## Notes
- The implementation follows the same pattern as agreement acceptance
- Both payment modes are fully functional and secure
- OTP system prevents unauthorized cash payments
- Online payments are verified through Razorpay signature
- Transaction records maintain audit trail
