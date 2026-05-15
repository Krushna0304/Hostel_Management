# SMS OTP Setup Guide - Complete Steps

## Overview
This guide will help you set up SMS sending for OTP verification using Twilio. The system is already integrated and just needs configuration.

---

## Option 1: Twilio SMS (Recommended - Easiest)

### Step 1: Create Twilio Account

1. **Sign up for Twilio**:
   - Go to: https://www.twilio.com/try-twilio
   - Sign up for a free trial account
   - You'll get **$15 free credit** (enough for ~500 SMS)

2. **Verify your phone number**:
   - Twilio will ask you to verify your phone number
   - Enter your mobile number and verify the OTP they send

3. **Get a Twilio Phone Number**:
   - After signup, Twilio will assign you a free phone number
   - This will be your "From" number for sending SMS
   - Example: `+1234567890`

### Step 2: Get Twilio Credentials

1. **Go to Twilio Console**: https://console.twilio.com/
2. **Find your credentials** (on the dashboard):
   - **Account SID**: `ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **Auth Token**: Click "Show" to reveal it
   - **Phone Number**: Your Twilio number (e.g., `+1234567890`)

### Step 3: Configure Application

#### Option A: Using Environment Variables (Recommended for Production)

**Windows (Command Prompt):**
```cmd
set TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
set TWILIO_AUTH_TOKEN=your_auth_token_here
set TWILIO_PHONE_NUMBER=+1234567890
set TWILIO_ENABLED=true
```

**Windows (PowerShell):**
```powershell
$env:TWILIO_ACCOUNT_SID="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:TWILIO_AUTH_TOKEN="your_auth_token_here"
$env:TWILIO_PHONE_NUMBER="+1234567890"
$env:TWILIO_ENABLED="true"
```

**Linux/Mac:**
```bash
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your_auth_token_here
export TWILIO_PHONE_NUMBER=+1234567890
export TWILIO_ENABLED=true
```

#### Option B: Update application.yml Directly (For Testing Only)

Edit `HostelManagment_Backend/src/main/resources/application.yml`:

```yaml
twilio:
  account-sid: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
  auth-token: your_auth_token_here
  phone-number: +1234567890
  enabled: true
```

⚠️ **WARNING**: Never commit real credentials to Git! Use environment variables in production.

### Step 4: Add Verified Phone Numbers (Trial Account Only)

**For Twilio Trial Accounts:**
- You can only send SMS to **verified phone numbers**
- Go to: https://console.twilio.com/us1/develop/phone-numbers/manage/verified
- Click "Add a new number"
- Enter the owner's phone number (with country code, e.g., `+919876543210`)
- Verify it with the OTP Twilio sends

**For Paid Accounts:**
- No verification needed - can send to any number

### Step 5: Update Phone Number Format

The system automatically formats Indian numbers (+91). If you're using a different country:

Edit `SmsService.java` line 48:
```java
cleaned = "+91" + cleaned;  // Change +91 to your country code (e.g., +1 for US)
```

### Step 6: Test the System

1. **Reload Maven dependencies**:
   ```bash
   mvn clean install
   ```

2. **Restart your Spring Boot application**

3. **Check console on startup**:
   - ✅ Should see: `Twilio initialized successfully with phone number: +1234567890`
   - ⚠️ If disabled: `Twilio SMS is disabled. Set twilio.enabled=true to enable SMS sending.`

4. **Test OTP flow**:
   - Go to tenant activation page
   - Select "Cash Payment"
   - Click "Send OTP to Owner"
   - Owner should receive SMS with 6-digit OTP
   - Check console for: `✅ SMS sent successfully! SID: SMxxxxxxxx`

---

## Option 2: Alternative SMS Providers

### A. AWS SNS (Amazon Simple Notification Service)

**Pros**: Reliable, scalable, cheap ($0.00645 per SMS in India)

**Setup**:
1. Create AWS account
2. Go to SNS console
3. Enable SMS in your region
4. Add dependency:
```xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-sns</artifactId>
    <version>1.12.529</version>
