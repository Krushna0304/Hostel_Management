# 🔔 Automated Payment Reminder System

A comprehensive, subscription-based payment reminder system for the Hostel Management application that automatically sends SMS and email notifications to tenants about upcoming and overdue payments.

## 🌟 Key Features

### ✅ Automated Reminders
- **5 Days Before Due Date** - Proactive reminder
- **On Due Date** - Final reminder before overdue
- **After Due Date** - Daily reminders with penalty information

### 🎯 Subscription Tiers
- **FREE** - Email reminders, 1 hostel, 10 tenants
- **BETA** - SMS + Email, 3 hostels, 50 tenants (6 months)
- **PRO** - Custom templates, 10 hostels, 200 tenants
- **ENTERPRISE** - Unlimited everything + priority support

### 📝 Customizable Templates
- PRO and ENTERPRISE users can create custom SMS templates
- 7 dynamic placeholders for personalization
- Default templates provided for all tiers

### 📊 Complete Audit Trail
- Every reminder logged with delivery status
- Success/failure tracking
- Error message capture

## 🚀 Quick Start

### 1. Database Setup

The application uses JPA with `ddl-auto: update`, so tables will be created automatically. Alternatively, run the migration script:

```bash
psql -U postgres -d hostel_management -f database_migration_reminders.sql
```

### 2. Configuration

Ensure these settings in `application.yml`:

```yaml
# Twilio SMS Configuration
twilio:
  account-sid: your-account-sid
  auth-token: your-auth-token
  phone-number: your-twilio-number
  enabled: true

# Email Configuration
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# Enable email notifications
app:
  email:
    enabled: true
```

### 3. Start the Application

```bash
./mvnw spring-boot:run
```

Scheduled jobs will start automatically!

## 📱 API Usage

### Check Your Subscription

```bash
curl -X GET http://localhost:8080/owner/subscription \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "subscriptionId": "uuid",
  "tier": "FREE",
  "smsRemindersEnabled": false,
  "emailRemindersEnabled": true,
  "customTemplatesEnabled": false,
  "maxHostels": 1,
  "maxTenants": 10
}
```

### Upgrade to PRO

```bash
curl -X POST http://localhost:8080/owner/subscription/upgrade \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tier": "PRO"}'
```

### Create Custom Template (PRO+ only)

```bash
curl -X POST http://localhost:8080/owner/sms-templates \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reminderType": "BEFORE_DUE_DATE",
    "templateContent": "Dear {tenantName}, your rent of ₹{amount} for {hostelName} - Room {roomNumber} is due on {dueDate}. Thank you!"
  }'
```

### View All Templates

```bash
curl -X GET http://localhost:8080/owner/sms-templates/all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📋 Template Placeholders

Use these in your custom templates:

| Placeholder | Description | Example |
|------------|-------------|---------|
| `{tenantName}` | Tenant's display name | "John Doe" |
| `{amount}` | Rent amount | "8500" |
| `{dueDate}` | Payment due date | "2026-05-01" |
| `{hostelName}` | Hostel name | "Sunrise Hostel" |
| `{roomNumber}` | Room number | "A001" |
| `{lateFee}` | Late fee amount | "500" |
| `{totalAmount}` | Total with late fees | "9000" |

## ⏰ Reminder Schedule

| Reminder Type | Trigger | Schedule | Channels |
|--------------|---------|----------|----------|
| Before Due Date | 5 days before | 9:00 AM daily | SMS/Email* |
| On Due Date | On due date | 9:00 AM daily | SMS/Email* |
| After Due Date | After due date | 10:00 AM daily | SMS/Email* |

*Based on subscription tier

## 🎨 Default Templates

### Before Due Date
```
Hi {tenantName}, your rent of ₹{amount} for {hostelName} - Room {roomNumber} 
is due on {dueDate}. Please pay on time to avoid late fees.
```

### On Due Date
```
Hi {tenantName}, your rent of ₹{amount} for {hostelName} - Room {roomNumber} 
is due today ({dueDate}). Please make the payment to avoid penalties.
```

### After Due Date
```
Hi {tenantName}, your rent payment is overdue! Total amount due: ₹{totalAmount} 
(Rent: ₹{amount} + Late Fee: ₹{lateFee}). Please pay immediately for 
{hostelName} - Room {roomNumber}.
```

## 🔍 Monitoring

### Check Reminder Logs

```sql
SELECT 
    u.display_name as tenant_name,
    rl.reminder_type,
    rl.sent_via,
    rl.success,
    rl.error_message,
    rl.sent_at
FROM reminder_logs rl
JOIN users u ON rl.tenant_id = u.user_id
ORDER BY rl.sent_at DESC
LIMIT 20;
```

### Check Upcoming Reminders

```sql
SELECT 
    u.display_name as tenant_name,
    prs.due_date,
    prs.amount,
    prs.payment_status
