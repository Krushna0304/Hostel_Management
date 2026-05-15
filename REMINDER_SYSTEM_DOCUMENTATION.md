# Payment Reminder System with Subscription Tiers

## Overview
A comprehensive reminder system that sends automated SMS/Email notifications to tenants about upcoming and overdue payments. The system supports multiple subscription tiers with different feature sets and allows owners to customize SMS templates.

## Subscription Tiers

### FREE Tier
- **Features:**
  - Email reminders only
  - Default templates only
  - Max 1 hostel
  - Max 10 tenants
- **Cost:** Free forever
- **Best for:** Individual property owners testing the system

### BETA Tier
- **Features:**
  - SMS + Email reminders
  - Default templates only
  - Max 3 hostels
  - Max 50 tenants
  - 6 months validity
- **Cost:** Free during beta period
- **Best for:** Early adopters and small hostel chains

### PRO Tier
- **Features:**
  - SMS + Email reminders
  - Custom SMS templates
  - Max 10 hostels
  - Max 200 tenants
  - Unlimited validity
- **Cost:** Paid subscription
- **Best for:** Medium-sized hostel businesses

### ENTERPRISE Tier
- **Features:**
  - SMS + Email reminders
  - Custom SMS templates
  - Unlimited hostels
  - Unlimited tenants
  - Priority support
- **Cost:** Custom pricing
- **Best for:** Large hostel chains and property management companies

## Reminder Types

### 1. Before Due Date Reminder
- **Trigger:** 5 days before the due date
- **Schedule:** Daily at 9:00 AM
- **Purpose:** Proactive reminder to pay on time
- **Default Template:**
  ```
  Hi {tenantName}, your rent of ₹{amount} for {hostelName} - Room {roomNumber} is due on {dueDate}. Please pay on time to avoid late fees.
  ```

### 2. On Due Date Reminder
- **Trigger:** On the due date
- **Schedule:** Daily at 9:00 AM
- **Purpose:** Final reminder before payment becomes overdue
- **Default Template:**
  ```
  Hi {tenantName}, your rent of ₹{amount} for {hostelName} - Room {roomNumber} is due today ({dueDate}). Please make the payment to avoid penalties.
  ```

### 3. After Due Date Reminder (Overdue)
- **Trigger:** After the due date (for unpaid installments)
- **Schedule:** Daily at 10:00 AM
- **Purpose:** Urgent reminder with penalty information
- **Default Template:**
  ```
  Hi {tenantName}, your rent payment is overdue! Total amount due: ₹{totalAmount} (Rent: ₹{amount} + Late Fee: ₹{lateFee}). Please pay immediately for {hostelName} - Room {roomNumber}.
  ```

## Template Placeholders

Available placeholders for custom templates:
- `{tenantName}` - Tenant's full name
- `{amount}` - Original rent amount
- `{dueDate}` - Payment due date
- `{hostelName}` - Name of the hostel
- `{roomNumber}` - Room number
- `{lateFee}` - Late fee amount (if applicable)
- `{totalAmount}` - Total amount including late fees

## Database Schema

### New Tables

