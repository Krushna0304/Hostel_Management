package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.RoomAgreementPlan;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.model.Transaction;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.dto.InstallmentResponse;
import com.krunity.HostelManagment.dto.PaymentLedgerResponse;
import com.krunity.HostelManagment.dto.RecordPaymentRequest;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import com.krunity.HostelManagment.repository.TenantPaymentPlanRepository;
import com.krunity.HostelManagment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentScheduleService {

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CashPaymentOtpService cashPaymentOtpService;
    
    @Autowired
    private PaymentService paymentService;

    /**
     * Generates the full installment schedule for a tenant after agreement activation.
     * Called from AgreementService.activateAgreement().
     * 
     * Enhanced to:
     * - Calculate installment dates based on months per installment interval
     * - Mark first installment as COMPLETED (paid during activation)
     * - Generate dates based on calculated interval from plan configuration
     * - Link first installment to activation transaction
     */
    @Transactional
    public List<PaymentRequestSchedule> generateSchedule(
            TenantPaymentPlan plan,
            RoomAgreementPlan planSnapshot,
            Transaction activationTransaction
    ) {
        int totalInstallments = plan.getPendingInstallments() != null ? plan.getPendingInstallments() : 12;
        LocalDate startDate = plan.getStartDate();
        long installmentAmount = plan.getInstallmentAmount();

        // Determine due day from plan snapshot, default to 5th of each month
        int dueDayOfMonth = 5;
        if (planSnapshot != null
                && planSnapshot.getPaymentModel() != null
                && planSnapshot.getPaymentModel().getDueDayOfMonth() != null) {
            dueDayOfMonth = planSnapshot.getPaymentModel().getDueDayOfMonth();
        }

        // Calculate months per installment based on plan configuration
        int monthsPerInstallment = 1; // Default to monthly
        boolean isNotFixed = AgreementService.isNotFixedDuration(planSnapshot);
        if (!isNotFixed && planSnapshot != null && planSnapshot.getDuration() != null && planSnapshot.getPaymentModel() != null
                && planSnapshot.getDuration().getValue() != null) {
            int totalDurationMonths = planSnapshot.getDuration().getValue();
            int numberOfInstallments = planSnapshot.getPaymentModel().getInstallments();
            if (numberOfInstallments > 0) {
                monthsPerInstallment = Math.max(1, (int) Math.ceil((double) totalDurationMonths / numberOfInstallments));
            }
        }

        List<PaymentRequestSchedule> schedules = new ArrayList<>();
        for (int i = 0; i < totalInstallments; i++) {
            // Calculate due date based on installment interval (months per installment)
            LocalDate monthBase = startDate.plusMonths(i * monthsPerInstallment);
            // Clamp day to last day of month (e.g. Feb 28/29)
            int day = Math.min(dueDayOfMonth, monthBase.lengthOfMonth());
            LocalDate dueDate = monthBase.withDayOfMonth(day);

            // First installment is marked as COMPLETED (paid during activation)
            boolean isFirstInstallment = (i == 0);
            TransactionStatus status = isFirstInstallment ? TransactionStatus.COMPLETED : TransactionStatus.SCHEDULED;
            long paidAmount = isFirstInstallment ? installmentAmount : 0L;
            LocalDateTime paidAt = isFirstInstallment ? LocalDateTime.now() : null;
            UUID transactionId = isFirstInstallment && activationTransaction != null ? activationTransaction.getTransactionId() : null;

            PaymentRequestSchedule schedule = PaymentRequestSchedule.builder()
                    .tenantPaymentPlan(plan)
                    .installmentNumber(i + 1)
                    .amount(installmentAmount)
                    .dueDate(dueDate)
                    .paymentStatus(status)
                    .paidAmount(paidAmount)
                    .paidAt(paidAt)
                    .transactionId(transactionId)
                    .lateFeeApplied(0L)
                    .build();

            schedules.add(schedule);
        }

        // Set end date on the payment plan based on agreement duration, not last installment date
        // For prepaid plans, the agreement covers the full duration regardless of payment schedule
        // Example: 12-month plan with 3 installments still covers 12 months, not just until last payment
        if (planSnapshot != null && planSnapshot.getDuration() != null) {
            // Calculate actual agreement end date based on plan duration
            int durationValue = planSnapshot.getDuration().getValue();
            String durationUnit = planSnapshot.getDuration().getUnit();
            
            LocalDate agreementEndDate;
            if ("YEAR".equalsIgnoreCase(durationUnit)) {
                agreementEndDate = startDate.plusYears(durationValue);
            } else {
                // Default to months for "MONTH" or any other unit
                agreementEndDate = startDate.plusMonths(durationValue);
            }
            
            plan.setEndDate(agreementEndDate);
        } else if (!schedules.isEmpty()) {
            // Fallback to last installment date if no duration info available
            plan.setEndDate(schedules.get(schedules.size() - 1).getDueDate());
        }
        
        // Update pending installments count (subtract 1 since first is already paid)
        if (plan.getPendingInstallments() != null && plan.getPendingInstallments() > 0) {
            plan.setPendingInstallments(plan.getPendingInstallments() - 1);
        }
        
        paymentPlanRepository.save(plan);

        return scheduleRepository.saveAll(schedules);
    }

    /**
     * Overloaded method for backward compatibility
     * Also calculates agreement end date based on plan duration, not installment dates
     */
    @Transactional
    public List<PaymentRequestSchedule> generateSchedule(
            TenantPaymentPlan plan,
            RoomAgreementPlan planSnapshot
    ) {
        return generateSchedule(plan, planSnapshot, null);
    }

    /**
     * Returns the full payment schedule for a tenant (all installments).
     */
    public PaymentLedgerResponse getTenantLedger(UUID tenantId) {
        TenantPaymentPlan plan = paymentPlanRepository.findByTenant_UserIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new NotFoundException("No active payment plan found for tenant"));

        List<PaymentRequestSchedule> schedules =
                scheduleRepository.findByTenantPaymentPlan_PlanIdOrderByInstallmentNumber(plan.getPlanId());

        // Installments are already sorted by installment number from the database
        List<InstallmentResponse> installments = schedules.stream()
                .map(this::toInstallmentResponse)
                .collect(Collectors.toList());

        long totalPaid = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.COMPLETED)
                .mapToLong(PaymentRequestSchedule::getPaidAmount)
                .sum();

        long totalPending = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.SCHEDULED
                        || s.getPaymentStatus() == TransactionStatus.OVERDUE
                        || s.getPaymentStatus() == TransactionStatus.PARTIALLY_PAID)
                .mapToLong(s -> s.getAmount() - s.getPaidAmount() + s.getLateFeeApplied())
                .sum();

        long overdueCount = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.OVERDUE)
                .count();

        PaymentLedgerResponse response = new PaymentLedgerResponse();
        response.setPlanId(plan.getPlanId());
        response.setAgreementId(plan.getAgreementId());
        response.setInstallmentAmount(plan.getInstallmentAmount());
        response.setPaymentFrequency(plan.getPaymentFrequency().name());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setTotalPaid(totalPaid);
        response.setTotalPending(totalPending);
        response.setOverdueCount((int) overdueCount);
        response.setInstallments(installments);
        return response;
    }

    /**
     * Records a payment against a specific installment.
     * Supports full and partial payments with both CASH (OTP) and ONLINE (Razorpay) modes.
     */
    @Transactional
    public InstallmentResponse recordPayment(UUID scheduleId, RecordPaymentRequest request, User tenant, User owner) {
        System.out.println("=== PAYMENT SCHEDULE SERVICE START ===");
        System.out.println("Schedule ID: " + scheduleId);
        System.out.println("Request: " + request);
        System.out.println("Tenant: " + tenant.getUsername());
        System.out.println("Owner: " + owner.getUsername());
        
        PaymentRequestSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Installment not found"));

        System.out.println("Found schedule - Installment #" + schedule.getInstallmentNumber());
        System.out.println("Schedule status: " + schedule.getPaymentStatus());
        System.out.println("Schedule amount: " + schedule.getAmount());
        System.out.println("Already paid: " + schedule.getPaidAmount());
        System.out.println("Late fee: " + schedule.getLateFeeApplied());

        if (schedule.getPaymentStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("This installment is already fully paid");
        }

        long totalDue = schedule.getAmount() + schedule.getLateFeeApplied() - schedule.getPaidAmount();
        long paying = request.getAmount();
        
        System.out.println("Total due: " + totalDue);
        System.out.println("Paying amount: " + paying);

        if (paying <= 0 || paying > totalDue) {
            System.out.println("Invalid payment amount - must be between 1 and " + totalDue);
            throw new IllegalArgumentException("Payment amount must be between 1 and " + totalDue);
        }

        // Determine payment mode
        TransactionMode paymentMode = request.getPaymentMode() != null ? request.getPaymentMode() : TransactionMode.CASH;
        System.out.println("Payment mode: " + paymentMode);
        boolean otpVerified = false;
        
        // Handle payment verification based on mode
        if (paymentMode == TransactionMode.CASH) {
            System.out.println("Processing CASH payment...");
            // Special handling for owner collections
            if ("OWNER_COLLECTION".equals(request.getOtp())) {
                System.out.println("Owner collection - skipping OTP verification");
                otpVerified = true;
            } else {
                System.out.println("Regular tenant payment - verifying OTP");
                if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                    System.out.println("OTP is null or empty");
                    throw new IllegalStateException("OTP is required for cash payment");
                }
                System.out.println("Verifying OTP: " + request.getOtp() + " for schedule: " + scheduleId);
                otpVerified = cashPaymentOtpService.verifyInstallmentOtp(scheduleId, request.getOtp());
                System.out.println("OTP verification result: " + otpVerified);
                if (!otpVerified) {
                    throw new IllegalStateException("Invalid or expired OTP");
                }
            }
        } else if (paymentMode == TransactionMode.ONLINE) {
            System.out.println("Processing ONLINE payment...");
            // Verify online payment
            if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null) {
                throw new IllegalStateException("Payment details are required for online payment");
            }
            
            // Verify payment signature
            com.krunity.HostelManagment.dto.VerifyPaymentRequest verifyRequest = 
                new com.krunity.HostelManagment.dto.VerifyPaymentRequest();
            verifyRequest.setOrderId(request.getRazorpayOrderId());
            verifyRequest.setPaymentId(request.getRazorpayPaymentId());
            verifyRequest.setSignature(request.getRazorpaySignature());
            
            com.krunity.HostelManagment.dto.VerifyPaymentResponse verifyResponse = 
                paymentService.verifyInstallmentPayment(verifyRequest);
            
            if (!verifyResponse.isVerified()) {
                throw new IllegalStateException("Payment verification failed");
            }
            otpVerified = true; // Mark as verified for online payments
        }

        System.out.println("Payment verification completed. OTP verified: " + otpVerified);

        // Record transaction
        String transactionReason = "OWNER_COLLECTION".equals(request.getOtp()) 
            ? "Installment #" + schedule.getInstallmentNumber() + " payment (collected by owner)"
            : "Installment #" + schedule.getInstallmentNumber() + " payment";
            
        System.out.println("Creating transaction: " + transactionReason);
        Transaction transaction = Transaction.builder()
                .planId(schedule.getTenantPaymentPlan())
                .fromUser(tenant)
                .toUser(owner)
                .amount(paying)
                .mode(paymentMode)
                .status(TransactionStatus.COMPLETED)
                .reason(transactionReason)
                .otpVerified(otpVerified)
                .build();
        transaction = transactionRepository.save(transaction);
        System.out.println("Transaction saved with ID: " + transaction.getTransactionId());

        // Update schedule
        schedule.setPaidAmount(schedule.getPaidAmount() + paying);
        schedule.setTransactionId(transaction.getTransactionId());

        if (schedule.getPaidAmount() >= schedule.getAmount() + schedule.getLateFeeApplied()) {
            schedule.setPaymentStatus(TransactionStatus.COMPLETED);
            schedule.setPaidAt(LocalDateTime.now());
            System.out.println("Installment fully paid - marking as COMPLETED");
            // Decrement pending installments on the plan
            TenantPaymentPlan plan = schedule.getTenantPaymentPlan();
            if (plan.getPendingInstallments() != null && plan.getPendingInstallments() > 0) {
                plan.setPendingInstallments(plan.getPendingInstallments() - 1);
                paymentPlanRepository.save(plan);
                System.out.println("Decremented pending installments to: " + (plan.getPendingInstallments() - 1));
            }
        } else {
            schedule.setPaymentStatus(TransactionStatus.PARTIALLY_PAID);
            System.out.println("Partial payment - marking as PARTIALLY_PAID");
        }

        schedule = scheduleRepository.save(schedule);
        System.out.println("Schedule updated and saved");
        System.out.println("=== PAYMENT SCHEDULE SERVICE END ===");
        return toInstallmentResponse(schedule);
    }

    private InstallmentResponse toInstallmentResponse(PaymentRequestSchedule s) {
        InstallmentResponse r = new InstallmentResponse();
        r.setScheduleId(s.getScheduleId());
        r.setInstallmentNumber(s.getInstallmentNumber());
        r.setAmount(s.getAmount());
        r.setDueDate(s.getDueDate());
        r.setPaymentStatus(s.getPaymentStatus().name());
        r.setPaidAmount(s.getPaidAmount());
        r.setLateFeeApplied(s.getLateFeeApplied());
        r.setPaidAt(s.getPaidAt());
        r.setTransactionId(s.getTransactionId());
        // Populate payment mode from the linked transaction when available
        if (s.getTransactionId() != null) {
            transactionRepository.findById(s.getTransactionId())
                    .ifPresent(tx -> r.setPaymentMode(tx.getMode().name()));
        }
        return r;
    }
}
