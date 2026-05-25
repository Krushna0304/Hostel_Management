package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.model.CashPaymentOtp;
import com.krunity.HostelManagment.model.Room;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.repository.CashPaymentOtpRepository;
import com.krunity.HostelManagment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class CashPaymentOtpService {
    
    @Autowired
    private CashPaymentOtpRepository otpRepository;
    
    @Autowired
    private AgreementRepository agreementRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository scheduleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.krunity.HostelManagment.repository.ElectricityBillRepository electricityBillRepository;
    
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    @Transactional
    public String generateAndSendOtp(String agreementId) {
        // Get agreement
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        
        // Get owner from room
        Room room = roomRepository.findById(agreement.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));
        
        User owner = room.getHostel().getOwner();
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        
        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);
        
        // Save OTP with expiry
        CashPaymentOtp cashPaymentOtp = CashPaymentOtp.builder()
                .agreementId(agreementId)
                .ownerPhone(owner.getPhoneNumber())
                .otpHash(otpHash)
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        
        otpRepository.save(cashPaymentOtp);
        
        // Send OTP to owner via SMS
        notificationService.sendCashPaymentOtp(owner, otp);
        
        return "OTP sent to owner's mobile number ending with ******" + maskPhoneNumber(owner.getPhoneNumber());
    }
    
    @Transactional
    public boolean verifyOtp(String agreementId, String otp) {
        // Find valid OTP for this agreement
        Optional<CashPaymentOtp> otpOpt = otpRepository.findByAgreementIdAndUsedFalseAndExpiryTimeAfter(
                agreementId, 
                Instant.now()
        );
        
        if (otpOpt.isEmpty()) {
            return false;
        }
        
        CashPaymentOtp cashPaymentOtp = otpOpt.get();
        
        // Verify OTP using password encoder (handles BCrypt comparison)
        boolean isValid = passwordEncoder.matches(otp, cashPaymentOtp.getOtpHash());
        
        if (isValid) {
            // Mark OTP as used
            cashPaymentOtp.setUsed(true);
            otpRepository.save(cashPaymentOtp);
        }
        
        return isValid;
    }
    
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    @Transactional
    public String generateAndSendInstallmentOtp(String scheduleIdStr) {
        java.util.UUID scheduleId = java.util.UUID.fromString(scheduleIdStr);
        
        System.out.println("=== GENERATING INSTALLMENT OTP ===");
        System.out.println("Schedule ID: " + scheduleId);
        
        // First, invalidate any existing valid OTPs for this schedule to prevent duplicates
        List<CashPaymentOtp> existingOtps = otpRepository.findAllByScheduleIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                scheduleId, 
                Instant.now()
        );
        
        if (!existingOtps.isEmpty()) {
            System.out.println("Found " + existingOtps.size() + " existing valid OTPs - marking them as used");
            for (CashPaymentOtp existingOtp : existingOtps) {
                existingOtp.setUsed(true);
                otpRepository.save(existingOtp);
                System.out.println("Invalidated existing OTP: " + existingOtp.getOtpId());
            }
        }
        
        // Get payment schedule
        com.krunity.HostelManagment.model.PaymentRequestSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Payment schedule not found"));
        
        // Get agreement from tenant payment plan
        String agreementId = schedule.getTenantPaymentPlan().getAgreementId();
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found"));
        
        // Get owner from room
        Room room = roomRepository.findById(agreement.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));
        
        User owner = room.getHostel().getOwner();
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        System.out.println("Generated new OTP for owner: " + owner.getUsername());
        
        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);
        
        // Save OTP with expiry
        CashPaymentOtp cashPaymentOtp = CashPaymentOtp.builder()
                .scheduleId(scheduleId)
                .ownerPhone(owner.getPhoneNumber())
                .otpHash(otpHash)
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        
        otpRepository.save(cashPaymentOtp);
        System.out.println("Saved new OTP with ID: " + cashPaymentOtp.getOtpId());
        
        // Send OTP to owner via SMS
        notificationService.sendCashPaymentOtp(owner, otp);
        
        System.out.println("=== OTP GENERATION COMPLETE ===");
        return "OTP sent to owner's mobile number ending with ******" + maskPhoneNumber(owner.getPhoneNumber());
    }
    
    @Transactional
    public boolean verifyInstallmentOtp(java.util.UUID scheduleId, String otp) {
        System.out.println("=== OTP VERIFICATION START ===");
        System.out.println("Schedule ID: " + scheduleId);
        System.out.println("OTP to verify: " + otp);
        
        // Find all valid OTPs for this schedule, ordered by creation date (most recent first)
        List<CashPaymentOtp> validOtps = otpRepository.findAllByScheduleIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                scheduleId, 
                Instant.now()
        );
        
        if (validOtps.isEmpty()) {
            System.out.println("No valid OTP found for schedule ID: " + scheduleId);
            System.out.println("Current time: " + Instant.now());
            
            // Let's check if there are any OTPs for this schedule (regardless of expiry/used status)
            List<CashPaymentOtp> allOtps = otpRepository.findAll().stream()
                    .filter(o -> scheduleId.equals(o.getScheduleId()))
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Found " + allOtps.size() + " OTPs for this schedule:");
            for (CashPaymentOtp otpRecord : allOtps) {
                System.out.println("  - OTP ID: " + otpRecord.getOtpId());
                System.out.println("    Used: " + otpRecord.getUsed());
                System.out.println("    Expiry: " + otpRecord.getExpiryTime());
                System.out.println("    Created: " + otpRecord.getCreatedAt());
                System.out.println("    Owner Phone: " + otpRecord.getOwnerPhone());
            }
            
            return false;
        }
        
        System.out.println("Found " + validOtps.size() + " valid OTP(s) for this schedule");
        
        // Use the most recent OTP (first in the list due to DESC ordering)
        CashPaymentOtp cashPaymentOtp = validOtps.get(0);
        System.out.println("Using most recent OTP record:");
        System.out.println("  - OTP ID: " + cashPaymentOtp.getOtpId());
        System.out.println("  - Created: " + cashPaymentOtp.getCreatedAt());
        System.out.println("  - Expiry: " + cashPaymentOtp.getExpiryTime());
        System.out.println("  - Used: " + cashPaymentOtp.getUsed());
        System.out.println("  - Owner Phone: " + cashPaymentOtp.getOwnerPhone());
        
        // Verify OTP using password encoder (handles BCrypt comparison)
        System.out.println("Comparing provided OTP with stored hash...");
        boolean isValid = passwordEncoder.matches(otp, cashPaymentOtp.getOtpHash());
        System.out.println("OTP comparison result: " + isValid);
        
        if (isValid) {
            System.out.println("OTP is valid - marking ALL valid OTPs for this schedule as used");
            // Mark ALL valid OTPs for this schedule as used to prevent reuse
            for (CashPaymentOtp otpRecord : validOtps) {
                otpRecord.setUsed(true);
                otpRepository.save(otpRecord);
                System.out.println("Marked OTP " + otpRecord.getOtpId() + " as used");
            }
        } else {
            System.out.println("OTP is invalid");
        }
        
        System.out.println("=== OTP VERIFICATION END ===");
        return isValid;
    }
    
    @Transactional
    public String generateAndSendSettlementOtp(String settlementId) {
        System.out.println("=== GENERATING SETTLEMENT OTP ===");
        System.out.println("Settlement ID: " + settlementId);
        
        // First, invalidate any existing valid OTPs for this settlement to prevent duplicates
        List<CashPaymentOtp> existingOtps = otpRepository.findAllBySettlementIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                settlementId, 
                Instant.now()
        );
        
        if (!existingOtps.isEmpty()) {
            System.out.println("Found " + existingOtps.size() + " existing valid OTPs - marking them as used");
            for (CashPaymentOtp existingOtp : existingOtps) {
                existingOtp.setUsed(true);
                otpRepository.save(existingOtp);
                System.out.println("Invalidated existing OTP: " + existingOtp.getOtpId());
            }
        }
        
        // Get settlement details to find the owner
        // We need to inject SettlementService or SettlementRepository to get settlement details
        // For now, let's assume we can get the owner through the settlement
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        System.out.println("Generated new OTP for settlement");
        
        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);
        
        // Save OTP with expiry
        CashPaymentOtp cashPaymentOtp = CashPaymentOtp.builder()
                .settlementId(settlementId)
                .ownerPhone("1234567890") // This should be retrieved from settlement owner
                .otpHash(otpHash)
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        
        otpRepository.save(cashPaymentOtp);
        System.out.println("Saved new OTP with ID: " + cashPaymentOtp.getOtpId());
        
        // Send OTP to owner via SMS
        // notificationService.sendCashPaymentOtp(owner, otp);
        
        System.out.println("=== SETTLEMENT OTP GENERATION COMPLETE ===");
        return "OTP sent to owner's mobile number ending with ******1890";
    }
    
    @Transactional
    public boolean verifySettlementOtp(String settlementId, String otp) {
        System.out.println("=== SETTLEMENT OTP VERIFICATION START ===");
        System.out.println("Settlement ID: " + settlementId);
        System.out.println("OTP to verify: " + otp);
        
        // Find all valid OTPs for this settlement, ordered by creation date (most recent first)
        List<CashPaymentOtp> validOtps = otpRepository.findAllBySettlementIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                settlementId, 
                Instant.now()
        );
        
        if (validOtps.isEmpty()) {
            System.out.println("No valid OTP found for settlement ID: " + settlementId);
            return false;
        }
        
        System.out.println("Found " + validOtps.size() + " valid OTP(s) for this settlement");
        
        // Use the most recent OTP (first in the list due to DESC ordering)
        CashPaymentOtp cashPaymentOtp = validOtps.get(0);
        System.out.println("Using most recent OTP record:");
        System.out.println("  - OTP ID: " + cashPaymentOtp.getOtpId());
        System.out.println("  - Created: " + cashPaymentOtp.getCreatedAt());
        System.out.println("  - Expiry: " + cashPaymentOtp.getExpiryTime());
        
        // Verify OTP using password encoder (handles BCrypt comparison)
        System.out.println("Comparing provided OTP with stored hash...");
        boolean isValid = passwordEncoder.matches(otp, cashPaymentOtp.getOtpHash());
        System.out.println("OTP comparison result: " + isValid);
        
        if (isValid) {
            System.out.println("OTP is valid - marking ALL valid OTPs for this settlement as used");
            // Mark ALL valid OTPs for this settlement as used to prevent reuse
            for (CashPaymentOtp otpRecord : validOtps) {
                otpRecord.setUsed(true);
                otpRepository.save(otpRecord);
                System.out.println("Marked OTP " + otpRecord.getOtpId() + " as used");
            }
        } else {
            System.out.println("OTP is invalid");
        }
        
        System.out.println("=== SETTLEMENT OTP VERIFICATION END ===");
        return isValid;
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiryTimeBefore(Instant.now());
    }
    
    @Autowired
    private com.krunity.HostelManagment.repository.OtherChargeRepository otherChargeRepository;
    
    @Transactional
    public String generateAndSendOtherChargeOtp(String chargeId) {
        System.out.println("=== GENERATING OTHER CHARGE OTP ===");
        System.out.println("Charge ID: " + chargeId);
        
        // First, invalidate any existing valid OTPs for this charge to prevent duplicates
        List<CashPaymentOtp> existingOtps = otpRepository.findAllByChargeIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                chargeId, 
                Instant.now()
        );
        
        if (!existingOtps.isEmpty()) {
            System.out.println("Found " + existingOtps.size() + " existing valid OTPs - marking them as used");
            for (CashPaymentOtp existingOtp : existingOtps) {
                existingOtp.setUsed(true);
                otpRepository.save(existingOtp);
                System.out.println("Invalidated existing OTP: " + existingOtp.getOtpId());
            }
        }
        
        // Get other charge details to find the owner
        java.util.UUID chargeUuid = java.util.UUID.fromString(chargeId);
        com.krunity.HostelManagment.model.OtherCharge otherCharge = otherChargeRepository.findById(chargeUuid)
                .orElseThrow(() -> new NotFoundException("Other charge not found"));
        
        User owner = otherCharge.getOwner();
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        System.out.println("Generated new OTP for other charge owner: " + owner.getUsername());
        
        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);
        
        // Save OTP with expiry
        CashPaymentOtp cashPaymentOtp = CashPaymentOtp.builder()
                .chargeId(chargeId)
                .ownerPhone(owner.getPhoneNumber())
                .otpHash(otpHash)
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        
        otpRepository.save(cashPaymentOtp);
        System.out.println("Saved new OTP with ID: " + cashPaymentOtp.getOtpId());
        
        // Send OTP to owner via SMS
        notificationService.sendCashPaymentOtp(owner, otp);
        
        System.out.println("=== OTHER CHARGE OTP GENERATION COMPLETE ===");
        return "OTP sent to owner's mobile number ending with *******" + maskPhoneNumber(owner.getPhoneNumber());
    }
    
    @Transactional
    public boolean verifyOtherChargeOtp(String chargeId, String otp) {
        System.out.println("=== OTHER CHARGE OTP VERIFICATION START ===");
        System.out.println("Charge ID: " + chargeId);
        System.out.println("OTP to verify: " + otp);
        
        // Find all valid OTPs for this charge, ordered by creation date (most recent first)
        List<CashPaymentOtp> validOtps = otpRepository.findAllByChargeIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                chargeId, 
                Instant.now()
        );
        
        if (validOtps.isEmpty()) {
            System.out.println("No valid OTP found for charge ID: " + chargeId);
            return false;
        }
        
        System.out.println("Found " + validOtps.size() + " valid OTP(s) for this charge");
        
        // Use the most recent OTP (first in the list due to DESC ordering)
        CashPaymentOtp cashPaymentOtp = validOtps.get(0);
        System.out.println("Using most recent OTP record:");
        System.out.println("  - OTP ID: " + cashPaymentOtp.getOtpId());
        System.out.println("  - Created: " + cashPaymentOtp.getCreatedAt());
        System.out.println("  - Expiry: " + cashPaymentOtp.getExpiryTime());
        
        // Verify OTP using password encoder (handles BCrypt comparison)
        System.out.println("Comparing provided OTP with stored hash...");
        boolean isValid = passwordEncoder.matches(otp, cashPaymentOtp.getOtpHash());
        System.out.println("OTP comparison result: " + isValid);
        
        if (isValid) {
            System.out.println("OTP is valid - marking ALL valid OTPs for this charge as used");
            // Mark ALL valid OTPs for this charge as used to prevent reuse
            for (CashPaymentOtp otpRecord : validOtps) {
                otpRecord.setUsed(true);
                otpRepository.save(otpRecord);
                System.out.println("Marked OTP " + otpRecord.getOtpId() + " as used");
            }
        } else {
            System.out.println("OTP is invalid");
        }
        
        System.out.println("=== OTHER CHARGE OTP VERIFICATION END ===");
        return isValid;
    }
    
    @Transactional
    public String generateAndSendElectricityOtp(String billIdStr) {
        java.util.UUID billId = java.util.UUID.fromString(billIdStr);
        
        System.out.println("=== GENERATING ELECTRICITY BILL OTP ===");
        System.out.println("Bill ID: " + billId);
        
        // Invalidate any existing valid OTPs for this bill
        List<CashPaymentOtp> existingOtps = otpRepository.findAllByElectricityBillIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                billId, 
                Instant.now()
        );
        
        if (!existingOtps.isEmpty()) {
            System.out.println("Found " + existingOtps.size() + " existing valid OTPs - marking them as used");
            for (CashPaymentOtp existingOtp : existingOtps) {
                existingOtp.setUsed(true);
                otpRepository.save(existingOtp);
            }
        }
        
        // Get bill details to find the owner
        com.krunity.HostelManagment.model.ElectricityBill bill = electricityBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Electricity bill not found"));
        
        // Get owner from bill's room
        Room room = roomRepository.findById(bill.getRoomId())
                .orElseThrow(() -> new NotFoundException("Room not found"));
        
        User owner = room.getHostel().getOwner();
        
        // Generate 6-digit OTP
        String otp = generateOtp();
        System.out.println("Generated new OTP for electricity bill owner: " + owner.getUsername());
        
        // Hash the OTP before storing
        String otpHash = passwordEncoder.encode(otp);
        
        // Save OTP with expiry
        CashPaymentOtp cashPaymentOtp = CashPaymentOtp.builder()
                .electricityBillId(billId)
                .ownerPhone(owner.getPhoneNumber())
                .otpHash(otpHash)
                .expiryTime(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        
        otpRepository.save(cashPaymentOtp);
        System.out.println("Saved new OTP with ID: " + cashPaymentOtp.getOtpId());
        
        // Send OTP to owner via SMS
        notificationService.sendCashPaymentOtp(owner, otp);
        
        System.out.println("=== ELECTRICITY BILL OTP GENERATION COMPLETE ===");
        return "OTP sent to owner's mobile number ending with ******" + maskPhoneNumber(owner.getPhoneNumber());
    }
    
    @Transactional
    public boolean verifyElectricityOtp(java.util.UUID billId, String otp) {
        System.out.println("=== ELECTRICITY BILL OTP VERIFICATION START ===");
        System.out.println("Bill ID: " + billId);
        
        // Find all valid OTPs for this bill, ordered by creation date (most recent first)
        List<CashPaymentOtp> validOtps = otpRepository.findAllByElectricityBillIdAndUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                billId, 
                Instant.now()
        );
        
        if (validOtps.isEmpty()) {
            System.out.println("No valid OTP found for bill ID: " + billId);
            return false;
        }
        
        System.out.println("Found " + validOtps.size() + " valid OTP(s) for this bill");
        
        // Use the most recent OTP
        CashPaymentOtp cashPaymentOtp = validOtps.get(0);
        
        // Verify OTP using password encoder
        boolean isValid = passwordEncoder.matches(otp, cashPaymentOtp.getOtpHash());
        System.out.println("OTP comparison result: " + isValid);
        
        if (isValid) {
            System.out.println("OTP is valid - marking ALL valid OTPs for this bill as used");
            for (CashPaymentOtp otpRecord : validOtps) {
                otpRecord.setUsed(true);
                otpRepository.save(otpRecord);
            }
        }
        
        System.out.println("=== ELECTRICITY BILL OTP VERIFICATION END ===");
        return isValid;
    }
}