#### 1. owner_subscriptions
```sql
CREATE TABLE owner_subscriptions (
    subscription_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(user_id),
    tier VARCHAR(50) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sms_reminders_enabled BOOLEAN NOT NULL DEFAULT false,
    email_reminders_enabled BOOLEAN NOT NULL DEFAULT false,
    custom_templates_enabled BOOLEAN NOT NULL DEFAULT false,
    max_hostels INTEGER,
    max_tenants INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

#### 2. sms_templates
```sql
CREATE TABLE sms_templates (
    template_id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(user_id),
    reminder_type VARCHAR(50) NOT NULL,
    template_content VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

#### 3. reminder_logs
```sql
CREATE TABLE reminder_logs (
    log_id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES payment_request_schedules(schedule_id),
    tenant_id UUID NOT NULL REFERENCES users(user_id),
    reminder_type VARCHAR(50) NOT NULL,
    message_sent VARCHAR(500) NOT NULL,
    sent_via VARCHAR(20) NOT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    sent_at TIMESTAMP NOT NULL
);
```

#### 4. tenant_payment_plans (Updated)
```sql
ALTER TABLE tenant_payment_plans 
ADD COLUMN room_allotment_id UUID REFERENCES room_allotments(allotment_id);
```

## API Endpoints

### Subscription Management

#### Get Current Subscription
```http
GET /owner/subscription
Authorization: Bearer {token}
```

**Response:**
```json
{
  "subscriptionId": "uuid",
  "tier": "PRO",
  "startDate": "2026-01-01T00:00:00",
  "endDate": null,
  "isActive": true,
  "smsRemindersEnabled": true,
  "emailRemindersEnabled": true,
  "customTemplatesEnabled": true,
  "maxHostels": 10,
  "maxTenants": 200
}
```

#### Upgrade Subscription
```http
POST /owner/subscription/upgrade
Authorization: Bearer {token}
Content-Type: application/json

{
  "tier": "PRO"
}
```

#### Check Feature Availability
```http
GET /owner/subscription/feature/{featureName}
Authorization: Bearer {token}
```

**Example:** `/owner/subscription/feature/sms_reminders`

### SMS Template Management

#### Get All Templates
```http
GET /owner/sms-templates
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "templateId": "uuid",
    "reminderType": "BEFORE_DUE_DATE",
    "templateContent": "Custom template...",
    "isActive": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

#### Get All Available Templates (Custom + Default)
```http
GET /owner/sms-templates/all
Authorization: Bearer {token}
```

**Response:**
```json
{
  "BEFORE_DUE_DATE": "Hi {tenantName}, your rent...",
  "ON_DUE_DATE": "Hi {tenantName}, your rent is due today...",
  "AFTER_DUE_DATE": "Hi {tenantName}, your rent payment is overdue..."
}
```

#### Create/Update Template
```http
POST /owner/sms-templates
Authorization: Bearer {token}
Content-Type: application/json

{
  "reminderType": "BEFORE_DUE_DATE",
  "templateContent": "Dear {tenantName}, this is a friendly reminder that your rent of ₹{amount} for {hostelName} - Room {roomNumber} is due on {dueDate}. Thank you!"
}
```

**Note:** Requires PRO or ENTERPRISE subscription

#### Delete Template
```http
DELETE /owner/sms-templates/{templateId}
Authorization: Bearer {token}
```

## Scheduled Jobs

### Job Schedule
- **Before Due Date Reminders:** Daily at 9:00 AM
- **On Due Date Reminders:** Daily at 9:00 AM
- **Overdue Reminders:** Daily at 10:00 AM

### Job Logic
1. Query payment schedules based on due date and status
2. Check if reminder already sent (to avoid duplicates)
3. Get owner's subscription and check if reminders are enabled
4. Get appropriate template (custom or default)
5. Replace placeholders with actual values
6. Send SMS/Email based on subscription features
7. Log reminder in database

## Configuration

### Enable/Disable Reminders
Reminders are automatically enabled/disabled based on subscription tier. No manual configuration needed.

### Customize Cron Schedule
To change reminder timing, update the `@Scheduled` annotations in `ReminderService.java`:

```java
@Scheduled(cron = "0 0 9 * * ?")  // 9:00 AM daily
```

Cron format: `second minute hour day month weekday`

## Testing

### Manual Reminder Trigger
For testing purposes, you can manually trigger reminders:

```java
@Autowired
private ReminderService reminderService;

// Send manual reminder
reminderService.sendManualReminder(scheduleId, ReminderType.BEFORE_DUE_DATE);
```

### Test Scenarios
1. **Free Tier:** Only email reminders should be sent
2. **Beta/Pro Tier:** Both SMS and email should be sent
3. **Custom Templates:** PRO users should see their custom templates
4. **Duplicate Prevention:** Same reminder should not be sent twice
5. **Overdue Reminders:** Should include late fee in total amount

## Deployment Considerations

### 1. SMS Provider Configuration
Ensure Twilio credentials are configured in `application.yml`:
```yaml
twilio:
  account-sid: your-account-sid
  auth-token: your-auth-token
  phone-number: your-twilio-number
  enabled: true
```

### 2. Email Configuration
Configure SMTP settings for email reminders:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

### 3. Timezone Configuration
Set appropriate timezone for scheduled jobs:
```yaml
spring:
  jackson:
    time-zone: Asia/Kolkata
```

### 4. Database Indexes
Add indexes for better performance:
```sql
CREATE INDEX idx_payment_schedule_due_date ON payment_request_schedules(due_date, payment_status);
CREATE INDEX idx_reminder_log_schedule ON reminder_logs(schedule_id, reminder_type);
CREATE INDEX idx_subscription_owner ON owner_subscriptions(owner_id, is_active);
```

## Monitoring and Logs

### Log Levels
- **INFO:** Job start/completion, successful reminders
- **WARN:** Missing hostel/room information
- **ERROR:** Failed to send reminders, exceptions

### Key Metrics to Monitor
- Number of reminders sent per day
- Success/failure rate
- SMS delivery status
- Email bounce rate
- Subscription tier distribution

## Future Enhancements

1. **WhatsApp Integration:** Add WhatsApp as a notification channel
2. **Custom Reminder Schedule:** Allow owners to set custom reminder days
3. **Multi-language Support:** Templates in different languages
4. **Reminder Preferences:** Let tenants choose notification preferences
5. **Analytics Dashboard:** Show reminder statistics and effectiveness
6. **A/B Testing:** Test different template variations
7. **Smart Reminders:** ML-based optimal reminder timing

## Support

For issues or questions:
- Check logs in `reminder_logs` table
- Verify subscription tier and features
- Ensure SMS/Email services are configured
- Contact support with `log_id` for specific failures