</dependency>
```

5. Update `SmsService.java` to use AWS SNS SDK

### B. MSG91 (Popular in India)

**Pros**: India-focused, cheap, good delivery rates

**Setup**:
1. Sign up at: https://msg91.com/
2. Get API key
3. Use their REST API:
```java
RestTemplate restTemplate = new RestTemplate();
String url = "https://api.msg91.com/api/v5/otp";
// Make HTTP POST request
```

### C. Fast2SMS (India)

**Pros**: Very cheap for Indian numbers

**Setup**:
1. Sign up at: https://www.fast2sms.com/
2. Get API key
3. Use REST API similar to MSG91

---

## Troubleshooting

### Issue 1: "Twilio initialized successfully" but no SMS received

**Check**:
1. Is the phone number verified in Twilio console? (Trial accounts only)
2. Is the phone number format correct? (Must include country code: +919876543210)
3. Check Twilio console logs: https://console.twilio.com/us1/monitor/logs/sms
4. Check your Twilio balance

### Issue 2: "Twilio SMS is disabled"

**Solution**:
- Set `TWILIO_ENABLED=true` in environment variables OR
- Update `application.yml`: `twilio.enabled: true`

### Issue 3: "The number +919876543210 is unverified"

**Solution** (Trial account):
- Verify the number at: https://console.twilio.com/us1/develop/phone-numbers/manage/verified

**Solution** (Paid account):
- Upgrade your Twilio account to remove restrictions

### Issue 4: SMS not delivered in India

**Check**:
1. Indian regulations require DLT registration for commercial SMS
2. For testing, use Twilio trial with verified numbers
3. For production, register with DLT: https://www.twilio.com/docs/sms/regulatory/a2p-10dlc

### Issue 5: Maven build fails with Twilio dependency

**Solution**:
```bash
mvn clean install -U
```

If still fails, check internet connection and Maven repository access.

---

## Testing Without Real SMS (Development Mode)

If you want to test without sending real SMS:

1. Keep `twilio.enabled: false` in `application.yml`
2. OTP will be printed in console logs
3. System will work normally, just no actual SMS sent

---

## Cost Estimation

### Twilio Pricing (as of 2024):
- **India**: ₹0.50 - ₹1.00 per SMS (~$0.006 - $0.012)
- **USA**: $0.0079 per SMS
- **Free Trial**: $15 credit (~500-1000 SMS)

### Monthly Cost Example:
- 100 OTPs/day = 3000 OTPs/month
- Cost: 3000 × ₹0.50 = ₹1,500/month (~$18/month)

---

## Security Best Practices

1. ✅ **Never commit credentials to Git**
   - Use environment variables
   - Add `.env` to `.gitignore`

2. ✅ **Rotate credentials regularly**
   - Change Twilio auth token every 3-6 months

3. ✅ **Monitor usage**
   - Set up billing alerts in Twilio console
   - Monitor for unusual activity

4. ✅ **Rate limiting**
   - Already implemented: OTP expires in 10 minutes
   - Consider adding: Max 3 OTP requests per agreement

5. ✅ **Use HTTPS**
   - Ensure your backend uses HTTPS in production

---

## Production Checklist

Before going live:

- [ ] Upgrade Twilio account (remove trial restrictions)
- [ ] Set up environment variables on production server
- [ ] Enable HTTPS
- [ ] Set up monitoring/alerts for SMS failures
- [ ] Test with real phone numbers
- [ ] Set up DLT registration (if sending to India)
- [ ] Configure rate limiting
- [ ] Set up backup SMS provider (optional)

---

## Support

**Twilio Support**:
- Documentation: https://www.twilio.com/docs/sms
- Support: https://support.twilio.com/

**Application Support**:
- Check console logs for detailed error messages
- All SMS operations are logged with ✅ or ❌ indicators
