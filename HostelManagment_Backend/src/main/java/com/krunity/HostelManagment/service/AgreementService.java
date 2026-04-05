package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.AcceptAgreementRequest;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.repository.AgreementRepository;
import com.krunity.HostelManagment.enums.PaymentFrequency;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgreementService {
    
    @Autowired
    private AgreementRepository agreementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private RoomAgreementPlanService planService;
    
    private static final int QR_TOKEN_EXPIRY_HOURS = 72; // 3 days
    
    @Transactional
    public Agreement createAgreement(Agreement agreement) {
        // Validate user exists
        User user = userRepository.findById(agreement.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + agreement.getUserId()));
        
        // Validate room exists if it's a room agreement
        if (agreement.getType() == AgreementType.ROOM && agreement.getRoomId() != null) {
            roomRepository.findById(agreement.getRoomId())
                    .orElseThrow(() -> new NotFoundException("Room not found with ID: " + agreement.getRoomId()));
        }
        
        // Set status and timestamps
        agreement.setStatus(AgreementStatus.PENDING_TENANT_ACTION);
        agreement.setCreatedAt(Instant.now());
        agreement.setQrUsed(false);
        
        // Generate QR token
        String qrToken = generateQrToken();
        agreement.setQrToken(qrToken);
        agreement.setQrExpiry(Instant.now().plus(QR_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
        
        Agreement savedAgreement = agreementRepository.save(agreement);
        
        // Send notification
        notificationService.sendQrActivationEmail(savedAgreement, user);
        notificationService.sendQrActivationSms(savedAgreement, user);
        
        return savedAgreement;
    }
    
    public Optional<Agreement> validateQrToken(String token) {
        // Use repository method that checks expiry automatically
        return agreementRepository.findByQrTokenAndQrUsedFalseAndQrExpiryAfter(token, Instant.now());
    }
    
    @Transactional
    public Agreement activateAgreement(String agreementId, AcceptAgreementRequest request) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
        
        // Validate QR token is not used
        if (agreement.getQrUsed() != null && agreement.getQrUsed()) {
            throw new IllegalStateException("Agreement QR token has already been used");
        }
        
        // Validate token is not expired
        if (agreement.getQrExpiry() != null && agreement.getQrExpiry().isBefore(Instant.now())) {
            throw new IllegalStateException("Agreement QR token has expired");
        }
        
        // Validate status
        if (agreement.getStatus() != AgreementStatus.PENDING_TENANT_ACTION) {
            throw new IllegalStateException("Agreement is not in PENDING_TENANT_ACTION status");
        }
        
        // Get user
        User tenant = userRepository.findById(agreement.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Process payment
        processPayment(agreement, tenant, request);
        
        // Create payment plan
        createPaymentPlan(agreement, tenant);
        
        // Update agreement
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setQrUsed(true);
        agreement.setActivatedAt(Instant.now());
        
        // Activate user account
        tenant.setActive(true);
        userRepository.save(tenant);
        
        // Generate password reset token for tenant to set password
        passwordResetService.generatePasswordResetToken(tenant.getUserId());
        
        Agreement savedAgreement = agreementRepository.save(agreement);
        
        // Notify owner
        User owner = getOwnerForAgreement(agreement);
        notificationService.sendAgreementAcceptedNotification(savedAgreement, owner);
        
        return savedAgreement;
    }
    
    @Transactional
    public Agreement rejectAgreement(String agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new NotFoundException("Agreement not found with ID: " + agreementId));
        
        if (agreement.getStatus() != AgreementStatus.PENDING_TENANT_ACTION) {
            throw new IllegalStateException("Agreement cannot be rejected in current status");
        }
        
        agreement.setStatus(AgreementStatus.REJECTED);
        agreement.setQrUsed(true); // Mark QR as used even if rejected
        
        return agreementRepository.save(agreement);
    }
    
    private String generateQrToken() {
        // Generate a secure random token
        return UUID.randomUUID().toString().replace("-", "") + 
               Instant.now().toEpochMilli();
    }
    
    private void processPayment(Agreement agreement, User tenant, AcceptAgreementRequest request) {
        // Calculate total amount (rent + deposit + cleaning + maintenance)
        BigDecimal totalAmount = agreement.getRent()
                .add(agreement.getDeposit() != null ? agreement.getDeposit() : BigDecimal.ZERO)
                .add(agreement.getCleaningCharges());
//                .add(agreement.getMaintenanceCharges());
        
        // Get owner (assuming first user with OWNER role or from room's hostel owner)
        User owner = getOwnerForAgreement(agreement);

        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .fromUser(tenant)
                .toUser(owner)
                .amount(totalAmount.longValue())
                .mode(request.getPaymentMode())
                .status(request.getPaymentMode() == TransactionMode.ONLINE 
                        ? TransactionStatus.COMPLETED 
                        : TransactionStatus.COMPLETED)
                .reason("Agreement activation payment")
                .otpVerified(request.getPaymentMode() == TransactionMode.CASH && request.getOtp() != null)
                .build();
        
        transactionRepository.save(transaction);
    }
    
    private void createPaymentPlan(Agreement agreement, User tenant) {
        // Get payment type for rent
        
        // Calculate next due date (assuming monthly payments)
        LocalDate startDate = agreement.getStartDate();
        LocalDate nextDueDate = startDate.plusMonths(1);
        
        // Create payment plan
        TenantPaymentPlan paymentPlan = new TenantPaymentPlan();
        paymentPlan.setTenant(tenant);;
        paymentPlan.setPaymentFrequency(PaymentFrequency.MONTHLY);
        paymentPlan.setDepositAmount(agreement.getRent().longValue());
        paymentPlan.setStartDate(startDate);
        paymentPlan.setPendingInstallments(12); // Assuming 12 months
        paymentPlan.setActive(true);
        
        paymentPlanRepository.save(paymentPlan);
    }
    
    public List<Agreement> getAllAgreements() {
        return agreementRepository.findAll();
    }
    
    private User getOwnerForAgreement(Agreement agreement) {
        // If room agreement, get owner from room's hostel
        if (agreement.getType() == AgreementType.ROOM && agreement.getRoomId() != null) {
            Room room = roomRepository.findById(agreement.getRoomId())
                    .orElseThrow(() -> new NotFoundException("Room not found"));
            return room.getHostel().getOwner();
        }
        
        // Otherwise, find first user with OWNER role
        return userRepository.findAll().stream()
                .filter(user -> "OWNER".equalsIgnoreCase(user.getRole().getName()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Owner not found"));
    }
}