FROM payment_request_schedules prs
JOIN tenant_payment_plans tpp ON prs.tenant_payment_plan = tpp.plan_id
JOIN users u ON tpp.tenant_id = u.user_id
WHERE prs.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
AND prs.payment_status IN ('SCHEDULED', 'PARTIALLY_PAID')
ORDER BY prs.due_date;
```

### Application Logs

Look for these log messages:
```
INFO  - Starting before due date reminder job...
INFO  - Found 5 installments due on 2026-05-06
INFO  - Reminder sent to tenant xxx for schedule yyy via SMS
INFO  - Before due date reminder job completed
```

## 🧪 Testing

### Manual Reminder Trigger (for testing)

Add this endpoint to `ReminderService` for testing:

```java
@PostMapping("/test/reminder/{scheduleId}")
public ResponseEntity<?> testReminder(
    @PathVariable UUID scheduleId,
    @RequestParam ReminderType type
) {
    reminderService.sendManualReminder(scheduleId, type);
    return ResponseEntity.ok("Reminder sent");
}
```

Then trigger manually:
```bash
curl -X POST "http://localhost:8080/test/reminder/{scheduleId}?type=BEFORE_DUE_DATE"
```

### Test Checklist

- [ ] FREE tier sends only email
- [ ] BETA/PRO tier sends SMS + email
- [ ] Custom templates work for PRO users
- [ ] Default templates used when no custom template
- [ ] No duplicate reminders sent
- [ ] Before due date reminder sent 5 days before
- [ ] On due date reminder sent on due date
- [ ] Overdue reminders sent daily
- [ ] Late fees included in overdue messages
- [ ] All placeholders replaced correctly
- [ ] Subscription upgrade enables features immediately
- [ ] Template CRUD restricted to PRO+ users

## 📊 Subscription Comparison

| Feature | FREE | BETA | PRO | ENTERPRISE |
|---------|------|------|-----|------------|
| **Email Reminders** | ✅ | ✅ | ✅ | ✅ |
| **SMS Reminders** | ❌ | ✅ | ✅ | ✅ |
| **Custom Templates** | ❌ | ❌ | ✅ | ✅ |
| **Max Hostels** | 1 | 3 | 10 | ∞ |
| **Max Tenants** | 10 | 50 | 200 | ∞ |
| **Validity** | Forever | 6 months | Forever | Forever |
| **Priority Support** | ❌ | ❌ | ❌ | ✅ |
| **Cost** | Free | Free | Paid | Custom |

## 🛠️ Troubleshooting

### Reminders Not Sending

1. **Check subscription tier:**
   ```bash
   GET /owner/subscription
   ```

2. **Verify Twilio/SMTP configuration:**
   ```yaml
   twilio:
     enabled: true  # Must be true
   app:
     email:
       enabled: true  # Must be true
   ```

3. **Check application logs:**
   ```bash
   tail -f logs/application.log | grep -i reminder
   ```

4. **Verify payment schedules exist:**
   ```sql
   SELECT * FROM payment_request_schedules 
   WHERE due_date = CURRENT_DATE + INTERVAL '5 days';
   ```

### SMS Not Delivered

1. Check Twilio account balance
2. Verify phone number format (+91xxxxxxxxxx)
3. Check `reminder_logs` for error messages
4. Ensure `twilio.enabled=true`

### Email Not Delivered

1. Verify SMTP credentials
2. Check spam folder
3. Ensure `app.email.enabled=true`
4. Check email address format

### Custom Templates Not Working

1. Verify subscription tier is PRO or ENTERPRISE
2. Check template is active: `is_active = true`
3. Verify template exists for reminder type
4. Check application logs for errors

## 📚 Documentation

- **[Complete Documentation](REMINDER_SYSTEM_DOCUMENTATION.md)** - Detailed guide
- **[Implementation Summary](REMINDER_IMPLEMENTATION_SUMMARY.md)** - What was built
- **[Database Migration](database_migration_reminders.sql)** - SQL scripts

## 🎯 Future Enhancements

- [ ] WhatsApp integration
- [ ] Custom reminder schedules per owner
- [ ] Multi-language templates
- [ ] Tenant notification preferences
- [ ] Analytics dashboard
- [ ] A/B testing for templates
- [ ] Smart reminder timing (ML-based)
- [ ] Push notifications
- [ ] Voice call reminders
- [ ] Payment link in reminders

## 💡 Best Practices

1. **Start with FREE tier** - Test the system
2. **Monitor logs** - Check reminder delivery
3. **Customize templates** - Personalize for your brand (PRO+)
4. **Keep templates short** - SMS has character limits
5. **Test before production** - Use manual trigger for testing
6. **Monitor delivery rates** - Check success/failure ratios
7. **Update phone numbers** - Ensure tenant contact info is current

## 🤝 Support

For issues or questions:
1. Check `reminder_logs` table for delivery status
2. Review application logs
3. Verify configuration settings
4. Check subscription tier and features
5. Contact support with `log_id` for specific failures

## 📄 License

Part of the Hostel Management System

---

**Built with ❤️ for better tenant communication and reduced late payments!**
