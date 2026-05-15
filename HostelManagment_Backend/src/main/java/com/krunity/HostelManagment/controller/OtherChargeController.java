package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.Utils.ApplicationContext;
import com.krunity.HostelManagment.dto.OtherChargeRequest;
import com.krunity.HostelManagment.dto.OtherChargeResponse;
import com.krunity.HostelManagment.service.OtherChargeService;
import com.krunity.HostelManagment.service.UserService;
import com.krunity.HostelManagment.service.OtherChargePaymentService;
import com.krunity.HostelManagment.service.CashPaymentOtpService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/other-charges")
@RequiredArgsConstructor
public class OtherChargeController {

    private final OtherChargeService otherChargeService;
    private final UserService userService;

    /**
     * Create a new other charge (Owner only)
     */
    @PostMapping
    public ResponseEntity<OtherChargeResponse> createOtherCharge(
            @Valid @RequestBody OtherChargeRequest request,
            Authentication authentication) {

        UUID ownerId = ApplicationContext.getUser().getUserId();
        log.info("Creating other charge: {} by owner: {}", request.getChargeName(), ownerId);
        
        OtherChargeResponse response = otherChargeService.createOtherCharge(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all charges created by the owner
     */
    @GetMapping("/owner")
    public ResponseEntity<List<OtherChargeResponse>> getOwnerCharges(Authentication authentication) {
        UUID ownerId = userService.getCurrentUserId(authentication);
        log.info("Fetching charges for owner: {}", ownerId);
        
        List<OtherChargeResponse> charges = otherChargeService.getChargesByOwner(ownerId);
        return ResponseEntity.ok(charges);
    }

    /**
     * Get paginated charges created by the owner
     */
    @GetMapping("/owner/paginated")
    public ResponseEntity<Page<OtherChargeResponse>> getOwnerChargesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        UUID ownerId = userService.getCurrentUserId(authentication);
        log.info("Fetching paginated charges for owner: {} - page: {}, size: {}", ownerId, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OtherChargeResponse> charges = otherChargeService.getChargesByOwner(ownerId, pageable);
        
        return ResponseEntity.ok(charges);
    }

    /**
     * Get all charges for a tenant (both direct and room-based)
     */
    @GetMapping("/tenant")
    public ResponseEntity<List<OtherChargeResponse>> getTenantCharges(Authentication authentication) {
        UUID tenantId = userService.getCurrentUserId(authentication);
        log.info("Fetching charges for tenant: {}", tenantId);
        
        List<OtherChargeResponse> charges = otherChargeService.getChargesByTenant(tenantId);
        return ResponseEntity.ok(charges);
    }

    /**
     * Get specific charge by ID
     */
    @GetMapping("/{chargeId}")
    public ResponseEntity<OtherChargeResponse> getChargeById(
            @PathVariable UUID chargeId,
            Authentication authentication) {
        
        log.info("Fetching charge: {}", chargeId);
        
        OtherChargeResponse charge = otherChargeService.getChargeById(chargeId);
        
        // TODO: Add authorization check to ensure user can access this charge
        
        return ResponseEntity.ok(charge);
    }

    /**
     * Update an existing charge (Owner only)
     */
    @PutMapping("/{chargeId}")
    public ResponseEntity<OtherChargeResponse> updateCharge(
            @PathVariable UUID chargeId,
            @Valid @RequestBody OtherChargeRequest request,
            Authentication authentication) {
        
        UUID ownerId = userService.getCurrentUserId(authentication);
        log.info("Updating charge: {} by owner: {}", chargeId, ownerId);
        
        OtherChargeResponse response = otherChargeService.updateCharge(chargeId, request, ownerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a charge (Owner only)
     */
    @DeleteMapping("/{chargeId}")
    public ResponseEntity<Void> deleteCharge(
            @PathVariable UUID chargeId,
            Authentication authentication) {
        
        UUID ownerId = userService.getCurrentUserId(authentication);
        log.info("Deleting charge: {} by owner: {}", chargeId, ownerId);
        
        otherChargeService.deleteCharge(chargeId, ownerId);
        return ResponseEntity.noContent().build();
    }
}

/**
 * Tenant-specific endpoints
 */
@Slf4j
@RestController
@RequestMapping("/tenant/other-charges")
@RequiredArgsConstructor
class TenantOtherChargeController {

    private final OtherChargeService otherChargeService;
    private final UserService userService;
    private final OtherChargePaymentService otherChargePaymentService;
    private final CashPaymentOtpService cashPaymentOtpService;

    /**
     * Get all charges for the current tenant
     */
    @GetMapping
    public ResponseEntity<List<OtherChargeResponse>> getMyCharges(Authentication authentication) {
        UUID tenantId = ApplicationContext.getUser().getUserId();
        log.info("Tenant {} fetching their charges", tenantId);
        
        List<OtherChargeResponse> charges = otherChargeService.getChargesByTenant(tenantId);
        return ResponseEntity.ok(charges);
    }

    /**
     * Get specific charge details for tenant
     */
    @GetMapping("/{chargeId}")
    public ResponseEntity<OtherChargeResponse> getChargeDetails(
            @PathVariable UUID chargeId,
            Authentication authentication) {
        
        UUID tenantId = userService.getCurrentUserId(authentication);
        log.info("Tenant {} fetching charge details: {}", tenantId, chargeId);
        
        OtherChargeResponse charge = otherChargeService.getChargeById(chargeId);
        
        // TODO: Add authorization check to ensure tenant can access this charge
        
        return ResponseEntity.ok(charge);
    }

    /**
     * Pay other charge (supports both online and cash payment with OTP)
     */
    @PostMapping("/pay/{chargeId}")
    public ResponseEntity<?> payOtherCharge(
            @PathVariable UUID chargeId,
            @Valid @RequestBody OtherChargePaymentRequest request) {
        try {
            System.out.println("=== OTHER CHARGE PAYMENT REQUEST START ===");
            System.out.println("Charge ID: " + chargeId);
            System.out.println("Request Amount: " + request.getAmount());
            System.out.println("Payment Mode: " + request.getPaymentMode());
            System.out.println("OTP provided: " + (request.getOtp() != null ? "Yes (length: " + request.getOtp().length() + ")" : "No"));
            
            UUID tenantId = ApplicationContext.getUser().getUserId();
            System.out.println("Tenant: " + tenantId);

            // Get the charge to find the owner
            OtherChargeResponse charge = otherChargeService.getChargeById(chargeId);
            UUID ownerId = charge.getOwnerId(); // Assuming this field exists in the response

            // Validate OTP for cash payments
            if ("CASH".equals(request.getPaymentMode())) {
                if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("OTP is required for cash payments");
                }
                
                boolean otpValid = cashPaymentOtpService.verifyOtherChargeOtp(chargeId.toString(), request.getOtp());
                if (!otpValid) {
                    return ResponseEntity.badRequest().body("Invalid or expired OTP");
                }
            }

            // Process the payment
            if ("CASH".equals(request.getPaymentMode())) {
                // For cash payments, record directly
                otherChargePaymentService.recordOtherChargeCashPayment(
                    chargeId, 
                    tenantId, 
                    request.getAmount(), 
                    ownerId, // Use the actual owner ID from the charge
                    "Cash payment with OTP verification"
                );
            } else {
                // For online payments, this would go through Razorpay flow
                // Implementation would be similar to installment payments
                return ResponseEntity.badRequest().body("Online payments not yet implemented for other charges");
            }

            System.out.println("Payment processed successfully!");
            System.out.println("=== OTHER CHARGE PAYMENT REQUEST END ===");
            return ResponseEntity.ok().body("Payment processed successfully");
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("=== PAYMENT VALIDATION ERROR ===");
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("=== PAYMENT SYSTEM ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Pay other charge installment (supports both online and cash payment with OTP)
     */
    @PostMapping("/pay-installment/{installmentId}")
    public ResponseEntity<?> payOtherChargeInstallment(
            @PathVariable UUID installmentId,
            @Valid @RequestBody OtherChargePaymentRequest request) {
        try {
            System.out.println("=== OTHER CHARGE INSTALLMENT PAYMENT REQUEST START ===");
            System.out.println("Installment ID: " + installmentId);
            System.out.println("Request Amount: " + request.getAmount());
            System.out.println("Payment Mode: " + request.getPaymentMode());
            System.out.println("OTP provided: " + (request.getOtp() != null ? "Yes (length: " + request.getOtp().length() + ")" : "No"));
            
            UUID tenantId = ApplicationContext.getUser().getUserId();
            System.out.println("Tenant: " + tenantId);

            // Validate OTP for cash payments
            if ("CASH".equals(request.getPaymentMode())) {
                if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("OTP is required for cash payments");
                }
                
                // For installments, we verify OTP against the parent charge ID
                // We need to get the charge ID from the installment
                boolean otpValid = cashPaymentOtpService.verifyOtherChargeOtp(installmentId.toString(), request.getOtp());
                if (!otpValid) {
                    return ResponseEntity.badRequest().body("Invalid or expired OTP");
                }
            }

            // Process the installment payment
            if ("CASH".equals(request.getPaymentMode())) {
                // For cash payments, record directly
                // The service method will get the owner ID from the installment's parent charge
                otherChargePaymentService.recordOtherChargeInstallmentCashPayment(
                    installmentId, 
                    request.getAmount(), 
                    tenantId, // Pass tenant ID, service will get owner ID from installment
                    "Cash payment with OTP verification"
                );
            } else {
                // For online payments, this would go through Razorpay flow
                return ResponseEntity.badRequest().body("Online payments not yet implemented for other charge installments");
            }

            System.out.println("Installment payment processed successfully!");
            System.out.println("=== OTHER CHARGE INSTALLMENT PAYMENT REQUEST END ===");
            return ResponseEntity.ok().body("Installment payment processed successfully");
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("=== INSTALLMENT PAYMENT VALIDATION ERROR ===");
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("=== INSTALLMENT PAYMENT SYSTEM ERROR ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * DTO for other charge payment requests
     */
    @Data
    public static class OtherChargePaymentRequest {
        private BigDecimal amount;
        private String paymentMode; // "ONLINE" or "CASH"
        private String otp; // Required for cash payments
        private String razorpayOrderId; // For online payments
        private String razorpayPaymentId; // For online payments
        private String razorpaySignature; // For online payments
    }
}

/**
 * Owner-specific endpoints
 */
@Slf4j
@RestController
@RequestMapping("/owner/other-charges")
@RequiredArgsConstructor
class OwnerOtherChargeController {

    private final OtherChargeService otherChargeService;
    private final UserService userService;

    /**
     * Get all charges created by the owner
     */
    @GetMapping
    public ResponseEntity<List<OtherChargeResponse>> getMyCharges(Authentication authentication) {
        UUID ownerId = ApplicationContext.getUser().getUserId();
        log.info("Owner {} fetching their charges", ownerId);
        
        List<OtherChargeResponse> charges = otherChargeService.getChargesByOwner(ownerId);
        return ResponseEntity.ok(charges);
    }

    /**
     * Create a new charge
     */
    @PostMapping
    public ResponseEntity<OtherChargeResponse> createCharge(
            @Valid @RequestBody OtherChargeRequest request,
            Authentication authentication) {
        
        UUID ownerId = userService.getCurrentUserId(authentication);
        log.info("Owner {} creating new charge: {}", ownerId, request.getChargeName());
        
        OtherChargeResponse response = otherChargeService.createOtherCharge(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}